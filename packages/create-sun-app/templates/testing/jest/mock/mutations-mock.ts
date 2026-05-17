// Mock for server actions/mutations
export const mockCreateBlogPost = jest.fn();
export const mockMutateCreateBlogPost = jest.fn();

const originalApi = jest.requireActual("~/utils/api");

jest.mock("~/server/actions/blog-post", () => ({
  createBlogPost: mockCreateBlogPost,
}));

jest.mock("~/utils/api", () => ({
  ...originalApi,
  mutateCreateBlogPost: mockMutateCreateBlogPost,
}));
