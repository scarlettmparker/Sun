import { cn } from "~/utils/cn";
import "./sidebar.module.css";

type SidebarProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Sidebar.
 */
const Sidebar = (props: SidebarProps) => {
  const { children, className, ...rest } = props;

  return (
    <div {...rest} className={cn("sidebar", className)}>
      {children}
    </div>
  );
};

export default Sidebar;
