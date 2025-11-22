import styles from "./sidebar.module.css";

type SidebarProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Sidebar.
 */
const Sidebar = (props: SidebarProps) => {
  const { children, className, ...rest } = props;

  return (
    <div {...rest} className={`${styles.sidebar} ${className ?? ""}`}>
      {children}
    </div>
  );
};

export default Sidebar;
