/**
 * @fileoverview Tests for Tooltip component.
 * Tests open/close timing, group instant-switch, asChild, positioning, and focus.
 */

import { act, fireEvent, render, screen } from "@testing-library/react";
import {
  Tooltip,
  TooltipContent,
  TooltipGroup,
  TooltipTrigger,
} from "@sun/components";

const OPEN_DELAY = 400;
const CLOSE_DELAY = 650;

beforeEach(() => {
  jest.useFakeTimers();
});

afterEach(() => {
  jest.useRealTimers();
});

describe("Tooltip", () => {
  it("does not show content before the open delay", () => {
    render(
      <Tooltip>
        <TooltipTrigger>
          <button>Hover me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    fireEvent.mouseEnter(screen.getByText(/hover me/i));
    expect(screen.queryByText("Tooltip text")).not.toBeInTheDocument();
  });

  it("shows content after the open delay", () => {
    render(
      <Tooltip>
        <TooltipTrigger>
          <button>Hover me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    fireEvent.mouseEnter(screen.getByText(/hover me/i));
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY);
    });
    expect(screen.getByText("Tooltip text")).toBeInTheDocument();
  });

  it("hides content after the close delay on mouse leave", () => {
    render(
      <Tooltip>
        <TooltipTrigger>
          <button>Hover me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    const trigger = screen.getByText(/hover me/i);
    fireEvent.mouseEnter(trigger);
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY);
    });
    expect(screen.getByText("Tooltip text")).toBeInTheDocument();

    fireEvent.mouseLeave(trigger);
    act(() => {
      jest.advanceTimersByTime(CLOSE_DELAY);
    });
    expect(screen.queryByText("Tooltip text")).not.toBeInTheDocument();
  });

  it("cancels the close if the trigger is re-entered before the delay", () => {
    render(
      <Tooltip>
        <TooltipTrigger>
          <button>Hover me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    const trigger = screen.getByText(/hover me/i);
    fireEvent.mouseEnter(trigger);
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY);
    });

    fireEvent.mouseLeave(trigger);
    act(() => {
      jest.advanceTimersByTime(CLOSE_DELAY - 100);
    });
    fireEvent.mouseEnter(trigger);
    act(() => {
      jest.advanceTimersByTime(CLOSE_DELAY);
    });
    expect(screen.getByText("Tooltip text")).toBeInTheDocument();
  });

  it("opens on focus", () => {
    render(
      <Tooltip>
        <TooltipTrigger>
          <button>Focus me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    fireEvent.focus(screen.getByText(/focus me/i));
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY);
    });
    expect(screen.getByText("Tooltip text")).toBeInTheDocument();
  });

  it("closes on blur after the close delay", () => {
    render(
      <Tooltip>
        <TooltipTrigger>
          <button>Focus me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    const trigger = screen.getByText(/focus me/i);
    fireEvent.focus(trigger);
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY);
    });

    fireEvent.blur(trigger);
    act(() => {
      jest.advanceTimersByTime(CLOSE_DELAY);
    });
    expect(screen.queryByText("Tooltip text")).not.toBeInTheDocument();
  });

  it("renders the trigger as a wrapper span by default", () => {
    render(
      <Tooltip>
        <TooltipTrigger>
          <button>Hover me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    const wrapper = screen.getByText(/hover me/i).parentElement;
    expect(wrapper?.tagName).toBe("SPAN");
  });

  it("renders the child directly when asChild is true", () => {
    render(
      <Tooltip>
        <TooltipTrigger asChild>
          <button>Hover me</button>
        </TooltipTrigger>
        <TooltipContent>Tooltip text</TooltipContent>
      </Tooltip>,
    );

    const button = screen.getByText(/hover me/i);
    expect(button.parentElement?.tagName).not.toBe("SPAN");
  });
});

describe("TooltipGroup", () => {
  it("opens the second tooltip instantly when the first is already open", () => {
    render(
      <TooltipGroup>
        <Tooltip>
          <TooltipTrigger>
            <button>First</button>
          </TooltipTrigger>
          <TooltipContent>First tooltip</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger>
            <button>Second</button>
          </TooltipTrigger>
          <TooltipContent>Second tooltip</TooltipContent>
        </Tooltip>
      </TooltipGroup>,
    );

    fireEvent.mouseEnter(screen.getByText(/first/i));
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY);
    });
    expect(screen.getByText("First tooltip")).toBeInTheDocument();

    fireEvent.mouseEnter(screen.getByText(/second/i));
    act(() => {
      jest.advanceTimersByTime(0);
    });
    expect(screen.getByText("Second tooltip")).toBeInTheDocument();
  });

  it("restores the normal delay after all tooltips close", () => {
    render(
      <TooltipGroup>
        <Tooltip>
          <TooltipTrigger>
            <button>First</button>
          </TooltipTrigger>
          <TooltipContent>First tooltip</TooltipContent>
        </Tooltip>
        <Tooltip>
          <TooltipTrigger>
            <button>Second</button>
          </TooltipTrigger>
          <TooltipContent>Second tooltip</TooltipContent>
        </Tooltip>
      </TooltipGroup>,
    );

    const first = screen.getByText(/first/i);
    fireEvent.mouseEnter(first);
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY);
    });

    fireEvent.mouseLeave(first);
    act(() => {
      jest.advanceTimersByTime(CLOSE_DELAY);
    });

    fireEvent.mouseEnter(screen.getByText(/second/i));
    act(() => {
      jest.advanceTimersByTime(OPEN_DELAY - 100);
    });
    expect(screen.queryByText("Second tooltip")).not.toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(100);
    });
    expect(screen.getByText("Second tooltip")).toBeInTheDocument();
  });
});
