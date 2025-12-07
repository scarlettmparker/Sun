import React, { createContext, useContext } from "react";
import { cn } from "~/utils/cn";
import styles from "./form.module.css";
import Label from "../label";
import Input from "../input";
import TextArea from "../textarea";
import MarkdownEditor from "../markdown-editor";

const FormItemContext = createContext<{ name?: string }>({});

type FormProps = React.FormHTMLAttributes<HTMLFormElement>;

/**
 * Scarlet UI Form Component.
 */
const Form = (props: FormProps) => {
  const { className, children, ...rest } = props;

  return (
    <form className={cn(className, styles.form)} {...rest}>
      {children}
    </form>
  );
};

type FormFieldProps = React.HTMLAttributes<HTMLDivElement> & {
  name?: string;
};

/**
 * Scarlet UI Form Field.
 */
const FormField = (props: FormFieldProps) => {
  const { className, children, name, ...rest } = props;

  return (
    <FormItemContext.Provider value={{ name }}>
      {/* TODO: depending on child type (label, message, input, we choose were to display things */}
      <div className={cn(className, styles.form_item)} {...rest}>
        {children}
      </div>
    </FormItemContext.Provider>
  );
};

type FormLabelProps = React.LabelHTMLAttributes<HTMLLabelElement>;

/**
 * Scarlet UI Form Label.
 */
const FormLabel = (props: FormLabelProps) => {
  const { name } = useContext(FormItemContext);
  return <Label {...props} htmlFor={name} />;
};

type FormItemChildProps =
  | React.ComponentProps<typeof Input>
  | React.ComponentProps<typeof TextArea>
  | React.ComponentProps<typeof MarkdownEditor>;

type FormItemProps = {
  children: React.ReactElement<FormItemChildProps>;
};

/**
 * Scarlet UI Form Item.
 */
const FormItem = (props: FormItemProps) => {
  const { children } = props;
  const { name } = useContext(FormItemContext);

  // Tighten typing
  if (
    children.type !== Input &&
    children.type !== TextArea &&
    children.type != MarkdownEditor
  ) {
    throw new Error("FormItem only accepts Input or TextArea as children");
  }

  return React.cloneElement(children, { name, id: name });
};

type FormFooterProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Scarlet UI Form Footer.
 */
const FormFooter = (props: FormFooterProps) => {
  const { className, children, ...rest } = props;
  return (
    <footer className={cn(className, styles.form_footer)} {...rest}>
      {children}
    </footer>
  );
};

export default Form;
export { FormField, FormItem, FormLabel, FormFooter };
