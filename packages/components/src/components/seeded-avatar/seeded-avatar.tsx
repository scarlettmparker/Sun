import { seedInitial, hashToHue } from "@sun/utils/avatar-colour";
import { cn } from "~/utils/cn";
import styles from "./seeded-avatar.module.css";

type SeededAvatarProps = React.HTMLAttributes<HTMLDivElement> & {
  /**
   * Username to derive initial and colour from.
   */
  username: string;
  /**
   * Diameter in pixels.
   */
  size?: number;
};

/**
 * Circular avatar with initial and seeded HSL colour.
 */
const SeededAvatar = (props: SeededAvatarProps) => {
  const { username, size = 32, className, style, ...rest } = props;
  const initial = seedInitial(username);
  const hue = hashToHue(username || "?");
  const background = `hsl(${hue} 70% 45%)`;
  const foreground = "#ffffff";

  return (
    <div
      {...rest}
      className={cn(styles.avatar, className)}
      style={{
        width: size,
        height: size,
        background,
        color: foreground,
        ...style,
      }}
      aria-label={username}
      title={username}
    >
      <span className={styles.initial}>{initial}</span>
    </div>
  );
};

export default SeededAvatar;
