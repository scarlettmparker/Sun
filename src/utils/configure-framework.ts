import { configurePageData } from "@sun/ssr";

configurePageData({
  perPatternTtl: {
    "/currentRoles": 60000,
    "/currentUser": 60000,
    "/levelColours": 60000,
    "/themes": 60000,
  },
});
