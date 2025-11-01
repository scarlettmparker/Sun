import { RouteObject, useRoutes } from "react-router-dom";
import Index from "~/routes/index";
import NotFound from "~/routes/not-found";
import StemPlayerPage from "./routes/stem-player/stem-player";
import StemPlayerDetailsPage from "./routes/stem-player/[id]";

/**
 * List of routes.
 */
export const routes: RouteObject[] = [
  {
    path: "/",
    element: <Index />,
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
