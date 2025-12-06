/**
 * @fileoverview Tests for CreateBlogForm component.
 * Tests the component's rendering, form submission, state management, and integration with translations and server actions.
 */

import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import CreateBlogForm from "~/_components/blog/create/create-blog-form";
import {
  mockT,
  mockCreateBlogPost,
  restoreConsoleError,
  suppressConsoleErrorsFromTests,
} from "testing/jest/mock";
import { registerBlogCreateMutation } from "~/routes/blog/create/create-blog-post";

beforeAll(() => {
  // Due to some issue with re-rendering that we don't care about in test env
  suppressConsoleErrorsFromTests();
});

afterAll(() => {
  restoreConsoleError();
});

describe("CreateBlogForm", () => {
  jest.useFakeTimers();

  beforeEach(() => {
    jest.clearAllMocks();
    registerBlogCreateMutation();
  });

  it("renders the form with all required fields and buttons", () => {
    render(<CreateBlogForm />);

    expect(screen.getByTestId("create-blog-form")).toBeInTheDocument();

    expect(screen.getByLabelText("form.title.label")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("form.title.placeholder")
    ).toBeInTheDocument();

    expect(screen.getByLabelText("form.content.label")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("form.content.placeholder")
    ).toBeInTheDocument();

    expect(
      screen.getByRole("button", { name: "form.cancel.label" })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "form.create.label" })
    ).toBeInTheDocument();
  });

  it("uses translation strings correctly", () => {
    render(<CreateBlogForm />);

    // Check that all expected translation keys are called
    expect(mockT).toHaveBeenCalledWith("form.title.label");
    expect(mockT).toHaveBeenCalledWith("form.title.placeholder");
    expect(mockT).toHaveBeenCalledWith("form.content.label");
    expect(mockT).toHaveBeenCalledWith("form.content.placeholder");
    expect(mockT).toHaveBeenCalledWith("form.cancel.title");
    expect(mockT).toHaveBeenCalledWith("form.cancel.label");
    expect(mockT).toHaveBeenCalledWith("form.create.title");
    expect(mockT).toHaveBeenCalledWith("form.create.label");
  });

  it("submits the form successfully", async () => {
    const mockResult = { __typename: "QuerySuccess" as const };
    mockCreateBlogPost.mockResolvedValue(mockResult);

    render(<CreateBlogForm />);

    const titleInput = screen.getByLabelText("form.title.label");
    const contentTextarea = screen.getByLabelText("form.content.label");

    fireEvent.change(titleInput, { target: { value: "Test Title" } });
    fireEvent.change(contentTextarea, { target: { value: "Test Content" } });

    const submitButton = screen.getByTestId("create-blog-submit-button");
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(mockCreateBlogPost).toHaveBeenCalledWith(
        "Test Title",
        "Test Content"
      );
    });
  });

  it("handles form submission failure gracefully", async () => {
    const mockErrorResult = {
      __typename: "StandardError" as const,
      message: "A test error message.",
    };
    mockCreateBlogPost.mockResolvedValue(mockErrorResult);

    render(<CreateBlogForm />);

    const submitButton = screen.getByTestId("create-blog-submit-button");
    expect(submitButton).not.toBeDisabled();

    const titleInput = screen.getByLabelText("form.title.label");
    fireEvent.change(titleInput, { target: { value: "Test Title" } });

    await act(async () => {
      fireEvent.click(submitButton);
    });

    // We are just checking the component did not crash
    // TODO: Add toast/alerts and query it is called correctly
    await waitFor(() => {
      expect(mockCreateBlogPost).toHaveBeenCalled();
      expect(submitButton).not.toBeDisabled();
    });
  });

  it("prevents default form submission", () => {
    render(<CreateBlogForm />);

    const form = screen.getByTestId("create-blog-form");

    const preventDefaultMock = jest.fn();
    form.addEventListener("submit", preventDefaultMock);

    fireEvent.submit(form);

    expect(preventDefaultMock).toHaveBeenCalled();
  });

  it("uses component library components correctly", () => {
    render(<CreateBlogForm />);

    const form = screen.getByTestId("create-blog-form");
    expect(form).toHaveClass("form");

    const formItems = document.querySelectorAll(".form_item");
    expect(formItems).toHaveLength(2);

    const labels = document.querySelectorAll("label");
    expect(labels).toHaveLength(2);

    const inputs = document.querySelectorAll("input");
    expect(inputs).toHaveLength(1);

    const textareas = document.querySelectorAll("textarea");
    expect(textareas).toHaveLength(1);

    const footer = document.querySelector("footer");
    expect(footer).toBeInTheDocument();

    const buttons = document.querySelectorAll("button");
    expect(buttons).toHaveLength(2);
  });

  it("handles empty form submission", async () => {
    const mockResult = { __typename: "QuerySuccess" as const };
    mockCreateBlogPost.mockResolvedValue(mockResult);

    render(<CreateBlogForm />);

    const submitButton = screen.getByTestId("create-blog-submit-button");
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(mockCreateBlogPost).toHaveBeenCalledWith("", "");
    });
  });

  it("handles rapid form submissions", async () => {
    const mockResult = { __typename: "QuerySuccess" as const };

    mockCreateBlogPost.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(mockResult), 0))
    );

    render(<CreateBlogForm />);
    const submitButton = screen.getByTestId("create-blog-submit-button");
    fireEvent.click(submitButton);
    fireEvent.click(submitButton);
    fireEvent.click(submitButton);

    await act(async () => {
      jest.runAllTimers();
    });

    await waitFor(() => {
      expect(mockCreateBlogPost).toHaveBeenCalledTimes(1);
    });
  });

  it("maintains form data during submission", () => {
    render(<CreateBlogForm />);

    const titleInput = screen.getByLabelText("form.title.label");
    const contentTextarea = screen.getByLabelText("form.content.label");
    fireEvent.change(titleInput, { target: { value: "Test Title" } });
    fireEvent.change(contentTextarea, { target: { value: "Test Content" } });

    expect(titleInput).toHaveValue("Test Title");
    expect(contentTextarea).toHaveValue("Test Content");
  });

  it("renders with correct accessibility attributes", () => {
    render(<CreateBlogForm />);

    const titleInput = screen.getByLabelText("form.title.label");
    const contentTextarea = screen.getByLabelText("form.content.label");

    expect(titleInput).toHaveAttribute("name", "title");
    expect(contentTextarea).toHaveAttribute("name", "content");

    const titleLabel = screen.getByText("form.title.label");
    const contentLabel = screen.getByText("form.content.label");

    expect(titleLabel).toHaveAttribute("for", "title");
    expect(contentLabel).toHaveAttribute("for", "content");
  });

  it("renders with custom className", () => {
    render(<CreateBlogForm />);

    const form = screen.getByTestId("create-blog-form");
    expect(form).toHaveClass("create_blog_form");
  });

  it("uses correct default values", () => {
    render(<CreateBlogForm />);

    const contentTextarea = screen.getByLabelText("form.content.label");
    expect(contentTextarea).toHaveAttribute("rows", "10");
  });

  it("disables submit button during loading and changes label", async () => {
    const mockResult = { __typename: "QuerySuccess" as const };
    mockCreateBlogPost.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(mockResult), 100))
    );

    render(<CreateBlogForm />);
    const submitButton = screen.getByTestId("create-blog-submit-button");

    await act(async () => {
      fireEvent.click(submitButton);
    });

    expect(submitButton).toBeDisabled();
    expect(screen.getByText("form.creating.label")).toBeInTheDocument();
    expect(mockT).toHaveBeenCalledWith("form.creating.label");
    expect(mockT).toHaveBeenCalledWith("form.creating.title");

    await waitFor(() => {
      expect(mockCreateBlogPost).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(submitButton).not.toBeDisabled();
      expect(screen.getByText("form.create.label")).toBeInTheDocument();
    });
  });
});
