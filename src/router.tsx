import { RouteObject, useRoutes } from "react-router-dom";
import Index from "~/routes/index";
import NotFound from "~/routes/not-found";
import StemPlayerPage from "./routes/stem-player/stem-player";
import StemPlayerDetailsPage from "./routes/stem-player/[id]";
import BlogPage from "./routes/blog";

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
