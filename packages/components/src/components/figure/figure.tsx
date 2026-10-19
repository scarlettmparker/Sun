import { cn } from "~/utils/cn";
import styles from "./figure.module.css";

type FigureProps = React.ImgHTMLAttributes<HTMLImageElement> & {
  /**
   * Source URL for the image.
   */
  src: string;
  /**
   * Optional caption shown beneath the image.
   */
  caption?: React.ReactNode;
};

/**
 * A figure wrapping an image with an optional caption.
 */
const Figure = ({ src, caption, className, ...rest }: FigureProps) => {
  return (
    <figure className={cn(styles.figure, className)}>
      <img className={styles.figure_image} src={src} {...rest} />
      {caption && <figcaption className={styles.figure_caption}>{caption}</figcaption>}
    </figure>
  );
};

export default Figure;
