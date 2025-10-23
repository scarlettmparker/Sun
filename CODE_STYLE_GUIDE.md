# Code Style Guide

This guide outlines the coding standards and best practices for this project. It covers testing setup, mocking strategies, and component development patterns.

## Testing Setup

### Jest Configuration

The project uses Jest for testing with the following setup:

- **Setup file**: `testing/jest/setup.ts` imports libraries and mocks
- **Mock directory**: `testing/jest/mock/` contains custom mocks/matchers for external dependencies

### Example Mocks

#### Audio API Mock (`testing/jest/mock/audio-api-mock.ts`)

Mocks the Web Audio API for testing audio-related functionality:

```typescript
// Example usage in tests
import {
  mockAudioContext,
  mockGainNodes,
} from "testing/jest/mock/audio-api-mock";

// The mock provides:
// - mockAudioContext: Mock AudioContext with createGain, createBufferSource, etc.
// - mockGainNodes: Array of mock gain nodes with gain and connect methods
// - Global mocks for AudioContext and fetch
```

#### CSS Module Mock (`testing/jest/mock/css-module-mock.ts`)

Mocks CSS modules by returning the class name as the key:

```typescript
// Mock implementation
export default new Proxy(
  {},
  {
    get: (_, key) => key,
  }
);

// In tests, importing styles returns the class name directly
import styles from "./component.module.css";
console.log(styles.container); // 'container'
```

#### i18n Mock (`testing/jest/mock/i18n-mock.ts`)

Mocks react-i18next for internationalization:

```typescript
import { mockT, useTranslation } from "testing/jest/mock/i18n-mock";

// mockT: Jest mock function that returns the key
// useTranslation: Mock hook returning { t: mockT }
```

## Component Development

### Props Passing

Components extend primitive HTML elements using intersection types:

```typescript
type ComponentProps = {
  /**
   * This is a custom prop.
   */
  customProp: string;
} & React.HTMLAttributes<HTMLDivElement>;

const Component = (props: ComponentProps) => {
  const { customProp, ...rest } = props;
  return <div {...rest}>{customProp}</div>;
};
```

### Extending Primitives

Always spread remaining props to the underlying element:

```typescript
const Button = (props: ButtonProps) => {
  const { variant, children, ...rest } = props;
  return (
    <button
      {...rest}
      className={`${styles.button} ${styles[variant]}`}
    >
      {children}
    </button>
  );
};
```

### CSS Modules

Use CSS modules for component styling:

```css
/* component.module.css */
.container {
  display: flex;
  gap: var(--md);
}

.button {
  padding: var(--sm) var(--md);
}
```

Import and use in components:

```typescript
import styles from './component.module.css';

const Component = () => (
  <div className={styles.container}>
    <button className={styles.button}>Click me</button>
  </div>
);
```

### Docstrings

Use JSDoc comments for functions, types, and components:

```typescript
/**
 * Format time from seconds to M:SS
 *
 * @param seconds Time in seconds.
 */
export function formatTime(seconds: number): string {
  // implementation
}

/**
 * Component description.
 */
const Component = (props: ComponentProps) => {
  // implementation
};
```

### Hooks

Custom hooks should be well-documented and use proper TypeScript types:

```typescript
/**
 * Custom hook description.
 *
 * @param param Description of parameter.
 */
export function useCustomHook(param: string) {
  // implementation
  return { value, setter };
}
```

### Type Definitions

Define types in separate files under `types/` directory:

```typescript
// types/component.ts
export type ComponentType = {
  /**
   * Property description.
   */
  property: string;
};
```

### Utility Functions

Utility functions must live in a `utils/` directory colocated with the feature or component they support. Each file should have **a single named export** that represents the primary utility for that file. Supporting helper functions may be included, but must remain unexported.

```typescript
// utils/format-name.ts

/**
 * Formats a name into "Title Case".
 *
 * @param name The input name string.
 * @returns The formatted name string.
 */
export function formatName(name: string): string {
  return toTitleCase(name);
}

function toTitleCase(value: string): string {
  return; // implementation
}
```

### Component Structure

Organize components in directories with:

- `index.tsx` - Main component export
- `component.tsx` - Component implementation
- `component.module.css` - Styles
- `hooks/` - Custom hooks
- `utils/` - Utility functions
- `types/` - Type definitions

### Import Paths

Use tilde (`~`) for absolute imports from src:

```typescript
import Button from "~/components/button";
import { Stem } from "~/_components/stem-player/types/stem";
```

### Event Handlers

Name event handlers descriptively and strongly type:

```typescript
const handleSeek = (e: ChangeEvent<HTMLInputElement>) => {
  // implementation
};

const handleSeekerMouseMove = (e: React.MouseEvent<HTMLInputElement>) => {
  // implementation
};
```

### Accessibility

Include proper ARIA labels and titles:

```typescript
<Button
  aria-label={t("controls.aria.play")}
  title={t("controls.title.play")}
>
  Play
</Button>
```

### Internationalization

Use react-i18next for all user-facing text:

```typescript
import { useTranslation } from 'react-i18next';

const Component = () => {
  const { t } = useTranslation('namespace');

  return <div>{t('key')}</div>;
};
```

### Memoization

Use `memo` for components that re-render frequently:

```typescript
const Component = memo((props: ComponentProps) => {
  // implementation
});

Component.displayName = "Component";
```

### Constants

Define constants at the top of components or in separate files:

```typescript
const SKIP_OFFSET = 10;
const SEEK_BUFFER = 0.1;
```
