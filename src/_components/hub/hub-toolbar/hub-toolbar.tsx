import { useTranslation } from "react-i18next";
import { Button } from "@sun/components";
import ModeToggle from "~/_components/hub/mode-toggle";
import { useHub } from "~/routes/hub/hub-context";
import styles from "./hub-toolbar.module.css";

/**
 * Mode switcher and top-level actions.
 */
const HubToolbar = () => {
  const { t } = useTranslation("hub");
  const { mode, onMode, onReconcile, onAdd } = useHub();

  return (
    <div className={styles.toolbar}>
      <ModeToggle mode={mode} onChange={onMode} />
      <Button variant="secondary" onClick={onReconcile}>
        {t("reconcile")}
      </Button>
      <Button variant="secondary" onClick={onAdd}>
        {t("addApp")}
      </Button>
    </div>
  );
};

export default HubToolbar;
