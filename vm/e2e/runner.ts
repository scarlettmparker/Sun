import { spawn, spawnSync, type ChildProcess } from "node:child_process";
import { randomUUID } from "node:crypto";
import {
  cpSync,
  existsSync,
  mkdirSync,
  openSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { homedir } from "node:os";
import { join, resolve } from "node:path";

type RepoKey = "Guided-Reader" | "Checklist";
type RunStatus = "queued" | "running" | "passed" | "failed";

type RepoConfig = {
  /**
   * Clone URL.
   */
  url: string;
  /**
   * Port search start.
   */
  port: number;
};

type Run = {
  /**
   * Run identifier.
   */
  id: string;
  /**
   * Repository name.
   */
  repo: RepoKey;
  /**
   * Commit under test.
   */
  sha: string;
  /**
   * Current run state.
   */
  status: RunStatus;
  /**
   * Queue time.
   */
  createdAt: string;
  /**
   * Start time.
   */
  startedAt?: string;
  /**
   * Finish time.
   */
  finishedAt?: string;
  /**
   * Test process exit code.
   */
  exitCode?: number;
  /**
   * Failure summary.
   */
  error?: string;
  /**
   * Report directory.
   */
  reportPath?: string;
};

const SUN_ROOT = resolve(import.meta.dir, "../..");
const ROOT = process.env.E2E_RUNNER_ROOT ?? join(homedir(), ".local/share/sun-e2e");
const PORT = Number(process.env.E2E_RUNNER_PORT ?? "8090");
const TOKEN = process.env.E2E_RUNNER_TOKEN ?? "";
const JWT_SECRET =
  process.env.E2E_JWT_SECRET ??
  "test-secret-key-for-functional-tests-only-very-long-secure-key-1234567890ABCDEF";
const DATABASE_URL = "postgres://sun:sun@127.0.0.1:5436/sun_e2e";
const BACKEND_URL = "http://127.0.0.1:8085";
const BUN = process.env.BUN ?? join(homedir(), ".bun/bin/bun");
const COMPOSE = join(SUN_ROOT, "vm/e2e/compose.e2e.yml");

const REPOS: Record<RepoKey, RepoConfig> = {
  "Guided-Reader": {
    url: "https://github.com/scarlettmparker/Guided-Reader.git",
    port: 5200,
  },
  Checklist: {
    url: "https://github.com/scarlettmparker/Checklist.git",
    port: 5210,
  },
};

const runs = new Map<string, Run>();
const queue: Run[] = [];
let activeRun: Run | null = null;

/**
 * Writes a line to the runner log.
 *
 * @param message the message to print
 */
function log(message: string): void {
  console.log(`[sun-e2e] ${message}`);
}

/**
 * Waits for a promise-returning predicate to succeed.
 *
 * @param predicate the check to poll
 * @param timeoutMs maximum wait
 * @param intervalMs poll interval
 */
async function waitFor(
  predicate: () => Promise<boolean>,
  timeoutMs: number,
  intervalMs = 500,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await predicate()) return;
    await Bun.sleep(intervalMs);
  }
  throw new Error("Timed out waiting for the E2E stack");
}

/**
 * Checks whether a local port is open.
 *
 * @param port the port to probe
 */
async function isPortOpen(port: number): Promise<boolean> {
  try {
    const socket = await Bun.connect({
      hostname: "127.0.0.1",
      port,
      socket: { data() {}, open() {}, close() {}, error() {} },
    });
    socket.end();
    return true;
  } catch {
    return false;
  }
}

/**
 * Picks a free app port.
 *
 * @param start the first port to try
 */
async function findPort(start: number): Promise<number> {
  for (let port = start; port < start + 100; port += 1) {
    if (!(await isPortOpen(port))) return port;
  }
  throw new Error("No free app port");
}

/**
 * Runs a child process and writes its output to a log file.
 *
 * @param command the executable
 * @param args the arguments
 * @param cwd the working directory
 * @param logPath the log file
 * @param env extra environment
 * @returns the exit code
 */
async function runCommand(
  command: string,
  args: string[],
  cwd: string,
  logPath: string,
  env: Record<string, string> = {},
): Promise<number> {
  mkdirSync(resolve(logPath, ".."), { recursive: true });
  const fd = openSync(logPath, "a");
  return await new Promise((resolveExit) => {
    const child = spawn(command, args, {
      cwd,
      env: { ...process.env, ...env },
      stdio: ["ignore", fd, fd],
    });
    child.on("exit", (code) => {
      resolveExit(code ?? 1);
    });
  });
}

/**
 * Runs a command synchronously.
 *
 * @param command the executable
 * @param args the arguments
 * @param cwd the working directory
 */
function runSync(command: string, args: string[], cwd: string): void {
  const result = spawnSync(command, args, { cwd, encoding: "utf8" });
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(" ")} failed: ${result.stderr}`);
  }
}

/**
 * Starts the disposable database and waits for it.
 */
function startDatabase(): void {
  runSync("docker", ["compose", "-f", COMPOSE, "up", "-d", "db"], SUN_ROOT);
  for (let attempt = 0; attempt < 30; attempt += 1) {
    const result = spawnSync(
      "docker",
      ["compose", "-f", COMPOSE, "exec", "-T", "db", "pg_isready", "-U", "sun", "-d", "sun_e2e"],
      { encoding: "utf8" },
    );
    if (result.status === 0) return;
    spawnSync("sleep", ["1"]);
  }
  throw new Error("E2E database did not become ready");
}

/**
 * Drops the E2E schema.
 */
function resetDatabase(): void {
  runSync(join(SUN_ROOT, "vm/e2e/reset-db.sh"), [], SUN_ROOT);
}

/**
 * Starts the GraphQL backend and waits for health.
 *
 * @param logPath backend log file
 * @returns the backend process
 */
async function startBackend(logPath: string): Promise<ChildProcess> {
  mkdirSync(resolve(logPath, ".."), { recursive: true });
  const fd = openSync(logPath, "a");
  const child = spawn(join(SUN_ROOT, "vm/e2e/start-backend.sh"), [], {
    cwd: SUN_ROOT,
    env: { ...process.env, E2E_JWT_SECRET: JWT_SECRET },
    stdio: ["ignore", fd, fd],
  });

  await waitFor(async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/graphql`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ query: "{__typename}" }),
      });
      return response.ok;
    } catch {
      return false;
    }
  }, 90_000);

  return child;
}

/**
 * Stops a child process.
 *
 * @param child the process to stop
 */
async function stopProcess(child: ChildProcess | null): Promise<void> {
  if (!child || child.exitCode !== null) return;
  child.kill("SIGTERM");
  await Bun.sleep(1000);
  if (child.exitCode === null) child.kill("SIGKILL");
}

/**
 * Clones or updates the bare cache for a repository.
 *
 * @param repo the repository key
 */
function syncRepository(repo: RepoKey): string {
  const dir = join(ROOT, "repos", repo);
  if (!existsSync(join(dir, ".git"))) {
    mkdirSync(join(ROOT, "repos"), { recursive: true });
    runSync("git", ["clone", "--quiet", REPOS[repo].url, dir], ROOT);
  } else {
    runSync("git", ["fetch", "--quiet", "--tags", "origin"], dir);
  }
  return dir;
}

/**
 * Creates a detached worktree for a commit.
 *
 * @param repoDir the repository cache
 * @param repo the repository key
 * @param sha the commit
 * @returns the worktree path
 */
function createWorktree(repoDir: string, repo: RepoKey, sha: string): string {
  const worktree = join(ROOT, "worktrees", repo, sha);
  if (existsSync(worktree)) {
    rmSync(worktree, { recursive: true, force: true });
  }
  runSync("git", ["worktree", "prune"], repoDir);
  runSync("git", ["worktree", "add", "--detach", worktree, sha], repoDir);
  return worktree;
}

/**
 * Starts an app against the E2E backend.
 *
 * @param appDir the app checkout
 * @param port the app port
 * @param logPath the app log file
 * @returns the app process
 */
function startApp(appDir: string, port: number, logPath: string): ChildProcess {
  mkdirSync(resolve(logPath, ".."), { recursive: true });
  const fd = openSync(logPath, "a");
  const child = spawn(BUN, ["--preload", "./css-plugin.ts", "server.js"], {
    cwd: appDir,
    env: {
      ...process.env,
      PATH: `${join(homedir(), ".bun/bin")}:/usr/local/bin:/usr/bin:/bin`,
      NODE_ENV: "production",
      SERVER_HOST: "127.0.0.1",
      SERVER_PORT: String(port),
      GRAPHQL_ENDPOINT: `${BACKEND_URL}/graphql`,
      VITE_SERVER_BASE: `http://127.0.0.1:${port}`,
      ALLOWED_HOSTS: "localhost,127.0.0.1",
      ALLOWED_ORIGINS: "localhost,127.0.0.1",
      JWT_SECRET,
    },
    stdio: ["ignore", fd, fd],
  });
  return child;
}

/**
 * Copies Playwright output into the report directory.
 *
 * @param worktree the app checkout
 * @param reportPath the export directory
 */
function copyReports(worktree: string, reportPath: string): void {
  for (const name of ["playwright-report", "test-results"]) {
    const source = join(worktree, "e2e", name);
    if (existsSync(source)) {
      cpSync(source, join(reportPath, name), { recursive: true });
    }
  }
}

/**
 * Executes one run.
 *
 * @param run the queued run
 */
async function executeRun(run: Run): Promise<void> {
  run.status = "running";
  run.startedAt = new Date().toISOString();

  const reportPath = join(ROOT, "reports", run.id);
  mkdirSync(reportPath, { recursive: true });
  run.reportPath = reportPath;

  let backend: ChildProcess | null = null;
  let app: ChildProcess | null = null;
  let repoDir = "";
  let worktree = "";

  try {
    log(`start ${run.repo} ${run.sha} (${run.id})`);
    startDatabase();
    resetDatabase();
    backend = await startBackend(join(reportPath, "backend.log"));

    repoDir = syncRepository(run.repo);
    worktree = createWorktree(repoDir, run.repo, run.sha);

    const buildCode = await runCommand(
      BUN,
      ["install", "--frozen-lockfile", "--no-progress"],
      worktree,
      join(reportPath, "build.log"),
    );
    if (buildCode !== 0) throw new Error("Dependency install failed");

    const compileCode = await runCommand(
      BUN,
      ["run", "build"],
      worktree,
      join(reportPath, "build.log"),
    );
    if (compileCode !== 0) throw new Error("App build failed");

    const port = await findPort(REPOS[run.repo].port);
    app = startApp(worktree, port, join(reportPath, "app.log"));
    await waitFor(async () => {
      try {
        const response = await fetch(`http://127.0.0.1:${port}/login`);
        return response.status < 500;
      } catch {
        return false;
      }
    }, 60_000);

    const testCode = await runCommand(
      "./e2e/run.sh",
      [],
      worktree,
      join(reportPath, "test.log"),
      {
        APP_URL: `http://127.0.0.1:${port}`,
        BACKEND_URL,
        DATABASE_URL,
        E2E_ENV_FILE: "/dev/null",
      },
    );
    run.exitCode = testCode;
    run.status = testCode === 0 ? "passed" : "failed";
    if (testCode !== 0) run.error = "Playwright tests failed";
  } catch (error) {
    run.status = "failed";
    run.error = error instanceof Error ? error.message : String(error);
    log(`failed ${run.id}: ${run.error}`);
  } finally {
    run.finishedAt = new Date().toISOString();
    await stopProcess(app);
    await stopProcess(backend);

    copyReports(worktree, reportPath);

    if (repoDir && worktree) {
      try {
        runSync("git", ["worktree", "remove", "--force", worktree], repoDir);
      } catch {
        rmSync(worktree, { recursive: true, force: true });
      }
    }

    writeFileSync(join(reportPath, "run.json"), JSON.stringify(run, null, 2));
    log(`done ${run.id}: ${run.status}`);
  }
}

/**
 * Starts the next queued run.
 */
async function drain(): Promise<void> {
  if (activeRun) return;
  activeRun = queue.shift() ?? null;
  while (activeRun) {
    await executeRun(activeRun);
    activeRun = null;
    activeRun = queue.shift() ?? null;
  }
}

/**
 * Reads the bearer token from a request.
 *
 * @param request the incoming request
 */
function authorized(request: Request): boolean {
  if (!TOKEN) return false;
  return request.headers.get("authorization") === `Bearer ${TOKEN}`;
}

/**
 * JSON error response.
 *
 * @param message the error
 * @param status the HTTP status
 */
function errorResponse(message: string, status: number): Response {
  return Response.json({ error: message }, { status });
}

const server = Bun.serve({
  hostname: "127.0.0.1",
  port: PORT,
  async fetch(request) {
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return Response.json({ ok: true });
    }
    if (!authorized(request)) {
      return errorResponse("Unauthorized", 401);
    }

    if (request.method === "POST" && url.pathname === "/runs") {
      let body: { repo?: string; sha?: string };
      try {
        body = (await request.json()) as { repo?: string; sha?: string };
      } catch {
        return errorResponse("Invalid JSON", 400);
      }

      const repo = body.repo as RepoKey | undefined;
      const sha = body.sha ?? "";
      if (!repo || !(repo in REPOS)) {
        return errorResponse("Unknown repo", 400);
      }
      if (!/^[0-9a-f]{7,40}$/i.test(sha)) {
        return errorResponse("Invalid sha", 400);
      }

      const existing = [...runs.values()].find(
        (candidate) =>
          candidate.repo === repo && candidate.sha === sha && candidate.status !== "failed",
      );
      if (existing) {
        return Response.json(existing, { status: 200 });
      }

      const run: Run = {
        id: randomUUID(),
        repo,
        sha,
        status: "queued",
        createdAt: new Date().toISOString(),
      };
      runs.set(run.id, run);
      queue.push(run);
      void drain();
      return Response.json(run, { status: 202 });
    }

    if (request.method === "GET" && url.pathname.startsWith("/runs/")) {
      const id = url.pathname.slice("/runs/".length);
      const run = runs.get(id);
      return run ? Response.json(run) : errorResponse("Run not found", 404);
    }

    if (request.method === "GET" && url.pathname === "/runs") {
      return Response.json([...runs.values()].slice(-50).reverse());
    }

    return errorResponse("Not found", 404);
  },
});

log(`listening on http://127.0.0.1:${server.port}`);
