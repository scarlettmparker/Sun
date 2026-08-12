import { executeMutation } from "@sun/ssr";
import type { MutationResult } from "@sun/ssr";
import type { ControlAction, HubAppConfig, HubMode } from "~/server/hub/types";

/**
 * Adds an app to the registry.
 */
export async function createHubApp(
  app: HubAppConfig,
  token: string,
): Promise<MutationResult> {
  return executeMutation("hub/apps/create", { app, token });
}

/**
 * Updates an app in the registry.
 */
export async function updateHubApp(
  key: string,
  app: HubAppConfig,
  token: string,
): Promise<MutationResult> {
  return executeMutation("hub/apps/update", { key, app, token });
}

/**
 * Removes an app from the registry.
 */
export async function deleteHubApp(
  key: string,
  token: string,
): Promise<MutationResult> {
  return executeMutation("hub/apps/delete", { key, token });
}

/**
 * Starts, stops or restarts a hub app.
 */
export async function controlHubApp(
  action: ControlAction,
  key: string,
  token: string,
): Promise<MutationResult> {
  return executeMutation(`hub/apps/${action}`, { key, token });
}

/**
 * Switches the ecosystem mode and restarts managed apps.
 */
export async function setHubMode(
  mode: HubMode,
  token: string,
): Promise<MutationResult> {
  return executeMutation("hub/mode", { mode, token });
}

/**
 * Spawns every enabled app whose port is free.
 */
export async function reconcileHubApps(
  token: string,
): Promise<MutationResult> {
  return executeMutation("hub/reconcile", { token });
}
