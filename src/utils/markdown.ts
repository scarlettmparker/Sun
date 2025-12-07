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

  return escaped.split("\n").map(processLine).map(processInline).join("\n");
};

/**
 * Strips markdown syntax from the highlighted HTML, leaving only styled spans.
 */
export const stripMarkdown = (html: string): string => {
  let result = html;

  result = result.replace(
    /<span class="md-h1">#\s+(.*?)<\/span>/g,
    '<span class="md-h1">$1</span>'
  );
  result = result.replace(
    /<span class="md-h2">##\s+(.*?)<\/span>/g,
    '<span class="md-h2">$1</span>'
  );
  result = result.replace(
    /<span class="md-h3">###\s+(.*?)<\/span>/g,
    '<span class="md-h3">$1</span>'
  );
  result = result.replace(
    /<span class="md-h4">####\s+(.*?)<\/span>/g,
    '<span class="md-h4">$1</span>'
  );
  result = result.replace(
    /<span class="md-h5">#####\s+(.*?)<\/span>/g,
    '<span class="md-h5">$1</span>'
  );
  result = result.replace(
    /<span class="md-h6">######\s+(.*?)<\/span>/g,
    '<span class="md-h6">$1</span>'
  );

  result = result.replace(/<span class="md-list">[-*+]\s*<\/span>\s*/g, "");

  result = result.replace(
    /<span class="md-quote">&gt;\s+(.*?)<\/span>/g,
    '<span class="md-quote">$1</span>'
  );

  result = result.replace(
    /<span class="md-code">`([^`]+)`<\/span>/g,
    '<span class="md-code">$1</span>'
  );
  result = result.replace(
    /<span class="md-bold-italic">\*\*\*([^*]+)\*\*\*<\/span>/g,
    '<span class="md-bold-italic">$1</span>'
  );
  result = result.replace(
    /<span class="md-bold">\*\*([^*]+)\*\*<\/span>/g,
    '<span class="md-bold">$1</span>'
  );
  result = result.replace(
    /<span class="md-underline">__([^_]+)__<\/span>/g,
    '<span class="md-underline">$1</span>'
  );
  result = result.replace(
    /<span class="md-strike">~~([^~]+)~~<\/span>/g,
    '<span class="md-strike">$1</span>'
  );
  result = result.replace(
    /<span class="md-italic">\*([^*]+)\*<\/span>/g,
    '<span class="md-italic">$1</span>'
  );
  result = result.replace(
    /<span class="md-italic">_([^_]+)_<\/span>/g,
    '<span class="md-italic">$1</span>'
  );
  result = result.replace(
    /<span class="md-link">\[([^\]]+)\]\(([^)]+)\)<\/span>/g,
    '<span class="md-link">$1</span>'
  );

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
 * @param cls - The CSS class name.
 * @param content - The content to wrap.
 * @returns HTML span element.
 */
function wrap(cls: string, content: string) {
  return `<span class="${cls}">${content}</span>`;
}

/**
 * Processes inline markdown elements within text, wrapping them with styled spans.
 * Handles overlapping matches by prioritizing patterns and using greedy selection.
 * @param text - The text to process for inline elements.
 * @returns The processed text with HTML spans for inline styling.
 */
function processInline(text: string): string {
  // Protect existing spans first
  const { tokenized, tokens } = extractSpans(text);

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
 * @param input - The string with tokens to replace.
 * @param tokens - Array of original span strings.
 * @returns The string with spans restored.
 */
function restoreSpans(input: string, tokens: string[]) {
  let out = input;
  for (let i = 0; i < tokens.length; i++) {
    out = out.replace(`§§SPAN${i}§§`, tokens[i]);
  }
  return out;
}
