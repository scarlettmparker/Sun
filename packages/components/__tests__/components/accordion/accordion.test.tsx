/**
 * @fileoverview Tests for Accordion component.
 * Tests toggling open state and ARIA wiring.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import { Accordion, AccordionTrigger, AccordionContent } from "@sun/components";

jest.mock("~/components/accordion/accordion.module.css", () => ({
  accordion: "accordion",
  trigger: "trigger",
  trigger_label: "trigger_label",
  chevron: "chevron",
  chevron_open: "chevron_open",
  content: "content",
}));

describe("Accordion", () => {
  it("hides content until the trigger is clicked", () => {
    render(
      <Accordion>
        <AccordionTrigger>3 replies</AccordionTrigger>
        <AccordionContent>body</AccordionContent>
      </Accordion>,
    );

    const trigger = screen.getByRole("button", { name: "3 replies" });
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByText("body")).not.toBeInTheDocument();

    fireEvent.click(trigger);
    expect(trigger).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("body")).toBeInTheDocument();
  });

  it("starts open when defaultOpen is set", () => {
    render(
      <Accordion defaultOpen>
        <AccordionTrigger>replies</AccordionTrigger>
        <AccordionContent>body</AccordionContent>
      </Accordion>,
    );

    expect(screen.getByRole("button")).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("body")).toBeInTheDocument();
  });

  it("toggles closed on a second click", () => {
    render(
      <Accordion>
        <AccordionTrigger>replies</AccordionTrigger>
        <AccordionContent>body</AccordionContent>
      </Accordion>,
    );

    const trigger = screen.getByRole("button");
    fireEvent.click(trigger);
    fireEvent.click(trigger);

    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByText("body")).not.toBeInTheDocument();
  });
});
