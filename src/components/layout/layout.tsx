import TopNavBar from "./menu/top-nav-bar";

type LayoutProps = React.PropsWithChildren;

/**
 * We don't actually have a layout now but maybe we will want one.
 */
const Layout = (props: LayoutProps) => {
  const { children } = props;

  return (
    <>
      <TopNavBar />
      <main>{children}</main>
    </>
  );
};

export default Layout;
