/**
 * @fileoverview API routes for stem player operations.
 * Handles GraphQL data fetching for stem player functionality.
 */

import express from "express";
import { fetchList } from "../src/utils/api.ts";
import { handleQuery } from "../utils/query-handler.js";
const router = express.Router();

/**
 * GET /api/stem-player
 * Fetches list of songs from GraphQL server.
 */
router.get("/", async (req, res) => {
  const { query } = req.query;

  switch (query) {
    case "list":
      return handleQuery(fetchList, res);
    default:
      res.status(400).json({
        success: false,
        error: "Invalid query parameter. Supported: list",
      });
  }
});

export default router;
