import { useTranslation } from "react-i18next";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Input,
  Label,
} from "@sun/components";
import HubToolbar from "~/_components/hub/hub-toolbar";
import { useHub } from "~/routes/hub/hub-context";
import styles from "./hub-header.module.css";

/**
 * Hub title card with the toolbar, token field and error line.
 */
const HubHeader = () => {
  const { t } = useTranslation("hub");
  const { token, error, onTokenChange } = useHub();

  return (
    <Card className={styles.card}>
      <CardHeader>
        <CardTitle>{t("title")}</CardTitle>
      </CardHeader>
      <CardBody>
        <HubToolbar />
        <div className={styles.token_field}>
          <Label>{t("token.label")}</Label>
          <Input
            type="password"
            placeholder={t("token.placeholder")}
            className={styles.token_input}
            value={token}
            onChange={(event) => onTokenChange(event.target.value)}
          />
        </div>
        {error && <p className={styles.error}>{error}</p>}
      </CardBody>
    </Card>
  );
};

export default HubHeader;
