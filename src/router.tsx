import { RouteObject, useRoutes } from "react-router-dom";
import Index from "~/routes/index";
import NotFound from "~/routes/not-found";

/**
 * List of routes.
 */
const routes: RouteObject[] = [
  {
    path: "/",
    element: <Index />,
  },
  {
    path: "*",
    element: <NotFound />,
  },
];

export const Router = () => {
  return useRoutes(routes);
};
