/**
 * @fileoverview Configuration settings for the server, loaded from environment variables.
 * @module config
 */

import { config } from "dotenv";

/**
 * Checks if the current environment is production.
 * @type {boolean}
 */
export const isProduction = process.env.NODE_ENV === "production";

// Load environment variables based on the environment.
config({
  path: `.env${isProduction ? ".production" : ""}`,
});

/**
 * The port on which the server will listen. Defaults to 5173.
 * @type {number}
 */
export const port = parseInt(process.env.SERVER_PORT || "5173", 10);

/**
 * The host on which the server will start. Defaults to localhost.
 * @type {number}
 */

export const host = process.env.SERVER_HOST || "0.0.0.0";

/**
 * The base URL path for the server. Defaults to "/".
 * @type {string}
 * @example http://int.scarlettparker.co.uk
 */
export const base = process.env.VITE_SERVER_BASE || "/";

/**
 * The host of the backend API. Defaults to "0.0.0.0".
 * @type {string}
 */
export const backendHost = process.env.BACKEND_HOST || "0.0.0.0";

/**
 * The port of the backend API. Defaults to 443.
 * @type {number}
 */
export const backendPort = parseInt(process.env.BACKEND_PORT || "443", 10);

/**
 * The path to the client manifest file for production builds.
 * @type {string}
 */
export const manifestPath = "./dist/client/.vite/manifest.json";

/**
 * Token required for hub control mutations. Empty disables the check.
 * @type {string}
 */
export const hubAdminToken = process.env.HUB_ADMIN_TOKEN || "";

/**
 * Per-app credentials forwarded to the backend as X-Client-Id / X-Client-Secret.
 */
export const clientId = process.env.CLIENT_ID || "sun";
export const clientSecret = process.env.CLIENT_SECRET || "";

/**
 * The app's public base URL, used for gateway-relative redirects.
 */
export const appBaseUrl = process.env.APP_BASE_URL || "http://localhost:5173";
