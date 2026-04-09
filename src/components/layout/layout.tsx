import TopNavBar from "./menu/top-nav-bar";
import styles from "./layout.module.css";
import { getBackgroundHex } from "~/utils/background-colour";
import { useEffect, useState } from "react";

type LayoutProps = React.PropsWithChildren;

/**
 * We don't actually have a layout now but maybe we will want one.
 */
const Layout = (props: LayoutProps) => {
  const { children } = props;
  const [backgroundColour, setBackgroundColour] = useState(getBackgroundHex());

  const updateBackgroundColour = () => {
    setBackgroundColour(getBackgroundHex());
  };

  useEffect(() => {
    const interval = setInterval(() => updateBackgroundColour(), 5000); // 5 second
    return () => clearInterval(interval);
  }),
    [];

  return (
    <main style={{ backgroundColor: backgroundColour }} className={styles.main}>
      <TopNavBar />
      {children}
    </main>
  );
};

export default Layout;
