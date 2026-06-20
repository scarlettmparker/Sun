import {
  detectFrontendMode,
} from "@sun/shared";
import type { FrontendMode } from "@sun/shared";

type UICheckProps = {
  /**
   * One or more frontend modes in which the children should render.
   */
  frontends: FrontendMode | FrontendMode[];
} & React.PropsWithChildren;

/**
 * Conditionally renders children based on the detected frontend mode.
 */
const UICheck = (props: UICheckProps) => {
  const { frontends, children } = props;
  const mode = detectFrontendMode();

  const allowed = Array.isArray(frontends) ? frontends : [frontends];

  if (!allowed.includes(mode)) {
    return null;
  }

  return children;
};

export default UICheck;