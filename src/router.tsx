import { RouteObject, useRoutes } from "react-router-dom";
import Index from "~/routes/index";
import NotFound from "~/routes/not-found";
import StemPlayerPage from "./routes/stem-player/stem-player";
import StemPlayerDetailsPage from "./routes/stem-player/[id]";
import BlogPage from "./routes/blog";
import BlogPostPage from "./routes/blog/[id]";
import CreateBlogPostPage from "./routes/blog/create";

/**
 * List of routes.
 */
export const routes: RouteObject[] = [
  {
    path: "/",
    element: <Index />,
  },
  {
    path: "blog",
    element: <BlogPage />,
  },
  {
    path: "blog/:id",
    element: <BlogPostPage />,
  },
  {
    path: "blog/create",
    element: <CreateBlogPostPage />,
  },
  {
    path: "stem-player",
    element: <StemPlayerPage />,
    children: [
      {
        path: ":id",
        element: <StemPlayerDetailsPage />,
      },
    ],
  },
  {
    path: "*",
    element: <NotFound />,
  },
];

export const Router = () => {
  return useRoutes(routes);
};
