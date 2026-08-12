import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { usePageData } from "@sun/ssr/react";
import type { MutationResult } from "@sun/ssr";
import { HubContext } from "./hub-context";
import { getHubToken, setHubToken, useHubStatusStream } from "./api";
import {
  controlHubApp,
  createHubApp,
  deleteHubApp,
  reconcileHubApps,
  setHubMode,
  updateHubApp,
} from "~/server/actions/hub";
import type {
  ControlAction,
  HubAppConfig,
  HubMode,
  HubRegistry,
} from "~/server/hub/types";

type HubProviderProps = {
  /**
   * Rendered inside the hub context.
   */
  children: ReactNode;
};

/**
 * Owns the hub page state and registry, exposing them via useHub.
 */
const HubProvider = ({ children }: HubProviderProps) => {
  const { data: registry } = usePageData<HubRegistry>("hubRegistry", "hub");
  const statuses = useHubStatusStream();
  const [token, setToken] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<HubAppConfig | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<HubAppConfig | null>(null);

  useEffect(() => {
    setToken(getHubToken());
  }, []);

  const run = useCallback(async (action: () => Promise<MutationResult>) => {
    const result = await action();
    setError(result.__typename === "StandardError" ? result.message : null);
  }, []);

  const handleTokenChange = useCallback((value: string) => {
    setToken(value);
    setHubToken(value);
  }, []);

  const handleControl = useCallback(
    (action: ControlAction, key: string) => {
      void run(() => controlHubApp(action, key, token));
    },
    [run, token],
  );

  const handleToggleEnabled = useCallback(
    (app: HubAppConfig) => {
      void run(() =>
        updateHubApp(app.key, { ...app, enabled: !app.enabled }, token),
      );
    },
    [run, token],
  );

  const handleMode = useCallback(
    (mode: HubMode) => {
      void run(() => setHubMode(mode, token));
    },
    [run, token],
  );

  const handleReconcile = useCallback(() => {
    void run(() => reconcileHubApps(token));
  }, [run, token]);

  const handleAdd = useCallback(() => setCreating(true), []);
  const handleEdit = useCallback((app: HubAppConfig) => setEditing(app), []);
  const handleDelete = useCallback((app: HubAppConfig) => setDeleting(app), []);

  const closeForm = useCallback(() => {
    setCreating(false);
    setEditing(null);
  }, []);

  const handleFormSubmit = useCallback(
    (app: HubAppConfig) => {
      const action = editing
        ? () => updateHubApp(app.key, app, token)
        : () => createHubApp(app, token);
      void run(action);
      closeForm();
    },
    [editing, run, token, closeForm],
  );

  const closeDelete = useCallback(() => setDeleting(null), []);

  const handleConfirmDelete = useCallback(() => {
    if (deleting) {
      void run(() => deleteHubApp(deleting.key, token));
    }
    setDeleting(null);
  }, [deleting, run, token]);

  const value = useMemo(
    () => ({
      registry,
      statuses,
      mode: registry?.mode ?? ("dev" as const),
      token,
      error,
      editing,
      creating,
      deleting,
      onTokenChange: handleTokenChange,
      onControl: handleControl,
      onToggleEnabled: handleToggleEnabled,
      onMode: handleMode,
      onReconcile: handleReconcile,
      onAdd: handleAdd,
      onEdit: handleEdit,
      onDelete: handleDelete,
      closeForm,
      onSubmit: handleFormSubmit,
      closeDelete,
      confirmDelete: handleConfirmDelete,
    }),
    [
      registry,
      statuses,
      token,
      error,
      editing,
      creating,
      deleting,
      handleTokenChange,
      handleControl,
      handleToggleEnabled,
      handleMode,
      handleReconcile,
      handleAdd,
      handleEdit,
      handleDelete,
      closeForm,
      handleFormSubmit,
      closeDelete,
      handleConfirmDelete,
    ],
  );

  return <HubContext.Provider value={value}>{children}</HubContext.Provider>;
};

export default HubProvider;
