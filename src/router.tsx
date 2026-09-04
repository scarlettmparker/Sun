import { RouteObject, useRoutes } from "react-router-dom";
import { lazy, Suspense } from "react";
import type { RouteMeta } from "@sun/ssr/server";
import BlogSkeleton from "./_components/blog/skeleton/blog-skeleton";
import { BlogDetailSkeleton } from "./components/blog/blog-detail/skeletons";
import { BlogListSkeleton } from "./components/blog/skeletons";
import HubSkeleton from "./_components/hub/skeleton/hub-skeleton";
import ProfileSkeleton from "./routes/profile/skeletons";
import HomeSkeleton from "./components/home/skeletons/home-skeleton";
import LoginSkeleton from "./routes/login/skeletons/login-skeleton";
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
const Profile = lazy(() => import("~/routes/profile"));

/**
 * Per-route SEO metadata, keyed by the composed route path.
 */
export const routeMeta: Record<string, RouteMeta> = {
  "/": {
    title: "Scarlet Sun | Home",
    description:
      "Scarlett Parker - artist and web developer in Exeter. Personal portfolio and hub.",
    ogImage: "/og-default.png",
  },
  blog: {
    title: "Blog | Scarlet Sun",
    description: "Browse knowledge, reviews and documentation.",
    ogImage: "/og-default.png",
  },
  "blog/:id": {
    title: "Post | Scarlet Sun",
    description: "Read a blog post.",
    ogImage: "/og-default.png",
  },
  gallery: {
    title: "Gallery | Scarlet Sun",
    description: "Browse gallery items.",
    ogImage: "/og-default.png",
  },
  "stem-player": {
    title: "Stem Player | Scarlet Sun",
    description: "Play and isolate stems.",
    ogImage: "/og-default.png",
  },
  admin: {
    title: "Admin | Scarlet Sun",
    description: "Manage the ecosystem hub.",
    ogImage: "/og-default.png",
  },
  login: {
    title: "Login | Scarlet Sun",
    description: "Sign in to continue.",
    ogImage: "/og-default.png",
  },
  profile: {
    title: "Profile | Scarlet Sun",
    description: "Manage your profile.",
    ogImage: "/og-default.png",
  },
};

/**
 * List of routes.
 */
export const routes: RouteObject[] = [
  {
    path: "/",
    element: (
      <Suspense fallback={<HomeSkeleton />}>
        <Index />
      </Suspense>
    ),
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
          <Suspense fallback={<BlogListSkeleton />}>
            <BlogListPage />
          </Suspense>
        ),
      },
      {
        path: "create",
        element: (
          <Suspense fallback={null}>
            <CreateBlogPostPage />
          </Suspense>
        ),
      },
      {
        path: ":id",
        element: (
          <Suspense fallback={<BlogDetailSkeleton />}>
            <BlogPostPage />
          </Suspense>
        ),
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
      <Suspense fallback={<LoginSkeleton />}>
        <Login />
      </Suspense>
    ),
  },
  {
    path: "profile",
    element: (
      <Suspense fallback={<ProfileSkeleton />}>
        <Profile />
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
