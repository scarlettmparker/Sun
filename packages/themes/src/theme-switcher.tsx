import type { CSSProperties } from "react";
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@sun/components";
import { applyTheme } from "./apply-theme";
import type { ThemeValues } from "./types";
import "./theme-switcher.css";

/**
 * A selectable theme in the switcher.
 */
export type ThemeOption = {
  name: string;
  values: ThemeValues;
};

export type ThemeSwitcherProps = {
  /**
   * Themes a user can pick from.
   */
  themes: ThemeOption[];
};

/**
 * A primary-coloured circle that opens a dropdown of themes.
 *
 * @param props the switcher props
 */
export const ThemeSwitcher = ({ themes }: ThemeSwitcherProps) => {
  const select = (option: ThemeOption) => {
    applyTheme(option.values);
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="default"
          className="theme_switcher_trigger"
          aria-label="Switch theme"
          title="Switch theme"
        />
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        {themes.map((option) => (
          <DropdownMenuItem
            key={option.name}
            variant="secondary"
            onSelect={() => select(option)}
          >
            <span
              className="theme_switcher_swatch"
              style={
                { "--theme-swatch": option.values.primary } as CSSProperties
              }
            />
            {option.name}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
