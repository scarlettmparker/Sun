/**
 * @fileoverview Tests for Select component.
 * Tests the Select component's rendering, props handling, and form integration.
 */

import { render, screen, fireEvent, within } from "@testing-library/react";
import {
  restoreConsoleError,
  suppressConsoleErrorsFromTests,
} from "testing/jest/mock";
import Select, { SelectOption } from "~/components/select";

describe("Select", () => {
  it("renders select element with correct attributes", () => {
    render(
      <Select data-testid="select">
        <SelectOption value="option1">Option 1</SelectOption>
        <SelectOption value="option2">Option 2</SelectOption>
      </Select>
    );
    const select = screen.getByTestId("hidden-select");
    expect(select.tagName).toBe("SELECT");
  });

  it("renders options correctly", () => {
    render(
      <Select data-testid="select">
        <SelectOption value="option1">Option 1</SelectOption>
        <SelectOption value="option2">Option 2</SelectOption>
        <SelectOption value="option3">Option 3</SelectOption>
      </Select>
    );

    const select = screen.getByTestId("hidden-select");
    const optionElements = select.querySelectorAll("option");

    expect(optionElements).toHaveLength(3);
    expect(optionElements[0]).toHaveAttribute("value", "option1");
    expect(optionElements[0]).toHaveTextContent("Option 1");
    expect(optionElements[1]).toHaveAttribute("value", "option2");
    expect(optionElements[1]).toHaveTextContent("Option 2");
    expect(optionElements[2]).toHaveAttribute("value", "option3");
    expect(optionElements[2]).toHaveTextContent("Option 3");
  });

  it("passes through select attributes correctly", () => {
    render(
      <Select
        data-testid="select"
        name="test-select"
        id="select-id"
        required
        disabled
        className="custom-select"
      >
        <SelectOption value="test">Test</SelectOption>
      </Select>
    );

    const select = screen.getByTestId("hidden-select");
    expect(select).toHaveAttribute("name", "test-select");
    expect(select).toHaveAttribute("id", "select-id");
    expect(select).toHaveAttribute("required");
    expect(select).toHaveAttribute("disabled");
    const container = screen.getByTestId("select");
    expect(container).toHaveClass("custom-select");
  });

  it("handles value changes correctly", () => {
    const handleChange = jest.fn();

    render(
      <Select data-testid="select" onChange={handleChange}>
        <SelectOption value="option1">Option 1</SelectOption>
        <SelectOption value="option2">Option 2</SelectOption>
      </Select>
    );

    const select = screen.getByTestId("hidden-select");
    fireEvent.change(select, { target: { value: "option2" } });

    expect(handleChange).toHaveBeenCalledTimes(1);
  });

  it("sets default value correctly", () => {
    render(
      <Select data-testid="select" defaultValue="option2">
        <SelectOption value="option1">Option 1</SelectOption>
        <SelectOption value="option2">Option 2</SelectOption>
      </Select>
    );

    const select = screen.getByTestId("hidden-select");
    expect(select).toHaveValue("option2");
  });

  it("handles no children", () => {
    render(<Select data-testid="select" />);

    const select = screen.getByTestId("hidden-select");
    const optionElements = select.querySelectorAll("option");
    expect(optionElements).toHaveLength(0);
  });

  it("applies correct styling classes", () => {
    render(
      <Select data-testid="select" className="additional-class">
        <SelectOption value="test">Test</SelectOption>
      </Select>
    );

    const container = screen.getByTestId("select");
    expect(container).toHaveClass("additional-class");
  });

  describe("Dropdown interaction", () => {
    it("opens dropdown when button is clicked", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();

      fireEvent.click(button);
      expect(screen.getByRole("listbox")).toBeInTheDocument();
    });

    it("closes dropdown when button is clicked again", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);
      expect(screen.getByRole("listbox")).toBeInTheDocument();

      fireEvent.click(button);
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    });

    it("selects option and closes dropdown when option is clicked", () => {
      const handleChange = jest.fn();
      render(
        <Select data-testid="select" onChange={handleChange}>
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);

      const listbox = screen.getByRole("listbox");
      const option = within(listbox).getByText("Option 2");
      fireEvent.click(option);

      expect(handleChange).toHaveBeenCalledWith(
        expect.objectContaining({
          target: { value: "option2" },
        })
      );
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
      expect(button).toHaveTextContent("Option 2");
    });

    it("updates selected value in hidden select when option is clicked", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);

      const listbox = screen.getByRole("listbox");
      const option = within(listbox).getByText("Option 2");
      fireEvent.click(option);

      const hiddenSelect = screen.getByTestId("hidden-select");
      expect(hiddenSelect).toHaveValue("option2");
    });
  });

  describe("Keyboard navigation", () => {
    it("opens dropdown on Enter key press", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.keyDown(button, { key: "Enter" });

      expect(screen.getByRole("listbox")).toBeInTheDocument();
    });

    it("opens dropdown on Space key press", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.keyDown(button, { key: " " });

      expect(screen.getByRole("listbox")).toBeInTheDocument();
    });

    it("closes dropdown on Escape key press", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);
      expect(screen.getByRole("listbox")).toBeInTheDocument();

      fireEvent.keyDown(button, { key: "Escape" });
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    });

    it("opens dropdown on ArrowDown key press when closed", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.keyDown(button, { key: "ArrowDown" });

      expect(screen.getByRole("listbox")).toBeInTheDocument();
    });

    it("selects option on Enter key press in dropdown", () => {
      const handleChange = jest.fn();
      render(
        <Select data-testid="select" onChange={handleChange}>
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);

      const listbox = screen.getByRole("listbox");
      const option = within(listbox).getByText("Option 2");
      fireEvent.keyDown(option, { key: "Enter" });

      expect(handleChange).toHaveBeenCalledWith(
        expect.objectContaining({
          target: { value: "option2" },
        })
      );
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    });
  });

  describe("Click outside", () => {
    it("closes dropdown when clicking outside", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);
      expect(screen.getByRole("listbox")).toBeInTheDocument();

      fireEvent.mouseDown(document.body);
      expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    });
  });

  describe("Accessibility", () => {
    it("has correct ARIA attributes on button", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      expect(button).toHaveAttribute("aria-expanded", "false");
      expect(button).toHaveAttribute("aria-haspopup", "listbox");
      expect(button).toHaveAttribute("aria-label", "Select option");
    });

    it("updates aria-expanded when dropdown opens", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      expect(button).toHaveAttribute("aria-expanded", "false");

      fireEvent.click(button);
      expect(button).toHaveAttribute("aria-expanded", "true");
    });

    it("has correct roles and attributes on options", () => {
      render(
        <Select data-testid="select" value="option1">
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);

      const listbox = screen.getByRole("listbox");
      const options = within(listbox).getAllByRole("option");
      expect(options).toHaveLength(2);

      expect(options[0]).toHaveAttribute("aria-selected", "true");
      expect(options[0]).toHaveAttribute("tabindex", "0");

      expect(options[1]).toHaveAttribute("aria-selected", "false");
      expect(options[1]).toHaveAttribute("tabindex", "0");
    });

    it("dropdown has correct role", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);

      const listbox = screen.getByRole("listbox");
      expect(listbox).toBeInTheDocument();
    });
  });

  describe("Edge cases", () => {
    it("displays default placeholder when no value selected", () => {
      render(
        <Select data-testid="select">
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      expect(button).toHaveTextContent("Select...");
    });

    it("handles controlled value prop", () => {
      const { rerender } = render(
        <Select data-testid="select" value="option1">
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      expect(button).toHaveTextContent("Option 1");

      rerender(
        <Select data-testid="select" value="option2">
          <SelectOption value="option1">Option 1</SelectOption>
          <SelectOption value="option2">Option 2</SelectOption>
        </Select>
      );

      expect(button).toHaveTextContent("Option 2");
    });

    it("handles empty options array", () => {
      render(<Select data-testid="select" />);

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);

      // Should not crash, dropdown should be empty
      expect(screen.getByRole("listbox")).toBeInTheDocument();
      expect(screen.queryAllByRole("option")).toHaveLength(0);
    });

    it("handles options with complex children", () => {
      // Since span is actually not a valid child of select option, and we don't want to bloat test output
      suppressConsoleErrorsFromTests();

      render(
        <Select data-testid="select">
          <SelectOption value="option1">
            <span>Option</span> 1
          </SelectOption>
        </Select>
      );

      const button = screen.getByRole("button", { name: /select option/i });
      fireEvent.click(button);

      const listbox = screen.getByRole("listbox");
      const option = within(listbox).getByRole("option");
      expect(option).toHaveTextContent("Option 1");

      restoreConsoleError();
    });

    it("passes disabled attribute to hidden select", () => {
      render(
        <Select data-testid="select" disabled>
          <SelectOption value="option1">Option 1</SelectOption>
        </Select>
      );

      const hiddenSelect = screen.getByTestId("hidden-select");
      expect(hiddenSelect).toBeDisabled();
    });
  });
});
