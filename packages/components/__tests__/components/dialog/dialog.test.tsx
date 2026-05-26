/**
 * @fileoverview Tests for Dialog component.
 * Tests the Dialog component and its sub-components for rendering, styling, portal behavior, and attribute passing.
 */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  Dialog,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogBody,
  DialogFooter,
  DialogClose,
} from "@sun/components";

describe("Dialog", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<Dialog open={true}>Dialog content</Dialog>);
    const dialog = screen.getByRole("dialog");
    expect(dialog.tagName).toBe("ARTICLE");
    expect(dialog).toHaveClass("dialog");
  });

  it("passes through attributes correctly", () => {
    render(
      <Dialog open={true} data-testid="test-dialog" aria-label="Test dialog">
        Content
      </Dialog>,
    );
    const dialog = screen.getByTestId("test-dialog");
    expect(dialog).toHaveAttribute("data-testid", "test-dialog");
    expect(dialog).toHaveAttribute("aria-label", "Test dialog");
  });

  it("merges custom className with default classes", () => {
    render(
      <Dialog open={true} className="custom-class">
        Content
      </Dialog>,
    );
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveClass("dialog", "custom-class");
  });

  it("renders children correctly", () => {
    render(
      <Dialog open={true}>
        <span>Child element</span>
      </Dialog>,
    );
    expect(screen.getByText("Child element")).toBeInTheDocument();
  });

  it("does not render when open is false", () => {
    render(<Dialog open={false}>Hidden content</Dialog>);
    expect(screen.queryByText("Hidden content")).not.toBeInTheDocument();
  });

  it("renders as modal by default, applying overlay and pointer event classes", () => {
    render(<Dialog open={true}>Modal content</Dialog>);
    const dialog = screen.getByRole("dialog");

    expect(dialog).toHaveAttribute("aria-modal", "true");

    const wrapper = dialog.parentElement;
    expect(wrapper).toHaveClass("dialog_wrapper", "dialog_modal_active");

    const overlay = wrapper?.querySelector(".dialog_overlay");
    expect(overlay).toBeInTheDocument();
    expect(overlay).toHaveAttribute("aria-hidden", "true");
  });

  it("does not render overlay when modal is false", () => {
    render(
      <Dialog open={true} modal={false}>
        Non-modal content
      </Dialog>,
    );
    const dialog = screen.getByRole("dialog");

    expect(dialog).toHaveAttribute("aria-modal", "false");

    const wrapper = dialog.parentElement;
    expect(wrapper).toHaveClass("dialog_wrapper");
    expect(wrapper).not.toHaveClass("dialog_modal_active");

    const overlay = wrapper?.querySelector(".dialog_overlay");
    expect(overlay).not.toBeInTheDocument();
  });

  it("renders the built-in top-right close button and fires onOpenChange when clicked", async () => {
    const handleOpenChange = jest.fn();
    render(
      <Dialog open={true} onOpenChange={handleOpenChange}>
        Content
      </Dialog>,
    );

    const closeBtn = screen.getByRole("button", { name: /close/i });
    expect(closeBtn).toHaveClass("dialog_close_button");

    await userEvent.click(closeBtn);
    expect(handleOpenChange).toHaveBeenCalledWith(false);
  });
});

describe("DialogHeader", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<DialogHeader>Header content</DialogHeader>);
    const header = screen.getByText("Header content");
    expect(header.tagName).toBe("HEADER");
    expect(header).toHaveClass("dialog_header");
  });

  it("passes through attributes correctly", () => {
    render(<DialogHeader data-testid="test-header">Content</DialogHeader>);
    const header = screen.getByTestId("test-header");
    expect(header).toHaveAttribute("data-testid", "test-header");
  });

  it("merges custom className with default classes", () => {
    render(<DialogHeader className="custom-class">Content</DialogHeader>);
    const header = screen.getByText("Content");
    expect(header).toHaveClass("dialog_header", "custom-class");
  });
});

describe("DialogTitle", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<DialogTitle>Title content</DialogTitle>);
    const title = screen.getByText("Title content");
    expect(title.tagName).toBe("H3");
    expect(title).toHaveClass("dialog_title");
  });

  it("passes through attributes correctly", () => {
    render(<DialogTitle data-testid="test-title">Content</DialogTitle>);
    const title = screen.getByTestId("test-title");
    expect(title).toHaveAttribute("data-testid", "test-title");
  });

  it("merges custom className with default classes", () => {
    render(<DialogTitle className="custom-class">Content</DialogTitle>);
    const title = screen.getByText("Content");
    expect(title).toHaveClass("dialog_title", "custom-class");
  });
});

describe("DialogDescription", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<DialogDescription>Description content</DialogDescription>);
    const description = screen.getByText("Description content");
    expect(description.tagName).toBe("P");
    expect(description).toHaveClass("dialog_description");
  });

  it("passes through attributes correctly", () => {
    render(
      <DialogDescription data-testid="test-description">
        Content
      </DialogDescription>,
    );
    const description = screen.getByTestId("test-description");
    expect(description).toHaveAttribute("data-testid", "test-description");
  });

  it("merges custom className with default classes", () => {
    render(
      <DialogDescription className="custom-class">Content</DialogDescription>,
    );
    const description = screen.getByText("Content");
    expect(description).toHaveClass("dialog_description", "custom-class");
  });
});

describe("DialogBody", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<DialogBody>Body content</DialogBody>);
    const body = screen.getByText("Body content");
    expect(body.tagName).toBe("DIV");
    expect(body).toHaveClass("dialog_body");
  });

  it("passes through attributes correctly", () => {
    render(<DialogBody data-testid="test-body">Content</DialogBody>);
    const body = screen.getByTestId("test-body");
    expect(body).toHaveAttribute("data-testid", "test-body");
  });

  it("merges custom className with default classes", () => {
    render(<DialogBody className="custom-class">Content</DialogBody>);
    const body = screen.getByText("Content");
    expect(body).toHaveClass("dialog_body", "custom-class");
  });
});

describe("DialogFooter", () => {
  it("renders with correct semantic element and applies correct classes", () => {
    render(<DialogFooter>Footer content</DialogFooter>);
    const footer = screen.getByText("Footer content");
    expect(footer.tagName).toBe("FOOTER");
    expect(footer).toHaveClass("dialog_footer");
  });

  it("passes through attributes correctly", () => {
    render(<DialogFooter data-testid="test-footer">Content</DialogFooter>);
    const footer = screen.getByTestId("test-footer");
    expect(footer).toHaveAttribute("data-testid", "test-footer");
  });

  it("merges custom className with default classes", () => {
    render(<DialogFooter className="custom-class">Content</DialogFooter>);
    const footer = screen.getByText("Content");
    expect(footer).toHaveClass("dialog_footer", "custom-class");
  });
});

describe("DialogClose", () => {
  it("renders standard button element and triggers change event on click", async () => {
    const handleOpenChange = jest.fn();
    render(
      <Dialog open={true} onOpenChange={handleOpenChange}>
        <DialogClose>Cancel</DialogClose>
      </Dialog>,
    );

    const closeBtn = screen.getByRole("button", { name: "Cancel" });
    expect(closeBtn).toHaveClass("dialog_close");

    await userEvent.click(closeBtn);
    expect(handleOpenChange).toHaveBeenCalledWith(false);
  });

  it("supports asChild property to mutate layout elements while executing context state logic", async () => {
    const handleOpenChange = jest.fn();
    render(
      <Dialog open={true} onOpenChange={handleOpenChange}>
        <DialogClose asChild>
          <button className="custom-trigger">Alternative Close Button</button>
        </DialogClose>
      </Dialog>,
    );

    const customBtn = screen.getByRole("button", {
      name: "Alternative Close Button",
    });
    expect(customBtn).toHaveClass("dialog_close", "custom-trigger");

    await userEvent.click(customBtn);
    expect(handleOpenChange).toHaveBeenCalledWith(false);
  });
});
