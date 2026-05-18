/**
 * @fileoverview Tests for Breadcrumb component and dynamic context control.
 *
 * Example dynamic usage:
 * const { addBreadcrumb, popBreadcrumb, setBreadcrumbs, deleteAllBreadcrumbs } = useContext(BreadcrumbContext);
 * // on link click: addBreadcrumb({label: 'New', href: '/new'});
 * // setBreadcrumbs([{label:'Home',href:'/'}, {label:'Page'}]);
 * // popBreadcrumbs(2); deleteBreadcrumb(0); deleteAllBreadcrumbs();
 */

import { render, screen, act } from "@testing-library/react";
import { Breadcrumb, BreadcrumbItem, BreadcrumbContext } from "@sun/components";
import React, { useContext } from "react";

describe("Breadcrumb", () => {
  it("renders empty initially", () => {
    render(<Breadcrumb />);
    const nav = screen.getByRole("navigation", { name: /breadcrumb/i });
    expect(nav).toBeInTheDocument();
    expect(screen.queryByRole("listitem")).not.toBeInTheDocument();
  });

  it("supports dynamic add via context", () => {
    const Controller = () => {
      const { addBreadcrumb } = useContext(BreadcrumbContext);
      return (
        <button onClick={() => addBreadcrumb({ label: "Home", href: "/" })}>
          Add
        </button>
      );
    };
    render(
      <Breadcrumb>
        <Controller />
      </Breadcrumb>,
    );
    act(() => {
      screen.getByText("Add").click();
    });
    expect(screen.getByText("Home")).toBeInTheDocument();
  });

  it("supports setBreadcrumbs, pop, delete, deleteAll", () => {
    const Controller = () => {
      const ctx = useContext(BreadcrumbContext);
      return (
        <>
          <button
            onClick={() =>
              ctx.setBreadcrumbs([
                { label: "A" },
                { label: "B" },
                { label: "C" },
              ])
            }
          >
            Set
          </button>
          <button onClick={() => ctx.popBreadcrumb()}>Pop</button>
          <button onClick={() => ctx.deleteBreadcrumb(0)}>Del0</button>
          <button onClick={() => ctx.deleteAllBreadcrumbs()}>Clear</button>
        </>
      );
    };
    render(
      <Breadcrumb>
        <Controller />
      </Breadcrumb>,
    );
    act(() => screen.getByText("Set").click());
    expect(screen.getAllByRole("listitem")).toHaveLength(3);
    act(() => screen.getByText("Pop").click());
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    act(() => screen.getByText("Del0").click());
    expect(screen.getAllByRole("listitem")).toHaveLength(1);
    act(() => screen.getByText("Clear").click());
    expect(screen.queryAllByRole("listitem")).toHaveLength(0);
  });

  it("sets current and applies active class/aria", () => {
    const Controller = () => {
      const { setBreadcrumbs, setCurrent } = useContext(BreadcrumbContext);
      return (
        <>
          <button
            onClick={() => {
              setBreadcrumbs([{ label: "X", href: "/x" }]);
              setCurrent("/x");
            }}
          >
            Activate
          </button>
        </>
      );
    };
    render(
      <Breadcrumb>
        <Controller />
      </Breadcrumb>,
    );
    act(() => screen.getByText("Activate").click());
    const li = screen.getByText("X").closest("li");
    expect(li).toHaveAttribute("aria-current", "page");
    expect(li).toHaveClass("active");
  });

  it("renders BreadcrumbItem standalone with accessibility", () => {
    render(<BreadcrumbItem href="/test">Test Item</BreadcrumbItem>);
    const item = screen.getByText("Test Item").closest("li");
    expect(item).toBeInTheDocument();
    expect(item?.querySelector("a")).toHaveAttribute("href", "/test");
  });
});
