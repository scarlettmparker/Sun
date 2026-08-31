/**
 * Markdown helpers for viewer and editor.
 */

/**
 * Renders markdown directly to viewer HTML.
 *
 * @param text - raw markdown
 * @returns viewer HTML
 */
export const renderMarkdown = (text: string): string => {
  const lines = text.split("\n");
  const blocks: string[] = [];

  for (let i = 0; i < lines.length; ) {
    if (
      isTableRow(lines[i]) &&
      i + 1 < lines.length &&
      isDelimiterRow(lines[i + 1])
    ) {
      const { html, nextIndex } = parseTableBlockViewer(lines, i);
      blocks.push(html);
      i = nextIndex;
    } else if (isHrRow(lines[i])) {
      blocks.push("<hr>");
      i += 1;
    } else {
      blocks.push(renderLine(lines[i]));
      i += 1;
    }
  }

  return blocks.join("\n").replace(/\n/g, "<br>\n");
};

/**
 * Renders a single line.
 *
 * @param line - raw line
 * @returns HTML for the line
 */
function renderLine(line: string): string {
  if (line.startsWith("# ")) return `<span class="md-h1">${renderInline(line.slice(2))}</span>`;
  if (line.startsWith("## ")) return `<span class="md-h2">${renderInline(line.slice(3))}</span>`;
  if (line.startsWith("### ")) return `<span class="md-h3">${renderInline(line.slice(4))}</span>`;
  if (line.startsWith("#### ")) return `<span class="md-h4">${renderInline(line.slice(5))}</span>`;
  if (line.startsWith("##### ")) return `<span class="md-h5">${renderInline(line.slice(6))}</span>`;
  if (line.startsWith("###### ")) return `<span class="md-h6">${renderInline(line.slice(7))}</span>`;
  if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")) {
    const rest = line.slice(2);
    return `<span class="md-list">•</span> ${renderInline(rest)}`;
  }
  if (line.startsWith("> ")) {
    return `<span class="md-quote">${renderInline(line.slice(2))}</span>`;
  }
  return renderInline(line);
}

/**
 * Parses a table block for the viewer.
 *
 * @param lines - all lines
 * @param start - start index
 * @returns HTML and next index
 */
function parseTableBlockViewer(
  lines: string[],
  start: number,
): { html: string; nextIndex: number } {
  const headerLine = lines[start];
  const delimiterLine = lines[start + 1];
  const alignments = parseAlignments(delimiterLine);
  const headerCells = splitRow(headerLine);
  const colCount = headerCells.length;
  const headHtml = `<thead><tr>${headerCells
    .map(
      (cell, idx) =>
        `<th${alignAttr(alignments[idx])}>${renderInline(cell.trim())}</th>`,
    )
    .join("")}</tr></thead>`;
  const bodyRows: string[] = [];
  let idx = start + 2;
  while (
    idx < lines.length &&
    isTableRow(lines[idx]) &&
    !isDelimiterRow(lines[idx])
  ) {
    const cells = splitRow(lines[idx]);
    while (cells.length < colCount) cells.push("");
    if (cells.length > colCount) cells.length = colCount;
    const rowHtml = `<tr>${cells
      .map(
        (cell, cIdx) =>
          `<td${alignAttr(alignments[cIdx])}>${renderInline(cell.trim())}</td>`,
      )
      .join("")}</tr>`;
    bodyRows.push(rowHtml);
    idx += 1;
  }
  const bodyHtml = bodyRows.length ? `<tbody>${bodyRows.join("")}</tbody>` : "";
  return { html: `<table class="md-table">${headHtml}${bodyHtml}</table>`, nextIndex: idx };
}

/**
 * Renders inline markup.
 *
 * @param text - inline text
 * @returns HTML
 */
function renderInline(text: string): string {
  let out = "";
  let i = 0;
  while (i < text.length) {
    if (text[i] === "`") {
      const end = text.indexOf("`", i + 1);
      if (end !== -1) {
        const inner = text.slice(i + 1, end);
        out += `<span class="md-code">${escapeHtml(inner)}</span>`;
        i = end + 1;
        continue;
      }
    }
    if (text.startsWith("***", i)) {
      const end = text.indexOf("***", i + 3);
      if (end !== -1) {
        const inner = text.slice(i + 3, end);
        out += `<span class="md-bold-italic">${renderInline(inner)}</span>`;
        i = end + 3;
        continue;
      }
    }
    if (text.startsWith("**", i)) {
      const end = text.indexOf("**", i + 2);
      if (end !== -1) {
        const inner = text.slice(i + 2, end);
        out += `<span class="md-bold">${renderInline(inner)}</span>`;
        i = end + 2;
        continue;
      }
    }
    if (text.startsWith("__", i)) {
      const end = text.indexOf("__", i + 2);
      if (end !== -1) {
        const inner = text.slice(i + 2, end);
        out += `<span class="md-underline">${renderInline(inner)}</span>`;
        i = end + 2;
        continue;
      }
    }
    if (text.startsWith("~~", i)) {
      const end = text.indexOf("~~", i + 2);
      if (end !== -1) {
        const inner = text.slice(i + 2, end);
        out += `<span class="md-strike">${renderInline(inner)}</span>`;
        i = end + 2;
        continue;
      }
    }
    if (text[i] === "[") {
      const link = tryParseLink(text, i);
      if (link) {
        out += `<a href="${escapeAttr(link.url)}" target="_blank" class="md-link">${escapeHtml(link.title)}</a>`;
        i = link.end;
        continue;
      }
    }
    if (text[i] === "*") {
      if (text.startsWith("**", i) || text.startsWith("***", i)) {
        out += escapeHtml(text[i]);
        i++;
        continue;
      }
      const end = text.indexOf("*", i + 1);
      if (end !== -1) {
        const inner = text.slice(i + 1, end);
        if (inner.length > 0) {
          out += `<span class="md-italic">${renderInline(inner)}</span>`;
          i = end + 1;
          continue;
        }
      }
    }
    if (text[i] === "_") {
      if (text.startsWith("__", i)) {
        out += escapeHtml(text[i]);
        i++;
        continue;
      }
      const end = text.indexOf("_", i + 1);
      if (end !== -1) {
        const inner = text.slice(i + 1, end);
        if (inner.length > 0) {
          out += `<span class="md-italic">${renderInline(inner)}</span>`;
          i = end + 1;
          continue;
        }
      }
    }
    const ch = text[i];
    if (ch === "&") out += "&amp;";
    else if (ch === "<") out += "&lt;";
    else if (ch === ">") out += "&gt;";
    else out += ch;
    i++;
  }
  return out;
}

/**
 * Parses a balanced link at a position.
 *
 * @param text - full text
 * @param pos - index of [
 * @returns link or null
 */
function tryParseLink(
  text: string,
  pos: number,
): { title: string; url: string; end: number } | null {
  const close = text.indexOf("]", pos + 1);
  if (close === -1 || text[close + 1] !== "(") return null;
  const title = text.slice(pos + 1, close);
  if (title.includes("\n")) return null;
  let depth = 1;
  let j = close + 2;
  while (j < text.length && depth > 0) {
    const ch = text[j];
    if (ch === "(") depth++;
    else if (ch === ")") depth--;
    j++;
  }
  if (depth !== 0) return null;
  const url = text.slice(close + 2, j - 1);
  if (!url) return null;
  return { title, url, end: j };
}

/**
 * Escapes HTML entities.
 *
 * @param s - raw text
 * @returns escaped text
 */
function escapeHtml(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/**
 * Escapes an attribute value.
 *
 * @param s - raw text
 * @returns escaped text
 */
function escapeAttr(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

/**
 * Converts markdown text to HTML with styled spans for syntax highlighting.
 *
 * @param text - Markdown text to highlight.
 * @returns HTML string with spans for styling markdown elements.
 */
export const highlightMarkdown = (text: string): string => {
  const escaped = text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

  const lines = escaped.split("\n");
  const out: string[] = [];

  for (let i = 0; i < lines.length;) {
    if (
      isTableRow(lines[i]) &&
      i + 1 < lines.length &&
      isDelimiterRow(lines[i + 1])
    ) {
      const { html, nextIndex } = parseTableBlock(lines, i);
      out.push(html);
      i = nextIndex;
    } else if (isHrRow(lines[i])) {
      out.push("<hr>");
      i += 1;
    } else {
      out.push(processLine(lines[i]));
      i += 1;
    }
  }

  return out
    .map((line) => (isTableHtml(line) ? line : processInline(line)))
    .join("\n");
};

/**
 * Checks whether a line looks like a table row.
 *
 * @param line - Line to check.
 * @returns True if it contains at least one pipe and a cell.
 */
function isTableRow(line: string): boolean {
  const trimmed = line.trim();
  if (!trimmed.includes("|")) return false;
  // Must have at least one pipe and not be just delimiter
  return /^\s*\|?.*\|.*\|?\s*$/.test(trimmed);
}

/**
 * Checks whether a line is a GFM delimiter row.
 *
 * @param line - Line to check.
 * @returns True if it is a separator like |---|---| or |:---|:---:|
 */
function isDelimiterRow(line: string): boolean {
  const trimmed = line.trim();
  // Remove leading/trailing pipe for parsing
  const inner = trimmed.replace(/^\|/, "").replace(/\|$/, "").trim();
  if (!inner) return false;
  const cells = inner.split("|");
  if (cells.length === 0) return false;
  return cells.every((cell) => /^\s*:?-+:?\s*$/.test(cell));
}

/**
 * Checks whether a line is a horizontal rule.
 *
 * @param line - Line to check.
 * @returns True if it is ---, *** or ___.
 */
function isHrRow(line: string): boolean {
  const trimmed = line.trim();
  return (
    /^(-\s*){3,}$/.test(trimmed) ||
    /^(\*\s*){3,}$/.test(trimmed) ||
    /^(_\s*){3,}$/.test(trimmed)
  );
}

/**
 * Checks whether a string is already table HTML or hr.
 *
 * @param html - String to check.
 * @returns True if it is block HTML.
 */
function isTableHtml(html: string): boolean {
  const t = html.trim();
  return t.startsWith('<table class="md-table"') || t === "<hr>";
}

/**
 * Parses a contiguous table block starting at index.
 *
 * @param lines - All lines.
 * @param start - Start index of header row.
 * @returns HTML and next index after block.
 */
function parseTableBlock(
  lines: string[],
  start: number,
): { html: string; nextIndex: number } {
  const headerLine = lines[start];
  const delimiterLine = lines[start + 1];
  const alignments = parseAlignments(delimiterLine);

  const headerCells = splitRow(headerLine);
  const colCount = headerCells.length;

  const headHtml = `<thead><tr>${headerCells
    .map(
      (cell, idx) =>
        `<th${alignAttr(alignments[idx])}>${processInline(cell.trim())}</th>`,
    )
    .join("")}</tr></thead>`;

  const bodyRows: string[] = [];
  let idx = start + 2;
  while (
    idx < lines.length &&
    isTableRow(lines[idx]) &&
    !isDelimiterRow(lines[idx])
  ) {
    const cells = splitRow(lines[idx]);
    // Pad/truncate to colCount
    while (cells.length < colCount) cells.push("");
    if (cells.length > colCount) cells.length = colCount;

    const rowHtml = `<tr>${cells
      .map(
        (cell, cIdx) =>
          `<td${alignAttr(alignments[cIdx])}>${processInline(cell.trim())}</td>`,
      )
      .join("")}</tr>`;
    bodyRows.push(rowHtml);
    idx += 1;
  }

  const bodyHtml = bodyRows.length ? `<tbody>${bodyRows.join("")}</tbody>` : "";
  const html = `<table class="md-table">${headHtml}${bodyHtml}</table>`;
  return { html, nextIndex: idx };
}

/**
 * Splits a table row into cells, respecting escaped pipes.
 *
 * @param row - Raw row line.
 * @returns Cell strings.
 */
function splitRow(row: string): string[] {
  const trimmed = row.trim();
  // Remove leading/trailing pipe
  const inner = trimmed.replace(/^\|/, "").replace(/\|$/, "");
  // Protect escaped pipes
  const placeholder = "§§PIPE§§";
  const protectedRow = inner.replace(/\\\|/g, placeholder);
  const rawCells = protectedRow.split("|");
  return rawCells.map((c) =>
    c.replace(new RegExp(placeholder, "g"), "|").trim(),
  );
}

/**
 * Parses delimiter row into alignment per column.
 *
 * @param delimiter - Delimiter line.
 * @returns Alignments.
 */
function parseAlignments(
  delimiter: string,
): Array<"left" | "center" | "right" | null> {
  const cells = splitRow(delimiter);
  return cells.map((cell) => {
    const t = cell.trim();
    const left = t.startsWith(":");
    const right = t.endsWith(":");
    if (left && right) return "center";
    if (right) return "right";
    if (left) return "left";
    return null;
  });
}

/**
 * Returns align attribute string.
 *
 * @param align - Alignment or null.
 * @returns Attribute string.
 */
function alignAttr(align: "left" | "center" | "right" | null): string {
  return align ? ` align="${align}"` : "";
}

/**
 * Strips markdown syntax from the highlighted HTML, leaving only styled spans.
 */
export const stripMarkdown = (html: string): string => {
  let result = html;

  result = result.replace(
    /<span class="md-h1">#\s+(.*?)<\/span>/g,
    '<span class="md-h1">$1</span>',
  );
  result = result.replace(
    /<span class="md-h2">##\s+(.*?)<\/span>/g,
    '<span class="md-h2">$1</span>',
  );
  result = result.replace(
    /<span class="md-h3">###\s+(.*?)<\/span>/g,
    '<span class="md-h3">$1</span>',
  );
  result = result.replace(
    /<span class="md-h4">####\s+(.*?)<\/span>/g,
    '<span class="md-h4">$1</span>',
  );
  result = result.replace(
    /<span class="md-h5">#####\s+(.*?)<\/span>/g,
    '<span class="md-h5">$1</span>',
  );
  result = result.replace(
    /<span class="md-h6">######\s+(.*?)<\/span>/g,
    '<span class="md-h6">$1</span>',
  );

  result = result.replace(
    /<span class="md-list">([-*+])\s*<\/span>\s*/g,
    '<span class="md-list">•</span> ',
  );

  result = result.replace(
    /<span class="md-quote">&gt;\s+(.*?)<\/span>/g,
    '<span class="md-quote">$1</span>',
  );

  result = result.replace(
    /<span class="md-code">`([^`]+)`<\/span>/g,
    '<span class="md-code">$1</span>',
  );
  result = result.replace(
    /<span class="md-bold-italic">\*\*\*([^*]+)\*\*\*<\/span>/g,
    '<span class="md-bold-italic">$1</span>',
  );
  result = result.replace(
    /<span class="md-bold">\*\*([^*]+)\*\*<\/span>/g,
    '<span class="md-bold">$1</span>',
  );
  result = result.replace(
    /<span class="md-underline">__([^_]+)__<\/span>/g,
    '<span class="md-underline">$1</span>',
  );
  result = result.replace(
    /<span class="md-strike">~~([^~]+)~~<\/span>/g,
    '<span class="md-strike">$1</span>',
  );
  result = result.replace(
    /<span class="md-italic">\*([^*]+)\*<\/span>/g,
    '<span class="md-italic">$1</span>',
  );
  result = result.replace(
    /<span class="md-italic">_([^_]+)_<\/span>/g,
    '<span class="md-italic">$1</span>',
  );
  result = replaceSpanLinks(result);

  // Protect block elements from <br> insertion, then replace remaining newlines
  const tableTokens: string[] = [];
  result = result.replace(/<table class="md-table">[\s\S]*?<\/table>/g, (m) => {
    const token = `§§TABLE${tableTokens.length}§§`;
    tableTokens.push(m);
    return token;
  });

  const hrTokens: string[] = [];
  result = result.replace(/<hr>/g, (m) => {
    const token = `§§HR${hrTokens.length}§§`;
    hrTokens.push(m);
    return token;
  });

  result = result.replace(/\n/g, "<br>\n");

  for (let i = 0; i < hrTokens.length; i++) {
    result = result.replace(`§§HR${i}§§`, hrTokens[i]);
  }

  for (let i = 0; i < tableTokens.length; i++) {
    result = result.replace(`§§TABLE${i}§§`, tableTokens[i]);
  }

  return result;
};

/**
 * Processes a single line.
 *
 * @param line - raw line
 * @returns HTML for the line
 */
function processLine(line: string): string {
  if (line.startsWith("# ")) return wrap("md-h1", line);
  if (line.startsWith("## ")) return wrap("md-h2", line);
  if (line.startsWith("### ")) return wrap("md-h3", line);
  if (line.startsWith("#### ")) return wrap("md-h4", line);
  if (line.startsWith("##### ")) return wrap("md-h5", line);
  if (line.startsWith("###### ")) return wrap("md-h6", line);

  if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")) {
    const prefix = line[0];
    const rest = line.slice(2);
    const processed = processInline(rest);
    return `<span class="md-list">${prefix}</span> ${processed}`;
  }

  if (line.startsWith("&gt; ")) {
    const content = line.slice(5);
    const processed = processInline(content);
    return wrap("md-quote", "&gt; " + processed);
  }

  return line;
}

/**
 * Wraps content in a span with the given CSS class.
 *
 * @param cls - CSS class name.
 * @param content - Content to wrap.
 * @returns HTML span element.
 */
function wrap(cls: string, content: string) {
  return `<span class="${cls}">${content}</span>`;
}

/**
 * Processes inline markup.
 *
 * @param text - inline text
 * @returns HTML
 */
function processInline(text: string): string {
  // Protect existing spans first
  const { tokenized: spanTokenized, tokens: spanTokens } = extractSpans(text);

  // Also protect table tokens if any slipped through (defensive)
  if (spanTokenized.includes("§§TABLE"))
    return restoreSpans(spanTokenized, spanTokens);

  // First, protect links so they can be nested inside other inline (e.g., italic)
  // Balanced paren URLs like Shadow_(psychology) need depth counting, not [^)]+
  const linkTokens: string[] = [];
  let linkIdx = 0;
  const linkTokenized = replaceLinksBalanced(spanTokenized, (raw) => {
    const t = `§§LINK${linkIdx}§§`;
    linkTokens.push(raw);
    linkIdx++;
    return t;
  });

  // Patterns in priority order (lower index = higher priority) — link already protected
  const patterns: { name: string; regex: RegExp; cls: string }[] = [
    { name: "code", regex: /`([^`]+)`/g, cls: "md-code" },
    {
      name: "bold-italic",
      regex: /\*\*\*([^*]+)\*\*\*/g,
      cls: "md-bold-italic",
    },
    { name: "bold", regex: /\*\*([^*]+)\*\*/g, cls: "md-bold" },
    { name: "underline", regex: /__([^_]+)__/g, cls: "md-underline" },
    { name: "strike", regex: /~~([^~]+)~~/g, cls: "md-strike" },
    { name: "italic-star", regex: /\*([^*]+)\*/g, cls: "md-italic" },
    { name: "italic-underscore", regex: /_([^_]+)_/g, cls: "md-italic" },
  ];

  let tokenized = linkTokenized;
  let tokens = [...spanTokens];
  // Keep link tokens separate for later restore
  const restoreLinks = (s: string): string => {
    let out = s;
    for (let i = 0; i < linkTokens.length; i++) {
      out = out.replace(
        `§§LINK${i}§§`,
        `<span class="md-link">${linkTokens[i]}</span>`,
      );
    }
    return out;
  };

  type Match = {
    start: number;
    end: number;
    raw: string;
    cls: string;
    priority: number;
  };
  const candidates: Match[] = [];

  for (let p = 0; p < patterns.length; p++) {
    const { regex, cls } = patterns[p];
    regex.lastIndex = 0;
    let m: RegExpExecArray | null;

    while ((m = regex.exec(tokenized)) !== null) {
      candidates.push({
        start: m.index,
        end: m.index + m[0].length,
        raw: m[0],
        cls,
        priority: p,
      });

      regex.lastIndex = m.index + 1;
    }
  }

  if (candidates.length === 0) {
    return restoreSpans(restoreLinks(tokenized), tokens);
  }

  // Sort candidates: by start asc, then priority asc, then longer matches
  candidates.sort((a, b) => {
    if (a.start !== b.start) return a.start - b.start;
    if (a.priority !== b.priority) return a.priority - b.priority;
    return b.end - b.start - (a.end - a.start);
  });

  // Greedily pick non-overlapping matches
  const picks: Match[] = [];
  let cursor = 0;
  for (const c of candidates) {
    if (c.start >= cursor) {
      picks.push(c);
      cursor = c.end;
    }
  }

  // Build output
  let out = "";
  let pos = 0;
  for (const pck of picks) {
    if (pos < pck.start) out += tokenized.slice(pos, pck.start);
    out += `<span class="${pck.cls}">${pck.raw}</span>`;
    pos = pck.end;
  }
  if (pos < tokenized.length) out += tokenized.slice(pos);

  return restoreSpans(restoreLinks(out), tokens);
}

/**
 * Protects existing spans.
 *
 * @param input - HTML with spans
 * @returns tokenized string and tokens
 */
function extractSpans(input: string) {
  const tokens: string[] = [];
  let idx = 0;
  const tokenized = input.replace(/<span[^>]*>[\s\S]*?<\/span>/g, (m) => {
    const t = `§§SPAN${idx}§§`;
    tokens.push(m);
    idx++;
    return t;
  });
  return { tokenized, tokens };
}

/**
 * Restores protected spans.
 *
 * @param input - string with tokens
 * @param tokens - original spans
 * @returns restored HTML
 */
function restoreSpans(input: string, tokens: string[]) {
  let out = input;
  for (let i = 0; i < tokens.length; i++) {
    out = out.replace(`§§SPAN${i}§§`, tokens[i]);
  }
  return out;
}

/**
 * Replaces links with balanced handling.
 *
 * @param text - text to scan
 * @param replacer - called with raw link
 * @returns text with links replaced
 */
function replaceLinksBalanced(
  text: string,
  replacer: (raw: string) => string,
): string {
  let out = "";
  let i = 0;
  while (i < text.length) {
    const open = text.indexOf("[", i);
    if (open === -1) {
      out += text.slice(i);
      break;
    }
    const close = text.indexOf("]", open + 1);
    if (close === -1 || text[close + 1] !== "(") {
      out += text.slice(i, close === -1 ? open + 1 : close + 1);
      i = close === -1 ? open + 1 : close + 1;
      continue;
    }
    let depth = 1;
    let j = close + 2;
    while (j < text.length && depth > 0) {
      const ch = text[j];
      if (ch === "(") depth++;
      else if (ch === ")") depth--;
      j++;
    }
    if (depth !== 0) {
      out += text.slice(i, close + 1);
      i = close + 1;
      continue;
    }
    const raw = text.slice(open, j);
    out += text.slice(i, open) + replacer(raw);
    i = j;
  }
  return out;
}

/**
 * Replaces span links with anchors.
 *
 * @param html - HTML with span links
 * @returns HTML with anchors
 */
function replaceSpanLinks(html: string): string {
  const prefix = '<span class="md-link">';
  const suffix = "</span>";
  let out = "";
  let i = 0;
  while (i < html.length) {
    const start = html.indexOf(prefix, i);
    if (start === -1) {
      out += html.slice(i);
      break;
    }
    out += html.slice(i, start);
    const innerStart = start + prefix.length;
    const end = html.indexOf(suffix, innerStart);
    if (end === -1) {
      out += html.slice(start);
      break;
    }
    const inner = html.slice(innerStart, end);
    const link = extractLinkBalanced(inner);
    if (link) {
      out += `<a href="${link.url}" target="_blank" class="md-link">${link.title}</a>`;
    } else {
      out += html.slice(start, end + suffix.length);
    }
    i = end + suffix.length;
  }
  return out;
}

/**
 * Extracts link title and url with balanced parens.
 *
 * @param text - raw [title](url)
 * @returns title and url or null
 */
function extractLinkBalanced(
  text: string,
): { title: string; url: string } | null {
  const open = text.indexOf("[");
  const close = text.indexOf("]", open + 1);
  if (open === -1 || close === -1 || text[close + 1] !== "(") return null;
  const title = text.slice(open + 1, close);
  let depth = 1;
  let j = close + 2;
  while (j < text.length && depth > 0) {
    const ch = text[j];
    if (ch === "(") depth++;
    else if (ch === ")") depth--;
    j++;
  }
  if (depth !== 0) return null;
  const url = text.slice(close + 2, j - 1);
  return { title, url };
}
