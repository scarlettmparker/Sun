import "@testing-library/jest-dom/vitest";
import { options } from "preact";
import "./mock";

options.debounceRendering = (fn: () => void) => fn();

globalThis.cancelAnimationFrame ??= () => {};
