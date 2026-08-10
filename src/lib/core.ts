import type { Connection } from "./store";

const HOST_PATTERN = /^[A-Za-z0-9][A-Za-z0-9.-]*$/;

export function buildArgs(connection: Connection): string[] {
  const args = [
    "-N",
    "-L",
    `${connection.port}:${connection.remoteHost}:${connection.port}`,
  ];
  if (connection.compression) args.push("-C");
  args.push(
    "-o",
    "BatchMode=yes",
    "-o",
    "ExitOnForwardFailure=yes",
    "-o",
    "ServerAliveInterval=30",
    "-o",
    "ServerAliveCountMax=3",
    connection.sshTarget,
  );
  return args;
}

export function connectionKey(connection: Connection): string {
  return JSON.stringify([
    connection.sshTarget,
    connection.port,
    connection.remoteHost,
    connection.compression,
  ]);
}

export function validateConnection(
  connection: Pick<Connection, "sshTarget" | "port" | "remoteHost">,
): string[] {
  const errors: string[] = [];
  if (!connection.sshTarget.trim()) {
    errors.push("SSH target wajib diisi");
  } else if (
    /\s/.test(connection.sshTarget) ||
    connection.sshTarget.startsWith("-")
  ) {
    errors.push("SSH target harus berupa user@host atau alias tanpa spasi");
  }
  if (
    !Number.isInteger(connection.port) ||
    connection.port < 1 ||
    connection.port > 65535
  ) {
    errors.push("Port harus berupa angka 1–65535");
  }
  if (
    !connection.remoteHost.trim() ||
    !HOST_PATTERN.test(connection.remoteHost)
  ) {
    errors.push("Remote host harus berupa IP atau hostname sederhana");
  }
  return errors;
}

export function formatConnection(
  connection: Pick<Connection, "sshTarget" | "port" | "remoteHost">,
): string {
  return `${connection.sshTarget} · ${connection.port} → ${connection.remoteHost}`;
}
