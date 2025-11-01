import type { Config } from "jest";

const config: Config = {
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/testing/jest/setup.ts"],
  moduleNameMapper: {
    "^~/(.*)$": "<rootDir>/src/$1",
    "^testing/(.*)$": "<rootDir>/testing/$1",
    "\\.(css|less|scss|sass)$":
      "<rootDir>/testing/jest/mock/css-module-mock.ts",
  },
  testMatch: ["<rootDir>/__tests__/**/*.(ts|tsx)"],
  testPathIgnorePatterns: ["/node_modules/", "\\.d\\.ts$"],
  collectCoverage: true,
  collectCoverageFrom: ["src/**/*.(ts|tsx)", "!src/**/*.d.ts"],
  coverageReporters: ["text", "html", "text-summary"],
  coverageDirectory: "coverage",
  moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json", "node"],
};

export default config;
