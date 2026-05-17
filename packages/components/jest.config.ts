import type { Config } from "jest";

const config: Config = {
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/testing/jest/setup.ts"],
  rootDir: ".",
  moduleNameMapper: {
    // 1. STUBS FIRST: Intercept and mock CSS file requests immediately
    "\\.module\\.css$": "<rootDir>/testing/jest/mock/css-module-mock.ts",
    "\\.css$": "<rootDir>/testing/jest/mock/css-module-mock.ts",

    // 2. React single-source forcing
    "^react$": "<rootDir>/node_modules/react",
    "^react-dom$": "<rootDir>/node_modules/react-dom",
    "^react-dom/(.*)$": "<rootDir>/node_modules/react-dom/$1",

    // 3. Path aliases (Only runs if the file wasn't caught by the CSS rules)
    "^~/(.*)$": "<rootDir>/src/$1",
    "^@sun/components$": "<rootDir>/src/index.ts",
  },
  testMatch: ["<rootDir>/__tests__/**/*.(ts|tsx)"],
  testPathIgnorePatterns: ["/node_modules/", "\\.d\\.ts$"],
  moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json", "node"],
  transform: {
    "^.+\\.(ts|tsx)$": [
      "ts-jest",
      {
        useESM: true,
        tsconfig: "<rootDir>/tsconfig.json",
      },
    ],
  },
};

export default config;
