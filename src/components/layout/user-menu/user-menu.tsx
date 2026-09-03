import { Suspense } from "react";
import { Link, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import {
  Button,
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
  Form,
  SeededAvatar,
} from "@sun/components";
import { CsrfField } from "@sun/ssr/react";
import type { MeQuery } from "~/generated/graphql";
import styles from "./user-menu.module.css";

type CurrentUser = NonNullable<MeQuery["gaiaQueries"]["me"]>;

const PUBLIC_PATHS = ["/login"];

/**
 * Top-right avatar menu for the current user.
 */
const UserMenu = () => {
  const { pathname } = useLocation();

  if (PUBLIC_PATHS.some((p) => pathname.startsWith(p))) {
    return null;
  }

  return (
    <div className={styles.menu}>
      <Suspense fallback={null}>
        <UserMenuContent />
      </Suspense>
    </div>
  );
};

const UserMenuContent = () => {
  const { t } = useTranslation("home");
  const { data: currentUser } = usePageData<CurrentUser | null>(
    "currentUser",
    "currentUser",
    {},
  );

  if (!currentUser) {
    return (
      <Link to="/login" style={{ textDecoration: "none" }}>
        <Button
          variant="secondary"
          title={t("profile.login")}
          aria-label={t("profile.login")}
        >
          {t("profile.login")}
        </Button>
      </Link>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className={styles.trigger} aria-label={currentUser.username}>
          <SeededAvatar username={currentUser.username} size={32} />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className={styles.content}>
        <Card className={styles.card}>
          <CardHeader className={styles.header}>
            <SeededAvatar
              username={currentUser.username}
              size={64}
              className={styles.header_avatar}
            />
            <div className={styles.header_text}>
              <CardTitle>{currentUser.username}</CardTitle>
              <CardDescription>@{currentUser.username}</CardDescription>
            </div>
          </CardHeader>
          <CardFooter className={styles.footer}>
            <Link to="/profile" onClick={(e) => e.stopPropagation()}>
              <Button
                title={t("profile.manage")}
                aria-label={t("profile.manage")}
              >
                {t("profile.manage")}
              </Button>
            </Link>
            <Form action="/__logout" method="post">
              <CsrfField />
              <Button
                type="submit"
                variant="secondary"
                title={t("profile.logout")}
                aria-label={t("profile.logout")}
              >
                {t("profile.logout")}
              </Button>
            </Form>
          </CardFooter>
        </Card>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default UserMenu;
