import { useState, useTransition } from "react";
import { useTranslation } from "react-i18next";
import { usePageData } from "@sun/ssr/react";
import {
  Button,
  Card,
  CardBody,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  SeededAvatar,
} from "@sun/components";
import ConfirmDeleteAccountDialog from "~/components/profile/confirm-delete-account-dialog";
import type { MeQuery } from "~/generated/graphql";
import styles from "./profile.module.css";

type CurrentUser = NonNullable<MeQuery["gaiaQueries"]["me"]>;

/**
 * Manage profile page with danger zone.
 */
const ProfilePage = () => {
  const { t } = useTranslation("home");
  const { data: currentUser } = usePageData<CurrentUser | null>(
    "currentUser",
    "currentUser",
    {},
  );
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [isPending, startTransition] = useTransition();

  const handleConfirm = () => {
    startTransition(async () => {
      setConfirmOpen(false);
    });
  };

  if (!currentUser) {
    return (
      <div className={styles.wrapper}>
        <Card className={styles.card}>
          <CardBody>
            <p>{t("profile.tbd")}</p>
          </CardBody>
        </Card>
      </div>
    );
  }

  return (
    <div className={styles.wrapper}>
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
      </Card>
      <Card className={styles.danger_card}>
        <CardHeader>
          <CardTitle>{t("profile.delete-title")}</CardTitle>
          <CardDescription>{t("profile.delete-body")}</CardDescription>
        </CardHeader>
        <CardFooter className={styles.danger_footer}>
          <Button
            variant="destructive"
            title={t("profile.delete")}
            aria-label={t("profile.delete")}
            onClick={() => setConfirmOpen(true)}
          >
            {t("profile.delete")}
          </Button>
        </CardFooter>
      </Card>
      <ConfirmDeleteAccountDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        isPending={isPending}
        onConfirm={handleConfirm}
      />
    </div>
  );
};

export default ProfilePage;
