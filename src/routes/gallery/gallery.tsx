import {
  Card,
  CardBody,
  CardDescription,
  CardHeader,
  CardTitle,
  MarkdownViewer,
} from "@sun/components";

import styles from "./gallery.module.css";
import { usePageData } from "@sun/ssr/react";
import { ListGalleryItemsQuery } from "~/generated/graphql";

const Gallery = () => {
  const { data: galleryItems } = usePageData<
    ListGalleryItemsQuery["galleryQueries"]["list"]
  >("galleryItems", "gallery");

  if (!galleryItems) {
    return null;
  }

  return (
    <div className={styles.gallery_wrapper}>
      {galleryItems.map((galleryItem) => (
        <Card key={galleryItem?.id} className={styles.gallery_card}>
          <CardHeader>
            <CardTitle>{galleryItem?.title}</CardTitle>
            {galleryItem?.description && (
              <CardDescription>
                <MarkdownViewer>{galleryItem?.description}</MarkdownViewer>
              </CardDescription>
            )}
          </CardHeader>
          <CardBody className={styles.gallery_card}>
            {galleryItem?.imagePath && (
              <>
                <img
                  src={galleryItem.imagePath}
                  title={galleryItem.title}
                  className={styles.gallery_item}
                  draggable={false}
                  loading="lazy"
                  decoding="async"
                />
                {galleryItem?.content && (
                  <Card className={styles.gallery_content}>
                    <CardBody>
                      <MarkdownViewer>{galleryItem.content}</MarkdownViewer>
                    </CardBody>
                  </Card>
                )}
              </>
            )}
          </CardBody>
        </Card>
      ))}
    </div>
  );
};

export default Gallery;
