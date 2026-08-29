import { Suspense, useEffect } from "react";
import { Breadcrumb, useBreadcrumbContext } from "@sun/components";
import { Card, CardBody } from "@sun/components";
import CreateBlogForm from "~/_components/blog/create/";
import styles from "./create-blog-post.module.css";

/**
 * Page for creating a new blog post.
 */
const CreateBlogPostPage = () => {
  const { setBreadcrumbs, setCurrent } = useBreadcrumbContext();

  useEffect(() => {
    setBreadcrumbs([
      { label: "Blog", href: "/blog" },
      { label: "New", href: "/blog/create" },
    ]);
    setCurrent("/blog/create");
  }, [setBreadcrumbs, setCurrent]);

  return (
    <div className={styles.create_blog_page}>
      <Breadcrumb />
      <Suspense fallback={null}>
        <Card>
          <CardBody>
            <CreateBlogForm />
          </CardBody>
        </Card>
      </Suspense>
    </div>
  );
};

export default CreateBlogPostPage;
