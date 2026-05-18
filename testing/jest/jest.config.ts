import type { Config } from "jest";

const config: Config = {
  transform: {
    "^.+\\.(t|j)sx?$": "@swc/jest",
  },
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/testing/jest/setup.ts"],
  moduleNameMapper: {
    "\\.module\\.css$": "<rootDir>/testing/jest/mock/css-module-mock.ts",
    "\\.css$": "<rootDir>/testing/jest/mock/css-module-mock.ts",
    "^react$": "<rootDir>/node_modules/react",
    "^react-dom$": "<rootDir>/node_modules/react-dom",
    "^react-dom/(.*)$": "<rootDir>/node_modules/react-dom/$1",
    "^react/jsx-runtime$": "<rootDir>/node_modules/react/jsx-runtime",
    "^~/(.*)$": "<rootDir>/src/$1",
    "^testing/(.*)$": "<rootDir>/testing/$1",
    "^@sun/components$": "<rootDir>/packages/components/src/index.ts",
    "^@sun/components/(.*)$": "<rootDir>/packages/components/src/$1",
  },
  testMatch: ["<rootDir>/__tests__/**/*.(ts|tsx)"],
  testPathIgnorePatterns: ["/node_modules/", "\\.d\\.ts$"],
  collectCoverage: true,
  collectCoverageFrom: [
    "src/**/*.(ts|tsx)",
    "!src/**/*.d.ts",
    "!src/generated/**/*",
  ],
  coverageReporters: ["text", "html", "text-summary"],
  coverageDirectory: "coverage",
  moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json", "node"],
};

export default config;
