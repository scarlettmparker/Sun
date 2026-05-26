if (import.meta.env.DEV) {
  const serverBase = import.meta.env.VITE_SERVER_BASE || "localhost";

  if (!window.__vite_plugin_react_preamble_installed__) {
    window.__vite_plugin_react_preamble_installed__ = true;

    import(/* @vite-ignore */ `${serverBase}/@react-refresh`).then(
      (RefreshRuntimeModule) => {
        const RefreshRuntime = RefreshRuntimeModule.default;

        RefreshRuntime.injectIntoGlobalHook(window);
        window.$RefreshReg$ = () => {};
        window.$RefreshSig$ = () => (type) => type;

        import("./entry-client.tsx");
      },
    );
  } else {
    import("./entry-client.tsx");
  }
} else {
  import("./entry-client.tsx");
}
