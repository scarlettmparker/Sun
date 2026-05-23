/**
 * @fileoverview Tests for DropdownMenu component.
 * Tests the open/close behavior, item activation, asChild support, submenus, keyboard navigation.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@sun/components";

describe("DropdownMenu", () => {
  it("opens and closes the menu when trigger is clicked", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Action</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    const trigger = screen.getByText(/open/i);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();

    fireEvent.click(trigger);
    expect(screen.getByRole("menu")).toBeInTheDocument();
    expect(
      screen.getByRole("menuitem", { name: /action/i }),
    ).toBeInTheDocument();

    fireEvent.click(trigger);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("closes the menu after selecting an item", () => {
    const onSelect = jest.fn();

    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onSelect={onSelect}>Action</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    fireEvent.click(screen.getByRole("menuitem", { name: /action/i }));

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("closes when clicking outside the open menu", () => {
    render(
      <div>
        <DropdownMenu>
          <DropdownMenuTrigger>Open</DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuItem>Action</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
        <button>Outside</button>
      </div>,
    );

    fireEvent.click(screen.getByText(/open/i));
    expect(screen.getByRole("menu")).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole("button", { name: /outside/i }));
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("activates a menu item with the Enter key", () => {
    const onSelect = jest.fn();

    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onSelect={onSelect}>Action</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    const menuItem = screen.getByRole("menuitem", { name: /action/i });
    fireEvent.keyDown(menuItem, { key: "Enter", code: "Enter" });

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("supports using a Sun Button as a child with asChild", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem asChild>
            <Button>Nested Action</Button>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    const nestedButton = screen.getByRole("menuitem", {
      name: /nested action/i,
    });
    expect(nestedButton).toHaveAttribute("role", "menuitem");
  });

  it("renders DropdownMenuGroup", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuGroup>
            <DropdownMenuItem>Grouped</DropdownMenuItem>
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    expect(
      screen.getByRole("menuitem", { name: /grouped/i }),
    ).toBeInTheDocument();
  });

  it("opens and closes sub-menu using pointer events", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuSub>
            <DropdownMenuSubTrigger>Sub</DropdownMenuSubTrigger>
            <DropdownMenuSubContent>
              <DropdownMenuItem>SubItem</DropdownMenuItem>
            </DropdownMenuSubContent>
          </DropdownMenuSub>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    const subTrigger = screen.getByText(/sub/i);
    fireEvent.pointerEnter(subTrigger);
    expect(
      screen.getByRole("menuitem", { name: /subitem/i }),
    ).toBeInTheDocument();
    fireEvent.pointerLeave(subTrigger);
    expect(
      screen.queryByRole("menuitem", { name: /subitem/i }),
    ).not.toBeInTheDocument();
  });

  it("closes on Escape key", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Action</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    expect(screen.getByRole("menu")).toBeInTheDocument();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("supports asChild on DropdownMenuTrigger", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button>Open</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Action</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    const trigger = screen.getByRole("button", { name: /open/i });
    fireEvent.click(trigger);
    expect(screen.getByRole("menu")).toBeInTheDocument();
  });

  it("does not select disabled menu item", () => {
    const onSelect = jest.fn();
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem disabled onSelect={onSelect}>
            Disabled
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    const item = screen.getByRole("menuitem", { name: /disabled/i });
    fireEvent.click(item);
    expect(onSelect).not.toHaveBeenCalled();
  });

  it("supports arrow key navigation between items", () => {
    render(
      <DropdownMenu>
        <DropdownMenuTrigger>Open</DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>First</DropdownMenuItem>
          <DropdownMenuItem>Second</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );

    fireEvent.click(screen.getByText(/open/i));
    const first = screen.getByRole("menuitem", { name: /first/i });
    const second = screen.getByRole("menuitem", { name: /second/i });

    // Focus should start on first
    expect(first).toHaveFocus();

    fireEvent.keyDown(first, { key: "ArrowDown" });
    expect(second).toHaveFocus();

    fireEvent.keyDown(second, { key: "ArrowUp" });
    expect(first).toHaveFocus();
  });
});
