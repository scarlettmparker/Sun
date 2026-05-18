/**
 * @fileoverview Tests for ContextMenu component.
 * Tests the open/close behavior, item activation, and asChild support.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import {
  Button,
  ContextMenu,
  ContextMenuContent,
  ContextMenuGroup,
  ContextMenuItem,
  ContextMenuSub,
  ContextMenuSubContent,
  ContextMenuSubTrigger,
  ContextMenuTrigger,
} from "@sun/components";

describe("ContextMenu", () => {
  it("opens and closes the menu when trigger is clicked", () => {
    render(
      <ContextMenu>
        <ContextMenuTrigger>Open</ContextMenuTrigger>
        <ContextMenuContent>
          <ContextMenuItem>Action</ContextMenuItem>
        </ContextMenuContent>
      </ContextMenu>,
    );

    const trigger = screen.getByRole("button", { name: /open/i });
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
      <ContextMenu>
        <ContextMenuTrigger>Open</ContextMenuTrigger>
        <ContextMenuContent>
          <ContextMenuItem onSelect={onSelect}>Action</ContextMenuItem>
        </ContextMenuContent>
      </ContextMenu>,
    );

    fireEvent.click(screen.getByRole("button", { name: /open/i }));
    fireEvent.click(screen.getByRole("menuitem", { name: /action/i }));

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("closes when clicking outside the open menu", () => {
    render(
      <div>
        <ContextMenu>
          <ContextMenuTrigger>Open</ContextMenuTrigger>
          <ContextMenuContent>
            <ContextMenuItem>Action</ContextMenuItem>
          </ContextMenuContent>
        </ContextMenu>
        <button>Outside</button>
      </div>,
    );

    fireEvent.click(screen.getByRole("button", { name: /open/i }));
    expect(screen.getByRole("menu")).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole("button", { name: /outside/i }));
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("activates a menu item with the Enter key", () => {
    const onSelect = jest.fn();

    render(
      <ContextMenu>
        <ContextMenuTrigger>Open</ContextMenuTrigger>
        <ContextMenuContent>
          <ContextMenuItem onSelect={onSelect}>Action</ContextMenuItem>
        </ContextMenuContent>
      </ContextMenu>,
    );

    fireEvent.click(screen.getByRole("button", { name: /open/i }));
    const menuItem = screen.getByRole("menuitem", { name: /action/i });
    fireEvent.keyDown(menuItem, { key: "Enter", code: "Enter" });

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("supports using a Sun Button as a child with asChild", () => {
    render(
      <ContextMenu>
        <ContextMenuTrigger>Open</ContextMenuTrigger>
        <ContextMenuContent>
          <ContextMenuItem asChild>
            <Button>Nested Action</Button>
          </ContextMenuItem>
        </ContextMenuContent>
      </ContextMenu>,
    );

    fireEvent.click(screen.getByRole("button", { name: /open/i }));
    const nestedButton = screen.getByRole("menuitem", {
      name: /nested action/i,
    });
    expect(nestedButton).toHaveAttribute("role", "menuitem");
  });

  it("renders ContextMenuGroup", () => {
    render(
      <ContextMenu>
        <ContextMenuTrigger>Open</ContextMenuTrigger>
        <ContextMenuContent>
          <ContextMenuGroup>
            <ContextMenuItem>Grouped</ContextMenuItem>
          </ContextMenuGroup>
        </ContextMenuContent>
      </ContextMenu>,
    );

    fireEvent.click(screen.getByRole("button", { name: /open/i }));
    expect(
      screen.getByRole("menuitem", { name: /grouped/i }),
    ).toBeInTheDocument();
  });

  it("opens and closes sub-menu", () => {
    render(
      <ContextMenu>
        <ContextMenuTrigger>Open</ContextMenuTrigger>
        <ContextMenuContent>
          <ContextMenuSub>
            <ContextMenuSubTrigger>Sub</ContextMenuSubTrigger>
            <ContextMenuSubContent>
              <ContextMenuItem>SubItem</ContextMenuItem>
            </ContextMenuSubContent>
          </ContextMenuSub>
        </ContextMenuContent>
      </ContextMenu>,
    );

    fireEvent.click(screen.getByRole("button", { name: /open/i }));
    const subTrigger = screen.getByRole("button", { name: /sub/i });
    fireEvent.click(subTrigger);
    expect(
      screen.getByRole("menuitem", { name: /subitem/i }),
    ).toBeInTheDocument();
    fireEvent.click(subTrigger);
    expect(
      screen.queryByRole("menuitem", { name: /subitem/i }),
    ).not.toBeInTheDocument();
  });
});
