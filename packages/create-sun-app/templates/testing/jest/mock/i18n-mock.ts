// Mock for react-i18next
export const mockT = jest.fn((key: string) => key);

export const useTranslation = jest.fn(() => ({
  t: mockT,
}));

// Mock i18next
jest.mock("i18next", () => ({
  createInstance: jest.fn(() => ({
    use: jest.fn().mockReturnThis(),
    init: jest.fn(),
  })),
  use: jest.fn().mockReturnThis(),
  init: jest.fn(),
}));

// Setup global mocks
jest.mock("react-i18next", () => ({
  useTranslation,
  initReactI18next: jest.fn(() => ({
    init: jest.fn(),
  })),
}));
