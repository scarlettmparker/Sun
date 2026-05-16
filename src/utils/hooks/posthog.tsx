import {
  PostHogProvider as PHProvider,
  useFeatureFlagEnabled,
  usePostHog,
} from "@posthog/react";
import posthog from "posthog-js";

type PostHogProviderProps = React.PropsWithChildren;

/**
 * All allowed posthog events. We don't need to capture $pageview
 * here, it's just used as an example for the typing.
 */
type PostHogEvent = "$pageview";

/**
 * Creates PostHog provider and initialises posthog
 * Use only as a server component
 */
const PostHogProvider = (props: PostHogProviderProps) => {
  const { children } = props;
  const posthogKey = process.env.POSTHOG_API_KEY ?? "";
  const posthogHost = process.env.POSTHOG_HOST ?? "";

  // Create posthog if not yet loaded
  if (!posthog.__loaded) {
    posthog.init(posthogKey, {
      api_host: posthogHost,
    });
  }

  return <PHProvider client={posthog}>{children}</PHProvider>;
};

/**
 * Capture an event of a single type.
 *
 * @param event Event to capture.
 */
const captureEvent = (event: PostHogEvent) => {
  const posthog = usePostHog();
  posthog.capture(event);
};

/**
 * Use feature flag event hook. We likely won't need to
 * type flags.
 *
 * @param flag Flag to check.
 */
const useFeatureFlag = (flag: string) => {
  return useFeatureFlagEnabled(flag) ?? false;
};

export { PostHogProvider, captureEvent, useFeatureFlag };
