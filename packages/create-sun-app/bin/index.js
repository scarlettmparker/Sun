#!/usr/bin/env node
import { program } from "commander";
import fs from "fs-extra";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

program
  .argument("[project-directory]", "Directory to create the app in")
  .action(async (projectDir = ".") => {
    const targetDir = path.resolve(process.cwd(), projectDir);
    await fs.ensureDir(targetDir);

    const templateDir = path.join(__dirname, "..", "templates");

    console.log(`Scaffolding Vite SSR app with registries in ${targetDir}...`);

    // Copy entire template structure (each file read from templates/)
    await fs.copy(templateDir, targetDir, { overwrite: true });

    console.log("Done! cd into directory, npm install, npm run dev.");
  });

program.parse();
