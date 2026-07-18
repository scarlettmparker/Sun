import type { Config } from "jest";

const config: Config = {
  transform: {
    "^.+\\.(t|j)sx?$": "@swc/jest",
  },
  // Workspace @sun packages ship TS source; transform them instead of
  // treating them as opaque node_modules.
  transformIgnorePatterns: ["/node_modules/(?!@sun/)"],
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/testing/jest/setup.ts"],
  rootDir: ".",
  moduleNameMapper: {
    "\\.module\\.css$": "<rootDir>/testing/jest/mock/css-module-mock.ts",
    "\\.css$": "<rootDir>/testing/jest/mock/css-module-mock.ts",
    "^react$": "<rootDir>/node_modules/react",
    "^react-dom$": "<rootDir>/node_modules/react-dom",
    "^react-dom/(.*)$": "<rootDir>/node_modules/react-dom/$1",
    "^~/(.*)$": "<rootDir>/src/$1",
    "^@sun/components$": "<rootDir>/src/index.ts",
  },
  testMatch: ["<rootDir>/__tests__/**/*.(ts|tsx)"],
  testPathIgnorePatterns: ["/node_modules/", "\\.d\\.ts$"],
  moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json", "node"],
};

export default config;
