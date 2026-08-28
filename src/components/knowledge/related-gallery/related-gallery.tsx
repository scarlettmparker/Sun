import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import { Card, CardBody, CardHeader, CardTitle } from "@sun/components";
import type { ListGalleryItemsByRemoteObjectsQuery } from "~/generated/graphql";
import styles from "./related-gallery.module.css";

type RelatedGalleryProps = {
  /**
   * Remote object ids for gallery items.
   */
  ids: string[];
};

/**
 * Renders related gallery items resolved via remoteObject.
 */
const RelatedGallery = (props: RelatedGalleryProps) => {
  const { ids } = props;
  const { t } = useTranslation("blog");

  if (ids.length === 0) {
    return null;
  }

  const { data: galleryItems } = usePageData<
    NonNullable<ListGalleryItemsByRemoteObjectsQuery["galleryQueries"]["listByRemoteObjects"]>
  >("galleryItems", "blog/gallery", { ids: JSON.stringify(ids) } as unknown as Record<string, string>);

  if ((galleryItems?.length ?? 0) === 0) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("detail.gallery")}</CardTitle>
      </CardHeader>
      <CardBody>
        <div className={styles.list_body}>
          {(galleryItems ?? []).map((item) => {
            if (item == null) {
              return null;
            }
            return (
              <Link key={item.id} to={`/gallery/${item.id}`} className={styles.row_link}>
                <span className={styles.row_title}>{item.title}</span>
              </Link>
            );
          })}
        </div>
      </CardBody>
    </Card>
  );
};

export default RelatedGallery;
