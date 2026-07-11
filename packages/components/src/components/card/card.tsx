import { cn } from "~/utils/cn";
import styles from "./card.module.css";

type CardProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Card Component.
 */
const Card = (props: CardProps) => {
  const { className, children, ...rest } = props;

  return (
    <article className={cn(styles.card, className)} {...rest}>
      {children}
    </article>
  );
};

type CardHeaderProps = React.HTMLAttributes<HTMLElement>;

/**
 * Scarlet UI Card Header.
 */
const CardHeader = (props: CardHeaderProps) => {
  const { className, children, ...rest } = props;

  return (
    <header className={cn(styles.card_header, className)} {...rest}>
      {children}
    </header>
  );
};

type CardTitleProps = React.HTMLAttributes<HTMLHeadingElement>;

/**
 * Scarlet UI Card Title.
 */
const CardTitle = (props: CardTitleProps) => {
  const { className, children, ...rest } = props;

  return (
    <h3 className={cn(styles.card_title, className)} {...rest}>
      {children}
    </h3>
  );
};

type CardDescriptionProps = React.HTMLAttributes<HTMLParagraphElement>;

/**
 * Scarlet UI Card Description.
 */
const CardDescription = (props: CardDescriptionProps) => {
  const { className, children, ...rest } = props;

  return (
    <p className={cn(styles.card_description, className)} {...rest}>
      {children}
    </p>
  );
};

type CardBodyProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Card Body.
 */
const CardBody = (props: CardBodyProps) => {
  const { className, children, ...rest } = props;

  return (
    <div className={cn(styles.card_body, className)} {...rest}>
      {children}
    </div>
  );
};

type CardFooterProps = React.HTMLAttributes<HTMLElement>;

/**
 * Scarlet UI Card Footer.
 */
const CardFooter = (props: CardFooterProps) => {
  const { className, children, ...rest } = props;

  return (
    <footer className={cn(styles.card_footer, className)} {...rest}>
      {children}
    </footer>
  );
};

export default Card;
export { CardHeader, CardTitle, CardDescription, CardBody, CardFooter };
