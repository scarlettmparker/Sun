/**
 * Markdown processing utilities for highlighting and stripping syntax.
 */

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
  result = result.replace(
    /<span class="md-link">\[([^\]]+)\]\(([^)]+)\)<\/span>/g,
    '<a href="$2" target="_blank" class="md-link">$1</a>',
  );

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
 * Processes a single line of markdown text, wrapping headers, lists, and quotes with styled spans.
 *
 * @param line - Line of text to process.
 * @returns Processed line with HTML spans for styling.
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
 * Processes inline markdown elements within text, wrapping them with styled spans.
 * Handles overlapping matches by prioritizing patterns and using greedy selection.
 *
 * @param text - Text to process for inline elements.
 * @returns Processed text with HTML spans for inline styling.
 */
function processInline(text: string): string {
  // Protect existing spans first
  const { tokenized, tokens } = extractSpans(text);

  // Also protect table tokens if any slipped through (defensive)
  if (tokenized.includes("§§TABLE")) return restoreSpans(tokenized, tokens);

  // Patterns in priority order (lower index = higher priority)
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
    { name: "link", regex: /\[([^\]]+)\]\(([^)]+)\)/g, cls: "md-link" },
  ];

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
    return restoreSpans(tokenized, tokens);
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

  return restoreSpans(out, tokens);
}

/**
 * Replaces existing span elements with tokens to protect them during processing.
 *
 * @param input - Input string containing spans.
 * @returns Object with tokenized string and array of original spans.
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
 * Restores the original span elements from tokens.
 * @param input - String with tokens to replace.
 * @param tokens - Array of original span strings.
 * @returns String with spans restored.
 */
function restoreSpans(input: string, tokens: string[]) {
  let out = input;
  for (let i = 0; i < tokens.length; i++) {
    out = out.replace(`§§SPAN${i}§§`, tokens[i]);
  }
  return out;
}
