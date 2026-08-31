/**
 * Checks whether a candidate IP falls within a CIDR range or matches a
 * wildcard pattern.
 *
 * Wildcard patterns use `*` as a single-octet placeholder, e.g. `"5.*"`
 * matches any IP starting with `5.`, and `"192.168.0.*"` matches only the
 * last octet. A bare `"*"` matches every IP.
 *
 * @param candidateIp - the IP to test (v4 or v6)
 * @param pattern - the range in CIDR notation (`"192.168.1.0/24"`) or a
 *                wildcard pattern (`"5.*"` , `"*"`)
 * @returns true if the IP is within the range
 */
export function ipMatchesCidr(candidateIp: string, pattern: string): boolean {
  if (pattern === "*") return true;

  if (pattern.includes("*")) {
    return matchesWildcard(candidateIp, pattern);
  }

  const parts = pattern.split("/");
  const prefix = parseInt(parts[1], 10);
  const baseIp = parts[0];
  const isV6 = baseIp.includes(":");

  if (isV6) {
    const candidate = ip6toBigInt(candidateIp);
    const base = ip6toBigInt(baseIp);
    const mask = BigInt(-1) << BigInt(128 - prefix);
    return (candidate & mask) === (base & mask);
  }

  const candidate = ip4toInt(candidateIp);
  const base = ip4toInt(baseIp);
  const mask = ~((1 << (32 - prefix)) - 1) >>> 0;
  return (candidate & mask) === (base & mask);
}

/**
 * Matches an IP against a wildcard pattern.
 *
 * @param ip - candidate IP
 * @param pattern - wildcard pattern
 * @returns true on match
 */
function matchesWildcard(ip: string, pattern: string): boolean {
  const ipOctets = ip.split(".");
  const patternOctets = pattern.split(".");

  for (let i = 0; i < patternOctets.length; i++) {
    if (patternOctets[i] === "*") continue;
    if (ipOctets[i] === undefined) return false;
    if (ipOctets[i] !== patternOctets[i]) return false;
  }
  return true;
}

/**
 * Converts an IPv4 string to a 32-bit integer.
 *
 * @param ip - dotted IPv4
 * @returns packed integer
 */
function ip4toInt(ip: string): number {
  const octets = ip.split(".");
  return (
    ((parseInt(octets[0], 10) << 24) |
      (parseInt(octets[1], 10) << 16) |
      (parseInt(octets[2], 10) << 8) |
      parseInt(octets[3], 10)) >>>
    0
  );
}

/**
 * Converts an IPv6 string to a bigint.
 *
 * @param ip - colon-separated IPv6
 * @returns packed bigint
 */
function ip6toBigInt(ip: string): bigint {
  const parts = ip.split(":");
  let hex = "";
  for (const part of parts) {
    if (part === "") continue;
    hex += part.padStart(4, "0");
  }
  return BigInt("0x" + hex);
}
