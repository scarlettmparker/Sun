import Input from "../input";
import Button from "../button";
import { Search, X } from "lucide-react";
import { cn } from "~/utils/cn";
import "./search-bar.module.css";

type SearchBarProps = Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  "onChange" | "value" | "type"
> & {
  /**
   * Current search value.
   */
  value: string;
  /**
   * Called on every keystroke.
   */
  onChange: (value: string) => void;
  /**
   * Called on Enter or blur (and on clear).
   */
  onSearch: (value: string) => void;
};

/**
 * Search input with a search icon, clear button, and Enter/blur callback.
 */
const SearchBar = ({
  value,
  onChange,
  onSearch,
  className,
  ...rest
}: SearchBarProps) => {
  return (
    <div className={cn("search_bar", className)}>
      <Search className="search_bar_icon" width={16} height={16} />
      <Input
        type="text"
        value={value}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange(e.target.value)}
        onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
          if (e.key === "Enter") onSearch(value);
        }}
        onBlur={() => onSearch(value)}
        className="search_bar_input"
        {...rest}
      />
      {value && (
        <Button
          variant="secondary"
          className="search_bar_clear"
          title="Clear search"
          aria-label="Clear search"
          onClick={() => {
            onChange("");
            onSearch("");
          }}
        >
          <X width={16} height={16} />
        </Button>
      )}
    </div>
  );
};

export default SearchBar;
