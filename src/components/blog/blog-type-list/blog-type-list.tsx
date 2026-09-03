import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Button } from "@sun/components";
import {
  BookOpenIcon,
  DocumentTextIcon,
  InformationCircleIcon,
  LightBulbIcon,
  QuestionMarkCircleIcon,
  Squares2X2Icon,
  StarIcon,
} from "@heroicons/react/24/outline";
import type { BlogPostTypesQuery } from "~/generated/graphql";
import styles from "./blog-type-list.module.css";

type BlogTypeListProps = {
  /**
   * Currently selected type name.
   */
  selectedType: string | null;
  /**
   * Called when a type is selected.
   */
  onSelect: (type: string | null) => void;
  /**
   * Layout variant.
   */
  variant?: "vertical" | "horizontal";
};

/**
 * Renders type filter buttons.
 */
const BlogTypeList = (props: BlogTypeListProps) => {
  const { selectedType, onSelect, variant = "vertical" } = props;
  const { t } = useTranslation("blog");
  const { data: types } = usePageData<
    BlogPostTypesQuery["blogQueries"]["blogPostTypes"]
  >("types", "home", {});

  return (
    <>
      <Button
        className={
          variant === "horizontal"
            ? styles.type_button_horizontal
            : styles.type_button
        }
        variant={selectedType == null ? "default" : "secondary"}
        onClick={() => onSelect(null)}
        title={t("filter.all")}
        aria-label={t("filter.all")}
      >
        <Squares2X2Icon className={styles.type_icon} width={16} height={16} />
        <span className={styles.type_name}>{t("filter.all")}</span>
      </Button>
      {(types ?? []).map((postType) => {
        const Icon = getIconForType(postType.name);
        return (
          <Button
            key={postType.id}
            className={
              variant === "horizontal"
                ? styles.type_button_horizontal
                : styles.type_button
            }
            variant={selectedType === postType.name ? "default" : "secondary"}
            onClick={() => onSelect(postType.name)}
            title={t(`types.${postType.name}`, {
              defaultValue: postType.name,
            })}
            aria-label={t(`types.${postType.name}`, {
              defaultValue: postType.name,
            })}
          >
            <Icon className={styles.type_icon} width={16} height={16} />
            <span className={styles.type_name}>
              {t(`types.${postType.name}`, { defaultValue: postType.name })}
            </span>
          </Button>
        );
      })}
    </>
  );
};

/**
 * Maps blog post type to heroicon. Checklist style: per-item icon data-driven.
 */
function getIconForType(name: string) {
  switch (name) {
    case "KNOWLEDGE":
      return LightBulbIcon;
    case "REVIEW":
      return StarIcon;
    case "DOCS":
      return DocumentTextIcon;
    case "BOT_FAQ":
      return QuestionMarkCircleIcon;
    case "BOT_HELP":
      return InformationCircleIcon;
    default:
      return BookOpenIcon;
  }
}

export default BlogTypeList;
