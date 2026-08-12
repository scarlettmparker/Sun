import { createContext, useContext } from "react";
import type {
  AppRuntimeStatus,
  ControlAction,
  HubAppConfig,
  HubMode,
  HubRegistry,
} from "~/server/hub/types";

type HubContextValue = {
  /**
   * Loaded hub registry.
   */
  registry: HubRegistry | null;
  /**
   * Live per-app statuses.
   */
  statuses: Record<string, AppRuntimeStatus>;
  /**
   * Active mode.
   */
  mode: HubMode;
  /**
   * Admin token for control mutations.
   */
  token: string;
  /**
   * Latest mutation error, if any.
   */
  error: string | null;
  /**
   * App being edited, or null.
   */
  editing: HubAppConfig | null;
  /**
   * Whether the create dialog is open.
   */
  creating: boolean;
  /**
   * App awaiting delete confirmation, or null.
   */
  deleting: HubAppConfig | null;
  /**
   * Persists the admin token.
   */
  onTokenChange: (value: string) => void;
  /**
   * Starts, stops or restarts an app.
   */
  onControl: (action: ControlAction, key: string) => void;
  /**
   * Toggles whether an app is managed.
   */
  onToggleEnabled: (app: HubAppConfig) => void;
  /**
   * Requests a mode change.
   */
  onMode: (mode: HubMode) => void;
  /**
   * Requests a reconcile.
   */
  onReconcile: () => void;
  /**
   * Opens the create dialog.
   */
  onAdd: () => void;
  /**
   * Opens the edit dialog.
   */
  onEdit: (app: HubAppConfig) => void;
  /**
   * Opens the delete confirmation dialog.
   */
  onDelete: (app: HubAppConfig) => void;
  /**
   * Closes the create/edit dialog.
   */
  closeForm: () => void;
  /**
   * Submits the create/edit form.
   */
  onSubmit: (app: HubAppConfig) => void;
  /**
   * Closes the delete confirmation dialog.
   */
  closeDelete: () => void;
  /**
   * Confirms the pending deletion.
   */
  confirmDelete: () => void;
};

export const HubContext = createContext<HubContextValue | null>(null);

/**
 * Reads the hub context, throwing when used outside the provider.
 */
export function useHub(): HubContextValue {
  const value = useContext(HubContext);
  if (!value) {
    throw new Error("useHub must be used within HubProvider");
  }
  return value;
}
