import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Button } from "@sun/components";
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
};

/**
 * Renders type filter buttons.
 */
const BlogTypeList = (props: BlogTypeListProps) => {
  const { selectedType, onSelect } = props;
  const { t } = useTranslation("blog");
  const { data: types } = usePageData<BlogPostTypesQuery["blogQueries"]["blogPostTypes"]>(
    "types",
    "home",
    {},
  );

  return (
    <>
      <Button variant={selectedType == null ? "default" : "secondary"} onClick={() => onSelect(null)}>
        {t("filter.all")}
      </Button>
      {(types ?? []).map((postType) => (
        <Button
          key={postType.id}
          variant={selectedType === postType.name ? "default" : "secondary"}
          onClick={() => onSelect(postType.name)}
        >
          {t(`types.${postType.name}`, { defaultValue: postType.name })}
        </Button>
      ))}
    </>
  );
};

export default BlogTypeList;
