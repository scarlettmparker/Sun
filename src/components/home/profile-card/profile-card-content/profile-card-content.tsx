import { useState, useTransition } from "react";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import {
  Button,
  CardBody,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  SeededAvatar,
} from "@sun/components";
import type { MeQuery } from "~/generated/graphql";
import ConfirmDeleteAccountDialog from "../confirm-delete-account-dialog";
import styles from "./profile-card-content.module.css";

type CurrentUser = NonNullable<MeQuery["gaiaQueries"]["me"]>;

/**
 * Content that depends on the current user.
 */
const ProfileCardContent = () => {
  const { t } = useTranslation("home");
  const { data: currentUser } = usePageData<CurrentUser | null>(
    "currentUser",
    "currentUser",
    {},
  );
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [isPending, startTransition] = useTransition();

  const handleDelete = () => {
    startTransition(async () => {
      setConfirmOpen(false);
    });
  };

  if (!currentUser) {
    return (
      <>
        <CardHeader>
          <CardTitle>{t("profile.title")}</CardTitle>
          <CardDescription>
            <a
              href="https://github.com/scarlettmparker"
              target="_blank"
              rel="noopener noreferrer"
              className={styles.github_link}
            >
              {t("profile.github-label")}
            </a>
          </CardDescription>
        </CardHeader>
        <CardBody>
          <div className={styles.body}>
            <p className={styles.tbd}>{t("profile.tbd")}</p>
          </div>
        </CardBody>
        <CardFooter>
          <Button
            variant="secondary"
            title={t("profile.login")}
            aria-label={t("profile.login")}
            onClick={() => window.location.assign("/login")}
          >
            {t("profile.login")}
          </Button>
        </CardFooter>
      </>
    );
  }

  return (
    <>
      <CardHeader className={styles.header}>
        <SeededAvatar
          username={currentUser.username}
          size={64}
          className={styles.header_avatar}
        />
        <div className={styles.header_text}>
          <CardTitle>{currentUser.username}</CardTitle>
          <CardDescription>
            <a
              href="https://github.com/scarlettmparker"
              target="_blank"
              rel="noopener noreferrer"
              className={styles.github_link}
            >
              {t("profile.github-label")}
            </a>
          </CardDescription>
        </div>
      </CardHeader>
      <CardBody>
        <div className={styles.body}>
          <p className={styles.tbd}>{t("profile.tbd")}</p>
        </div>
      </CardBody>
      <CardFooter className={styles.footer}>
        <form action="/__logout" method="post" className={styles.logout_form}>
          <Button
            type="submit"
            variant="secondary"
            title={t("profile.logout")}
            aria-label={t("profile.logout")}
          >
            {t("profile.logout")}
          </Button>
        </form>
        <Button
          variant="destructive"
          title={t("profile.delete")}
          aria-label={t("profile.delete")}
          onClick={() => setConfirmOpen(true)}
        >
          {t("profile.delete")}
        </Button>
      </CardFooter>
      <ConfirmDeleteAccountDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        isPending={isPending}
        onConfirm={handleDelete}
      />
    </>
  );
};

export default ProfileCardContent;
