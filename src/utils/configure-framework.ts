import { configurePageData } from "@sun/ssr";

configurePageData({
  perPatternTtl: {
    "/currentRoles": 60000,
  },
});
