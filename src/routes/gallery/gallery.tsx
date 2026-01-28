import Card, {
  CardBody,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "~/components/card";

import styles from "./gallery.module.css";
import { getPageData, pageDataRegistry } from "~/utils/page-data";
import { ListGalleryItemsQuery } from "~/generated/graphql";
import { fetchListGalleryItems } from "~/utils/api";
import MarkdownViewer from "~/components/markdown-viewer";

const Gallery = () => {
  const { data: galleryItems } = getPageData<
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
                />
                {galleryItem?.content && (
                  <MarkdownViewer className={styles.gallery_content}>
                    {galleryItem.content}
                  </MarkdownViewer>
                )}
              </>
            )}
          </CardBody>
        </Card>
      ))}
      {/* <Card className={styles.gallery_card}>
        <CardHeader>
          <CardTitle>Card Title</CardTitle>
          <CardDescription>Card Description</CardDescription>
        </CardHeader>
        <CardBody>Card Body</CardBody>
        <CardFooter>Card Footer</CardFooter>
      </Card> */}
    </div>
  );
};

export default Gallery;

/**
 * Server-side data fetching function for Gallery page.
 */
async function getGalleryData(): Promise<Record<string, unknown> | null> {
  try {
    const result = await fetchListGalleryItems();
    if (result?.data && result.success) {
      const galleryItems = (result.data as ListGalleryItemsQuery).galleryQueries
        .list;
      if (galleryItems) {
        return { galleryItems: galleryItems };
      }
    }
    return null;
  } catch (error) {
    console.error("Failed to fetch gallery items:", error);
    return null;
  }
}

/**
 * Register the data loader for this page.
 */
export function registerGalleryDataLoader(): void {
  pageDataRegistry.registerPageDataLoader("gallery", getGalleryData);
}
