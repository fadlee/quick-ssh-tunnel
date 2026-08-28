import { NativeModules } from "react-native";
import type { Connection } from "@shared/types";

export type Status = "running" | "stopped";

interface SshTunnelNativeModule {
  startTunnel(
    connectionId: string,
    sshTarget: string,
    mode: string,
    port: number,
    remoteHost: string,
    compression: boolean,
  ): Promise<void>;
  stopTunnel(connectionId: string): Promise<void>;
  getStatus(connectionId: string): Promise<Status>;
  getRunningTunnels(): Promise<string[]>;
}

const nativeModule = NativeModules.SshTunnelModule as
  | SshTunnelNativeModule
  | undefined;

/** Throws a descriptive error if the native module is not linked. */
function requireModule(): SshTunnelNativeModule {
  if (!nativeModule) {
    throw new Error(
      "SshTunnelModule native module is not linked. Rebuild the Android project.",
    );
  }
  return nativeModule;
}

export function startTunnel(connection: Connection): Promise<void> {
  return requireModule().startTunnel(
    connection.id,
    connection.sshTarget,
    connection.mode,
    connection.port,
    connection.remoteHost,
    connection.compression,
  );
}

export function stopTunnel(connection: Connection): Promise<void> {
  return requireModule().stopTunnel(connection.id);
}

export function getStatus(connection: Connection): Promise<Status> {
  return requireModule().getStatus(connection.id);
}

export function getRunningTunnels(): Promise<string[]> {
  return requireModule().getRunningTunnels();
}
