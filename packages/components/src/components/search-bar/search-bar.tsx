import { useRef } from "react";
import Input from "../input";
import Button from "../button";
import { Search, X } from "lucide-react";
import { cn } from "~/utils/cn";
import styles from "./search-bar.module.css";

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
   * Called on Enter, or on blur when the value has changed since the last search.
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
  const lastSearched = useRef(value);

  const fire = () => {
    if (value !== lastSearched.current) {
      lastSearched.current = value;
      onSearch(value);
    }
  };

  return (
    <div className={cn(styles.search_bar, className)}>
      <Search className={styles.search_bar_icon} width={16} height={16} />
      <Input
        type="text"
        value={value}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
          onChange(e.target.value)
        }
        onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
          if (e.key === "Enter") fire();
        }}
        onBlur={fire}
        className={styles.search_bar_input}
        {...rest}
      />
      {value && (
        <Button
          variant="secondary"
          className={styles.search_bar_clear}
          title="Clear search"
          aria-label="Clear search"
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => {
            onChange("");
            lastSearched.current = "";
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
