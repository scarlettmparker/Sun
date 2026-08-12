import { useTranslation } from "react-i18next";
import {
  Badge,
  Button,
  Card,
  CardBody,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@sun/components";
import { useHub } from "~/routes/hub/hub-context";
import styles from "./app-card.module.css";
import type { AppRuntimeStatus, HubAppConfig } from "~/server/hub/types";

type AppCardProps = {
  /**
   * App to display.
   */
  app: HubAppConfig;
  /**
   * Live process status.
   */
  status: AppRuntimeStatus | undefined;
};

/**
 * Card showing one ecosystem app and its controls.
 */
const AppCard = ({ app, status }: AppCardProps) => {
  const { t } = useTranslation("hub");
  const { onControl, onToggleEnabled, onEdit, onDelete } = useHub();

  const up = status?.up ?? false;
  const external = status?.external ?? false;
  const isSelf = app.self === true;
  const statusKey = isSelf
    ? "self"
    : up
      ? external
        ? "external"
        : "up"
      : "down";

  return (
    <Card className={styles.card}>
      <CardHeader>
        <CardTitle>{app.name}</CardTitle>
        {app.description && (
          <CardDescription>{app.description}</CardDescription>
        )}
      </CardHeader>
      <CardBody>
        <div className={styles.meta}>
          <Badge variant="default">{t(`status.${statusKey}`)}</Badge>
          {!isSelf && status?.port != null && (
            <span>
              {t("port")}: {status.port}
            </span>
          )}
          {app.url && (
            <a
              href={app.url}
              target="_blank"
              rel="noreferrer"
              className={styles.link}
            >
              {t("visit")}
            </a>
          )}
        </div>
      </CardBody>
      <CardFooter className={styles.actions}>
        {!isSelf && (
          <Button
            variant="secondary"
            onClick={() => onToggleEnabled(app)}
            title={app.enabled ? t("enabled") : t("disabled")}
            aria-label={app.enabled ? t("enabled") : t("disabled")}
          >
            {app.enabled ? t("enabled") : t("disabled")}
          </Button>
        )}
        {!isSelf && app.enabled && up && (
          <>
            <Button
              variant="secondary"
              onClick={() => onControl("stop", app.key)}
              title={t("stop")}
              aria-label={t("stop")}
            >
              {t("stop")}
            </Button>
            <Button
              variant="secondary"
              onClick={() => onControl("restart", app.key)}
              title={t("restart")}
              aria-label={t("restart")}
            >
              {t("restart")}
            </Button>
          </>
        )}
        {!isSelf && app.enabled && !up && (
          <Button
            variant="secondary"
            onClick={() => onControl("start", app.key)}
            title={t("start")}
            aria-label={t("start")}
          >
            {t("start")}
          </Button>
        )}
        {!isSelf && (
          <>
            <Button
              variant="secondary"
              onClick={() => onEdit(app)}
              title={t("edit")}
              aria-label={t("edit")}
            >
              {t("edit")}
            </Button>
            <Button
              variant="secondary"
              onClick={() => onDelete(app)}
              title={t("delete")}
              aria-label={t("delete")}
            >
              {t("delete")}
            </Button>
          </>
        )}
      </CardFooter>
    </Card>
  );
};

export default AppCard;
