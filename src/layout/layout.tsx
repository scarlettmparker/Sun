import { JSX } from "react";

type LayoutProps = {
  /**
   * Children in layout.
   */
  children: JSX.Element;
};

/**
 * We don't actually have a layout now but maybe we will want one.
 */
const Layout = (props: LayoutProps) => {
  const { children } = props;

  return <>{children}</>;
};

export default Layout;
