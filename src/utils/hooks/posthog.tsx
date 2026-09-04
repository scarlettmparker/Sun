import { PostHogProvider as PHProvider } from "@posthog/react";
import { useEffect, useState } from "react";

type PostHogProviderProps = {
  /**
   * Check if on client.
   */
  client?: boolean;
} & React.PropsWithChildren;

/**
 * Creates PostHog provider and initialises posthog after idle.
 */
const PostHogProvider = (props: PostHogProviderProps) => {
  const { children, client } = props;
  const [posthogClient, setPosthogClient] = useState<unknown | null>(null);

  useEffect(() => {
    const key = window.__posthog_key__ || "";
    const host = window.__posthog_host__ || "";
    if (!key || !host) return;
    const init = async () => {
      const { default: posthog } = await import("posthog-js");
      if (!posthog.__loaded) {
        posthog.init(key, { api_host: host });
      }
      setPosthogClient(posthog);
    };
    if (window.requestIdleCallback) {
      window.requestIdleCallback(init);
    } else {
      setTimeout(init, 1);
    }
  }, []);

  if (client && typeof window === "undefined") {
    return children;
  }

  if (!posthogClient) {
    return children;
  }

  return <PHProvider client={posthogClient as never}>{children}</PHProvider>;
};

export { PostHogProvider };
