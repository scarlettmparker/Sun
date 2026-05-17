import { Routes, Route } from "react-router-dom";

export const routes = [
  {
    path: "*",
    element: <div>Home / 404 - extend routes here</div>,
  },
];

export function Router() {
  return (
    <Routes>
      {routes.map((route, index) => (
        <Route key={index} path={route.path} element={route.element} />
      ))}
    </Routes>
  );
}
