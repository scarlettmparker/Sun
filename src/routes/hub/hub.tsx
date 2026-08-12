import { Suspense } from "react";
import HubProvider from "./hub-provider";
import HubHeader from "~/_components/hub/hub-header";
import AppGrid from "~/_components/hub/app-grid";
import AppForm from "~/_components/hub/app-form";
import ConfirmDeleteAppDialog from "~/_components/hub/confirm-delete-app-dialog";
import HubSkeleton from "~/_components/hub/skeleton/hub-skeleton";
import styles from "./hub.module.css";

/**
 * Hub page: manage the ecosystem apps and their run mode.
 */
const HubPage = () => (
  <Suspense fallback={<HubSkeleton />}>
    <HubProvider>
      <div className={styles.hub_wrapper}>
        <HubHeader />
        <AppGrid />
        <AppForm />
        <ConfirmDeleteAppDialog />
      </div>
    </HubProvider>
  </Suspense>
);

export default HubPage;
