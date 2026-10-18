import { usePageData } from "@sun/ssr/react";
import type { StatusReport } from "~/server/status/types";
import styles from "./status.module.css";

/**
 * Plain status list for humans and uptime monitors.
 */
const Status = () => {
  const { data } = usePageData<StatusReport>("status", "status");
  const backend = data.backend.available ? "available" : "unavailable";
  const backendSince = data.backend.sinceLabel
    ? ` since ${data.backend.sinceLabel}`
    : "";

  return (
    <div className={styles.status_wrapper}>
      <h1>Status</h1>
      <p>{`Backend: ${backend}${backendSince}`}</p>
      <ul className={styles.list}>
        {data.apps.map((app) => (
          <li key={app.key} className={styles.row}>
            {`${app.name}: ${app.state}${
              app.sinceLabel ? ` since ${app.sinceLabel}` : ""
            }`}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default Status;
