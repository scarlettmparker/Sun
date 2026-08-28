import { RouteObject, useRoutes } from "react-router-dom";
import { lazy, Suspense } from "react";
import type { RouteMeta } from "@sun/ssr/server";
import BlogSkeleton from "./_components/blog/skeleton/blog-skeleton";
import HubSkeleton from "./_components/hub/skeleton/hub-skeleton";
import Gallery from "./routes/gallery";

const Index = lazy(() => import("~/routes/index"));
const NotFound = lazy(() => import("~/routes/not-found"));
const StemPlayerPage = lazy(() => import("~/routes/stem-player/stem-player"));
const StemPlayerDetailsPage = lazy(() => import("~/routes/stem-player/[id]"));
const BlogLayout = lazy(() => import("~/routes/blog"));
const BlogListPage = lazy(() => import("~/routes/blog/list"));
const BlogPostPage = lazy(() => import("~/routes/blog/[id]"));
const CreateBlogPostPage = lazy(() => import("~/routes/blog/create"));
const HubPage = lazy(() => import("~/routes/hub"));
const Login = lazy(() => import("~/routes/login"));

/**
 * Per-route SEO metadata, keyed by the composed route path.
 */
export const routeMeta: Record<string, RouteMeta> = {
  "/": {
    title: "Scarlet Sun | Home",
    description: "Personal knowledge hub — browse blog types and curated docs.",
  },
  blog: {
    title: "Blog | Scarlet Sun",
    description: "Browse knowledge, reviews and documentation.",
  },
  "blog/:id": {
    title: "Post | Scarlet Sun",
    description: "Read a blog post.",
  },
  gallery: {
    title: "Gallery | Scarlet Sun",
    description: "Browse gallery items.",
  },
  "stem-player": {
    title: "Stem Player | Scarlet Sun",
    description: "Play and isolate stems.",
  },
  admin: {
    title: "Admin | Scarlet Sun",
    description: "Manage the ecosystem hub.",
  },
  login: {
    title: "Login | Scarlet Sun",
    description: "Sign in to continue.",
  },
};

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
    element: (
      <Suspense fallback={<BlogSkeleton />}>
        <BlogLayout />
      </Suspense>
    ),
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<BlogSkeleton />}>
            <BlogListPage />
          </Suspense>
        ),
      },
      {
        path: "create",
        element: <CreateBlogPostPage />,
      },
      {
        path: ":id",
        element: <BlogPostPage />,
      },
    ],
  },
  {
    path: "gallery",
    element: <Gallery />,
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
    path: "login",
    element: (
      <Suspense fallback={null}>
        <Login />
      </Suspense>
    ),
  },
  {
    path: "hub",
    element: (
      <Suspense fallback={<HubSkeleton />}>
        <HubPage />
      </Suspense>
    ),
  },
  {
    path: "admin",
    element: (
      <Suspense fallback={<HubSkeleton />}>
        <HubPage />
      </Suspense>
    ),
  },
  {
    path: "*",
    element: <NotFound />,
  },
];

export const Router = () => {
  return useRoutes(routes);
};
