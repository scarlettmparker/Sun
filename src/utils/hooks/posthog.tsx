import { PostHogProvider as PHProvider } from "@posthog/react";
import posthog from "posthog-js";

type PostHogProviderProps = {
  /**
   * Check if on client.
   */
  client?: boolean;
} & React.PropsWithChildren;

/**
 * Creates PostHog provider and initialises posthog.
 */
const PostHogProvider = (props: PostHogProviderProps) => {
  const { children, client } = props;

  // Don't initialize if client-only prop is set but we're on server
  if (client && typeof window === "undefined") {
    return children;
  }

  let posthogKey: string;
  let posthogHost: string;

  // Get configuration based on environment
  if (typeof window !== "undefined") {
    // Client-side
    posthogKey = window.__posthog_key__ || "";
    posthogHost = window.__posthog_host__ || "";
  } else {
    // Server-side
    posthogKey = process.env.POSTHOG_API_KEY || "";
    posthogHost = process.env.POSTHOG_HOST || "";
  }

  if (!posthogKey || !posthogHost) {
    return children;
  }

  // Initialize posthog if not yet loaded
  if (!posthog.__loaded) {
    posthog.init(posthogKey, {
      api_host: posthogHost,
    });
  }

  return <PHProvider client={posthog}>{children}</PHProvider>;
};

export { PostHogProvider };
