/**
 * @fileoverview Tests for Form components.
 * Tests the Form, FormItem, FormLabel, and FormInput components' rendering, context usage, and attribute passing.
 */

jest.mock("~/components/markdown-editor", () => ({
  default: () => <div data-testid="markdown-editor" />,
}));

import { render, screen } from "@testing-library/react";
import {
  Form,
  FormField,
  FormLabel,
  FormItem,
  FormFooter,
} from "~/components/form";
import Input from "~/components/input";
import TextArea from "~/components/textarea";
import Select from "~/components/select";

describe("Form", () => {
  it("renders form element with correct attributes", () => {
    render(<Form data-testid="form" onSubmit={() => {}} />);
    const form = screen.getByTestId("form");
    expect(form.tagName).toBe("FORM");
  });

  it("passes through form attributes correctly", () => {
    render(
      <Form
        method="post"
        action="/submit"
        className="custom-form"
        data-testid="form"
      />
    );
    const form = screen.getByTestId("form");
    expect(form).toHaveAttribute("method", "post");
    expect(form).toHaveAttribute("action", "/submit");
    expect(form).toHaveClass("custom-form");
  });

  it("renders children correctly", () => {
    render(
      <Form>
        <div data-testid="child">Child content</div>
      </Form>
    );
    expect(screen.getByTestId("child")).toBeInTheDocument();
  });
});

describe("FormField", () => {
  it("renders div element with correct attributes", () => {
    render(
      <FormField data-testid="form-field">
        <div />
      </FormField>
    );
    const field = screen.getByTestId("form-field");
    expect(field.tagName).toBe("DIV");
  });

  it("passes through div attributes correctly", () => {
    render(
      <FormField className="custom-field" role="group" data-testid="form-field">
        <div />
      </FormField>
    );
    const field = screen.getByTestId("form-field");
    expect(field).toHaveClass("custom-field");
    expect(field).toHaveAttribute("role", "group");
  });

  it("renders children correctly", () => {
    render(
      <FormField>
        <span data-testid="child">Child content</span>
      </FormField>
    );
    expect(screen.getByTestId("child")).toBeInTheDocument();
  });

  it("provides name context to children", () => {
    render(
      <FormField name="test-field">
        <FormLabel data-testid="label">Label</FormLabel>
        <FormItem>
          <Input data-testid="input" />
        </FormItem>
      </FormField>
    );
    const label = screen.getByTestId("label");
    const input = screen.getByTestId("input");
    expect(label).toHaveAttribute("for", "test-field");
    expect(input).toHaveAttribute("name", "test-field");
  });

  it("provides empty context when no name is given", () => {
    render(
      <FormField>
        <FormLabel data-testid="label">Label</FormLabel>
        <FormItem>
          <Input data-testid="input" />
        </FormItem>
      </FormField>
    );
    const label = screen.getByTestId("label");
    const input = screen.getByTestId("input");
    expect(label).not.toHaveAttribute("for");
    expect(input).not.toHaveAttribute("name");
  });
});

describe("FormLabel", () => {
  it("renders label element with correct attributes", () => {
    render(<FormLabel data-testid="label">Label text</FormLabel>);
    const label = screen.getByTestId("label");
    expect(label.tagName).toBe("LABEL");
    expect(label).toHaveTextContent("Label text");
  });

  it("passes through label attributes correctly", () => {
    render(
      <FormLabel
        className="custom-label"
        aria-describedby="description"
        data-testid="label"
      >
        Label
      </FormLabel>
    );
    const label = screen.getByTestId("label");
    expect(label).toHaveClass("custom-label");
    expect(label).toHaveAttribute("aria-describedby", "description");
  });

  it("sets htmlFor from context when name is provided", () => {
    render(
      <FormField name="field-name">
        <FormLabel data-testid="label">Label</FormLabel>
      </FormField>
    );
    const label = screen.getByTestId("label");
    expect(label).toHaveAttribute("for", "field-name");
  });

  it("does not set htmlFor when context has no name", () => {
    render(
      <FormField>
        <FormLabel data-testid="label">Label</FormLabel>
      </FormField>
    );
    const label = screen.getByTestId("label");
    expect(label).not.toHaveAttribute("for");
  });

  it("overrides htmlFor prop with context value", () => {
    render(
      <FormField name="context-name">
        <FormLabel htmlFor="prop-name" data-testid="label">
          Label
        </FormLabel>
      </FormField>
    );
    const label = screen.getByTestId("label");
    expect(label).toHaveAttribute("for", "context-name");
  });
});

describe("FormFooter", () => {
  it("renders footer element with correct attributes", () => {
    render(<FormFooter data-testid="footer" />);
    const footer = screen.getByTestId("footer");
    expect(footer.tagName).toBe("FOOTER");
  });

  it("passes through footer attributes correctly", () => {
    render(
      <FormFooter
        className="custom-footer"
        role="contentinfo"
        data-testid="footer"
      />
    );
    const footer = screen.getByTestId("footer");
    expect(footer).toHaveClass("custom-footer");
    expect(footer).toHaveAttribute("role", "contentinfo");
  });

  it("renders children correctly", () => {
    render(
      <FormFooter>
        <button data-testid="child">Submit</button>
      </FormFooter>
    );
    expect(screen.getByTestId("child")).toBeInTheDocument();
  });
});

describe("FormItem", () => {
  it("renders the child element with correct attributes", () => {
    render(
      <FormItem>
        <Input data-testid="input" />
      </FormItem>
    );
    const input = screen.getByTestId("input");
    expect(input.tagName).toBe("INPUT");
    expect(input).toHaveAttribute("type", "text");
  });

  it("passes through child attributes correctly", () => {
    render(
      <FormItem>
        <Input
          type="email"
          placeholder="Enter email"
          required
          className="custom-input"
          data-testid="input"
        />
      </FormItem>
    );
    const input = screen.getByTestId("input");
    expect(input).toHaveAttribute("type", "email");
    expect(input).toHaveAttribute("placeholder", "Enter email");
    expect(input).toHaveAttribute("required");
    expect(input).toHaveClass("custom-input");
  });

  it("sets name from context when name is provided", () => {
    render(
      <FormField name="field-name">
        <FormItem>
          <Input data-testid="input" />
        </FormItem>
      </FormField>
    );
    const input = screen.getByTestId("input");
    expect(input).toHaveAttribute("name", "field-name");
  });

  it("does not set name when context has no name", () => {
    render(
      <FormField>
        <FormItem>
          <Input data-testid="input" />
        </FormItem>
      </FormField>
    );
    const input = screen.getByTestId("input");
    expect(input).not.toHaveAttribute("name");
  });

  it("overrides name prop with context value", () => {
    render(
      <FormField name="context-name">
        <FormItem>
          <Input name="prop-name" data-testid="input" />
        </FormItem>
      </FormField>
    );
    const input = screen.getByTestId("input");
    expect(input).toHaveAttribute("name", "context-name");
  });

  it("handles different input types correctly", () => {
    render(
      <FormField name="test-field">
        <FormItem>
          <Input type="password" data-testid="input" />
        </FormItem>
      </FormField>
    );
    const input = screen.getByTestId("input");
    expect(input).toHaveAttribute("type", "password");
    expect(input).toHaveAttribute("name", "test-field");
  });
});

describe("Form components integration", () => {
  it("works together to create accessible form fields", () => {
    render(
      <Form onSubmit={() => {}}>
        <FormField name="username">
          <FormLabel>Username</FormLabel>
          <FormItem>
            <Input type="text" data-testid="username-input" />
          </FormItem>
        </FormField>
        <FormField name="email">
          <FormLabel>Email</FormLabel>
          <FormItem>
            <Input type="email" data-testid="email-input" />
          </FormItem>
        </FormField>
      </Form>
    );

    const usernameLabel = screen.getByText("Username");
    const usernameInput = screen.getByTestId("username-input");
    const emailLabel = screen.getByText("Email");
    const emailInput = screen.getByTestId("email-input");

    expect(usernameLabel).toHaveAttribute("for", "username");
    expect(usernameInput).toHaveAttribute("name", "username");
    expect(emailLabel).toHaveAttribute("for", "email");
    expect(emailInput).toHaveAttribute("name", "email");
  });

  it("handles nested structures correctly", () => {
    render(
      <FormField name="nested-field">
        <div>
          <FormLabel data-testid="nested-label">Nested Label</FormLabel>
          <FormItem>
            <Input data-testid="nested-input" />
          </FormItem>
        </div>
      </FormField>
    );

    const label = screen.getByTestId("nested-label");
    const input = screen.getByTestId("nested-input");

    expect(label).toHaveAttribute("for", "nested-field");
    expect(input).toHaveAttribute("name", "nested-field");
  });

  it("only accepts Input, TextArea, MarkdownEditor, or Select as children", () => {
    expect(() => {
      render(
        <FormField name="test">
          <FormItem>
            <Input data-testid="input" />
          </FormItem>
        </FormField>
      );
    }).not.toThrow();

    expect(() => {
      render(
        <FormField name="test">
          <FormItem>
            <TextArea data-testid="textarea" />
          </FormItem>
        </FormField>
      );
    }).not.toThrow();

    expect(() => {
      render(
        <FormField name="test">
          <FormItem>
            <Select
              data-testid="select"
              options={[
                { value: "option1", label: "Option 1" },
                { value: "option2", label: "Option 2" },
              ]}
            />
          </FormItem>
        </FormField>
      );
    }).not.toThrow();

    expect(() => {
      render(
        <FormField name="test">
          <FormItem>
            <div data-testid="invalid">Invalid</div>
          </FormItem>
        </FormField>
      );
    }).toThrow(
      "FormItem only accepts Input, TextArea, MarkdownEditor, or Select as children"
    );
  });
});
