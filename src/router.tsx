import { RouteObject, useRoutes } from "react-router-dom";
import Index from "~/routes/index";
import NotFound from "~/routes/not-found";
import StemPlayerPage from "./routes/stem-player";

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
  },
  {
    path: "*",
    element: <NotFound />,
  },
];

export const Router = () => {
  return useRoutes(routes);
};
