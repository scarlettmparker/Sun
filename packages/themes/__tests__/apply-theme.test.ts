import {
  applyTheme,
  loadPersistedTheme,
  clearTheme,
  THEME_CSS_VAR_MAP,
  THEME_STORAGE_KEY,
  type Theme,
} from "../src";

const greekTheme: Theme = {
  primary: "#1d4ed8",
  "primary-hover": "#3b82f6",
  "primary-active": "#1e3a8a",
  secondary: "#ffffff",
  "secondary-hover": "#f5f5f5",
  accent: "#bfdbfe",
  "accent-hover": "#dbeafe",
  tertiary: "#6d28d9",
  "tertiary-hover": "#7c3aed",
};

const defaultTheme: Theme = {
  primary: "#d90429",
  "primary-hover": "#fb3758",
  "primary-active": "#a0031d",
  secondary: "#ffffff",
  "secondary-hover": "#f5f5f5",
  accent: "#ffdaad",
  "accent-hover": "#ffe3c2",
  tertiary: "#d03991",
  "tertiary-hover": "#dc6aad",
};

function cssVar(key: keyof typeof THEME_CSS_VAR_MAP): string {
  return document.documentElement.style.getPropertyValue(THEME_CSS_VAR_MAP[key]);
}

describe("applyTheme", () => {
  beforeEach(() => {
    document.documentElement.style.cssText = "";
    window.localStorage.clear();
  });

  it("writes the greek theme values to the matching CSS custom properties", () => {
    applyTheme(greekTheme);

    expect(cssVar("primary")).toBe("#1d4ed8");
    expect(cssVar("primary-hover")).toBe("#3b82f6");
    expect(cssVar("accent")).toBe("#bfdbfe");
    expect(cssVar("tertiary")).toBe("#6d28d9");
  });

  it("persists the applied theme to localStorage", () => {
    applyTheme(greekTheme);

    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe(JSON.stringify(greekTheme));
  });

  it("only overrides the properties present in a partial payload", () => {
    const partial: Theme = { primary: "#1d4ed8", secondary: "#ffffff" };

    applyTheme(partial);

    expect(cssVar("primary")).toBe("#1d4ed8");
    expect(cssVar("secondary")).toBe("#ffffff");
    expect(cssVar("tertiary")).toBe("");
    expect(cssVar("accent")).toBe("");
  });

  it("overrides a previously applied theme", () => {
    applyTheme(greekTheme);
    applyTheme(defaultTheme);

    expect(cssVar("primary")).toBe("#d90429");
    expect(cssVar("tertiary")).toBe("#d03991");
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe(JSON.stringify(defaultTheme));
  });

  it("ignores unknown keys rather than creating arbitrary CSS variables", () => {
    applyTheme({ primary: "#1d4ed8", ...{ bogus: "#000000" } });

    expect(cssVar("primary")).toBe("#1d4ed8");
    expect(document.documentElement.style.getPropertyValue("--bogus")).toBe("");
  });
});

describe("loadPersistedTheme", () => {
  beforeEach(() => {
    document.documentElement.style.cssText = "";
    window.localStorage.clear();
  });

  it("reapplies a theme stored in localStorage", () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(greekTheme));

    loadPersistedTheme();

    expect(cssVar("primary")).toBe("#1d4ed8");
    expect(cssVar("accent")).toBe("#bfdbfe");
  });

  it("does nothing when no theme is persisted", () => {
    loadPersistedTheme();

    expect(cssVar("primary")).toBe("");
  });

  it("ignores a malformed persisted theme", () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, "{not json");

    loadPersistedTheme();

    expect(cssVar("primary")).toBe("");
  });
});

describe("clearTheme", () => {
  it("removes the overrides and the persisted theme", () => {
    applyTheme(greekTheme);

    clearTheme();

    expect(cssVar("primary")).toBe("");
    expect(cssVar("tertiary")).toBe("");
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBeNull();
  });
});
