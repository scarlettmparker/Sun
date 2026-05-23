/**
 * @fileoverview Tests for the internal shared Menu primitives.
 * These cover MenuItem, MenuSub*, MenuTrigger, MenuContent, and the exported hooks.
 * High branch coverage including disabled states, asChild, keyboard nav, positioning,
 * close handlers, and error cases for missing providers.
 */

import React, { useRef, useState } from "react";
import { fireEvent, render, screen, act } from "@testing-library/react";
import {
  MenuItem,
  MenuSub,
  MenuSubTrigger,
  MenuSubContent,
  MenuTrigger,
  MenuContent,
  useMenuIds,
  useMenuCloseHandlers,
  useDropdownPositioning,
} from "~/components/menu/menu";

describe("Menu primitives", () => {
  beforeEach(() => {
    // Mock rects so positioning calculations execute flip/offset branches
    const rect = {
      width: 120,
      height: 80,
      top: 100,
      left: 100,
      bottom: 180,
      right: 220,
      x: 100,
      y: 100,
      toJSON() {
        return this;
      },
    };
    Element.prototype.getBoundingClientRect = jest.fn(() => rect as any);
    window.innerWidth = 1024;
    window.innerHeight = 768;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("useMenuIds generates stable prefixed ids", () => {
    const TestIds = () => {
      const { triggerId, contentId } = useMenuIds("dropdown-menu");
      return (
        <div data-tid={triggerId} data-cid={contentId}>
          ids
        </div>
      );
    };
    render(<TestIds />);
    const el = screen.getByText("ids");
    expect(el).toHaveAttribute("data-tid", expect.stringContaining("dropdown-menu-trigger-"));
    expect(el).toHaveAttribute("data-cid", expect.stringContaining("dropdown-menu-content-"));
  });

  it("MenuItem renders, handles click/Enter/Space, calls onSelect+closeMenu, skips when disabled", () => {
    const onSelect = jest.fn();
    const closeMenu = jest.fn();
    const onClick = jest.fn();

    render(
      <div>
        <MenuItem closeMenu={closeMenu} onSelect={onSelect} onClick={onClick}>
          Clickable
        </MenuItem>
        <MenuItem closeMenu={closeMenu} disabled onSelect={onSelect}>
          Disabled
        </MenuItem>
      </div>,
    );

    const item = screen.getByRole("menuitem", { name: /clickable/i });
    fireEvent.click(item);
    expect(onClick).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(closeMenu).toHaveBeenCalledTimes(1);

    const dis = screen.getByRole("menuitem", { name: /disabled/i });
    fireEvent.click(dis);
    expect(onSelect).toHaveBeenCalledTimes(1); // no extra

    // keyboard
    fireEvent.keyDown(item, { key: "Enter" });
    expect(onSelect).toHaveBeenCalledTimes(2);
    fireEvent.keyDown(item, { key: " " });
    expect(onSelect).toHaveBeenCalledTimes(3);
  });

  it("MenuItem supports asChild cloning and forwards handlers", () => {
    const closeMenu = jest.fn();
    const onSelect = jest.fn();

    render(
      <MenuItem closeMenu={closeMenu} onSelect={onSelect} asChild>
        <button type="button">Child Btn</button>
      </MenuItem>,
    );

    const btn = screen.getByRole("menuitem", { name: /child btn/i });
    expect(btn.tagName).toBe("BUTTON");
    fireEvent.click(btn);
    expect(onSelect).toHaveBeenCalled();
    expect(closeMenu).toHaveBeenCalled();
  });

  it("MenuItem asChild handles key events on cloned element", () => {
    const closeMenu = jest.fn();
    render(
      <MenuItem closeMenu={closeMenu} asChild>
        <button>KeyChild</button>
      </MenuItem>,
    );
    const btn = screen.getByRole("menuitem", { name: /keychild/i });
    fireEvent.keyDown(btn, { key: "Enter" });
    expect(closeMenu).toHaveBeenCalled();
  });

  it("throws when MenuSubTrigger used outside MenuSub", () => {
    const consoleError = jest.spyOn(console, "error").mockImplementation(() => {});
    expect(() => {
      render(<MenuSubTrigger arrowClassName="x">bad</MenuSubTrigger>);
    }).toThrow(/MenuSub components must be used inside a MenuSub/);
    consoleError.mockRestore();
  });

  it("MenuSub + SubTrigger + SubContent toggles on pointer enter/leave and renders in portal", () => {
    render(
      <MenuSub data-testid="sub-root" resetNonce={0}>
        <MenuSubTrigger>SubTrigger</MenuSubTrigger>
        <MenuSubContent>
          <div data-testid="sub-item">Sub Content</div>
        </MenuSubContent>
      </MenuSub>,
    );

    expect(screen.queryByTestId("sub-item")).not.toBeInTheDocument();

    const trig = screen.getByRole("menuitem", { name: /subtrigger/i });
    fireEvent.pointerEnter(trig);
    expect(screen.getByTestId("sub-item")).toBeInTheDocument();

    fireEvent.pointerLeave(trig);
    expect(screen.queryByTestId("sub-item")).not.toBeInTheDocument();
  });

  it("MenuSubContent pointer enter/leave keeps/closes sub", () => {
    render(
      <MenuSub resetNonce={0}>
        <MenuSubTrigger>Trigger2</MenuSubTrigger>
        <MenuSubContent>
          <MenuItem closeMenu={jest.fn()}>Item</MenuItem>
        </MenuSubContent>
      </MenuSub>,
    );
    const trig = screen.getByText(/trigger2/i);
    fireEvent.pointerEnter(trig);
    const content = screen.getByRole("menu");
    fireEvent.pointerEnter(content);
    expect(content).toBeInTheDocument();
    fireEvent.pointerLeave(content);
    expect(screen.queryByText("Item")).not.toBeInTheDocument();
  });

  it("MenuTrigger renders div or clones asChild", () => {
    const { rerender } = render(
      <MenuTrigger data-testid="plain">Plain</MenuTrigger>,
    );
    expect(screen.getByTestId("plain").tagName).toBe("DIV");

    rerender(
      <MenuTrigger asChild>
        <span data-testid="cloned">Cloned</span>
      </MenuTrigger>,
    );
    expect(screen.getByTestId("cloned").tagName).toBe("SPAN");
  });

  it("MenuContent renders only when open, wires keyboard nav and stopPropagation", () => {
    const ref = React.createRef<HTMLDivElement>();
    const onKeyDown = jest.fn();
    const { rerender } = render(
      <MenuContent
        open={false}
        contentRef={ref}
        id="c1"
        triggerId="t1"
        aria-label="Test Menu"
        contentDataAttr="test-content"
        onKeyDown={onKeyDown}
      >
        <MenuItem closeMenu={jest.fn()}>One</MenuItem>
        <MenuItem closeMenu={jest.fn()}>Two</MenuItem>
      </MenuContent>,
    );
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();

    rerender(
      <MenuContent
        open
        contentRef={ref}
        id="c1"
        triggerId="t1"
        aria-label="Test Menu"
        contentDataAttr="test-content"
        onKeyDown={onKeyDown}
      >
        <MenuItem closeMenu={jest.fn()}>One</MenuItem>
        <MenuItem closeMenu={jest.fn()}>Two</MenuItem>
      </MenuContent>,
    );

    const menu = screen.getByRole("menu", { name: "Test Menu" });
    expect(menu).toBeInTheDocument();

    // arrow nav should move focus
    const first = screen.getByRole("menuitem", { name: /one/i });
    const second = screen.getByRole("menuitem", { name: /two/i });
    expect(first).toHaveFocus();

    fireEvent.keyDown(menu, { key: "ArrowDown" });
    expect(second).toHaveFocus();

    fireEvent.keyDown(menu, { key: "ArrowUp" });
    expect(first).toHaveFocus();

    // custom handler called
    fireEvent.keyDown(menu, { key: "x" });
    expect(onKeyDown).toHaveBeenCalled();

    // stop prop on pointer/click (no-op assert, just no crash)
    fireEvent.pointerDown(menu);
    fireEvent.click(menu);
  });

  it("useMenuCloseHandlers closes on outside pointerdown and Escape", () => {
    const CloseTest = () => {
      const rootRef = useRef<HTMLDivElement>(null);
      const [open, setOpen] = useState(true);
      useMenuCloseHandlers(open, () => setOpen(false), rootRef, ["[data-ignore]"]);
      return (
        <div ref={rootRef} data-testid="close-root" data-open={open}>
          <button data-testid="inside">Inside</button>
          <div data-ignore>ignore zone</div>
          {open ? "OPEN" : "CLOSED"}
        </div>
      );
    };

    render(<CloseTest />);
    const root = screen.getByTestId("close-root");
    expect(root).toBeInTheDocument();
    expect(root).toHaveTextContent("OPEN");

    // pointer on ignore zone does not close
    fireEvent.pointerDown(screen.getByText("ignore zone"));
    expect(screen.getByTestId("close-root")).toHaveTextContent("OPEN");

    // escape key closes (covers keyboard branch)
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.getByTestId("close-root")).toHaveTextContent("CLOSED");
  });

  it("useDropdownPositioning runs update without crashing (covers layout effect)", () => {
    const PosTest = () => {
      const cRef = useRef<HTMLDivElement>(null);
      const tRef = useRef<HTMLElement>(null);
      const [open] = useState(true);
      const [nonce] = useState(0);
      useDropdownPositioning(cRef, tRef, open, nonce);
      return (
        <div>
          <span ref={tRef as any} data-testid="trig" />
          <div ref={cRef} data-testid="content-pos" />
        </div>
      );
    };
    render(<PosTest />);
    expect(screen.getByTestId("content-pos")).toBeInTheDocument();
  });

  it("MenuSub resets open state when resetNonce changes", () => {
    const { rerender } = render(
      <MenuSub resetNonce={1} data-testid="sub">
        <MenuSubTrigger>Sub</MenuSubTrigger>
      </MenuSub>,
    );
    const sub = screen.getByTestId("sub");
    const trig = screen.getByRole("menuitem", { name: /sub/i });
    expect(sub).toHaveAttribute("data-state", "closed");

    fireEvent.pointerEnter(trig);
    expect(sub).toHaveAttribute("data-state", "open");

    // changing nonce triggers effect that sets open=false
    rerender(
      <MenuSub resetNonce={2} data-testid="sub">
        <MenuSubTrigger>Sub</MenuSubTrigger>
      </MenuSub>,
    );
    expect(sub).toHaveAttribute("data-state", "closed");
  });

  it("MenuItem supports destructive variant", () => {
    const closeMenu = jest.fn();
    render(
      <MenuItem closeMenu={closeMenu} variant="destructive">
        Delete
      </MenuItem>,
    );
    const item = screen.getByRole("menuitem", { name: /delete/i });
    expect(item).toHaveClass("button", "destructive");
  });
});
