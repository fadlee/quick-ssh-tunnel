export type Connection = {
  id: string;
  mode: "forward" | "socks5";
  sshTarget: string;
  port: number;
  remoteHost: string;
  compression: boolean;
  lastUsedAt: number;
};
