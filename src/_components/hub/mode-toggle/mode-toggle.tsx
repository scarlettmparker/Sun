import { useTranslation } from "react-i18next";
import { Button } from "@sun/components";
import styles from "./mode-toggle.module.css";
import type { HubMode } from "~/server/hub/types";

type ModeToggleProps = {
  /**
   * Active mode.
   */
  mode: HubMode;
  /**
   * Requests a mode change.
   */
  onChange: (mode: HubMode) => void;
};

/**
 * Dev/serve mode switcher.
 */
const ModeToggle = ({ mode, onChange }: ModeToggleProps) => {
  const { t } = useTranslation("hub");

  return (
    <div className={styles.toggle}>
      {(["dev", "serve"] as HubMode[]).map((candidate) => (
        <Button
          key={candidate}
          variant={mode === candidate ? "default" : "secondary"}
          onClick={() => onChange(candidate)}
          title={t(`mode.${candidate}`)}
          aria-label={t(`mode.${candidate}`)}
        >
          {t(`mode.${candidate}`)}
        </Button>
      ))}
    </div>
  );
};

export default ModeToggle;
