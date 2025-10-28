/**
 * @fileoverview API routes for stem player operations.
 * Handles GraphQL data fetching for stem player functionality.
 */

import express from "express";
import { fetchListSongs } from "../src/utils/api.js";

const router = express.Router();

/**
 * GET /api/stem-player?query=listSongs
 * Fetches list of songs from GraphQL server.
 */
router.get("/", async (req, res) => {
  const { query } = req.query;

  if (query === "listSongs") {
    try {
      const result = await fetchListSongs();

      if (result.success) {
        res.json({
          success: true,
          data: result.data,
        });
      } else {
        res.status(result.statusCode || 500).json({
          success: false,
          error: result.error,
        });
      }
    } catch (_error) {
      res.status(500).json({
        success: false,
        error: "Internal server error",
      });
    }
  } else {
    res.status(400).json({
      success: false,
      error: "Invalid query parameter. Supported: listSongs",
    });
  }
});

export default router;
