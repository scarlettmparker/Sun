import { RouteObject, useRoutes } from "react-router-dom";
import { lazy } from "react";

const Index = lazy(() => import("~/routes/index"));
const NotFound = lazy(() => import("~/routes/not-found"));
const StemPlayerPage = lazy(() => import("~/routes/stem-player/stem-player"));
const StemPlayerDetailsPage = lazy(() => import("~/routes/stem-player/[id]"));
const BlogPage = lazy(() => import("~/routes/blog"));
const BlogPostPage = lazy(() => import("~/routes/blog/[id]"));
const CreateBlogPostPage = lazy(() => import("~/routes/blog/create"));

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
