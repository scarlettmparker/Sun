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
  collectCoverageFrom: ["src/**/*.(ts|tsx)", "!src/**/*.d.ts"],
  moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json", "node"],
};

export default config;
