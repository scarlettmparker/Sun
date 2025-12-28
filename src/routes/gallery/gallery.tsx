import Card, {
  CardBody,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "~/components/card";

import styles from "./gallery.module.css";

const Gallery = () => {
  return (
    <div className={styles.gallery_wrapper}>
      <Card className={styles.gallery_card}>
        <CardHeader>
          <CardTitle>Card Title</CardTitle>
          <CardDescription>Card Description</CardDescription>
        </CardHeader>
        <CardBody>Card Body</CardBody>
        <CardFooter>Card Footer</CardFooter>
      </Card>
    </div>
  );
};

export default Gallery;
