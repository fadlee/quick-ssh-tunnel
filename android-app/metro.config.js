const { getDefaultConfig, mergeConfig } = require("@react-native/metro-config");
const path = require("path");

const projectRoot = __dirname;
const parentRoot = path.resolve(projectRoot, "..");
const sharedLib = path.resolve(parentRoot, "src", "lib");

/**
 * Metro config for the Android app.
 *
 * - Watches the parent directory so shared pure logic in ../src/lib/
 *   (core.ts, types.ts) can be bundled alongside the app.
 * - Resolves the `@shared/*` path alias to ../src/lib/*.
 * - Falls back to both the app's and the parent project's node_modules.
 */
module.exports = (async () => {
  const defaultConfig = await getDefaultConfig(projectRoot);
  const defaultResolver = defaultConfig.resolver.resolveRequest;

  const config = {
    watchFolders: [parentRoot],
    resolver: {
      nodeModulesPaths: [
        path.resolve(projectRoot, "node_modules"),
        path.resolve(parentRoot, "node_modules"),
      ],
      resolveRequest: (context, moduleName, platform) => {
        if (moduleName.startsWith("@shared/")) {
          const rewritten = moduleName.replace(
            /^@shared\//,
            sharedLib + "/",
          );
          return defaultResolver(context, rewritten, platform);
        }
        return defaultResolver(context, moduleName, platform);
      },
    },
  };

  return mergeConfig(defaultConfig, config);
})();
