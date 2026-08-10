import { describe, expect, test } from "bun:test";
import {
  buildArgs,
  connectionKey,
  formatConnection,
  validateConnection,
} from "../src/lib/core";
import type { Connection } from "../src/lib/store";

const base: Connection = {
  id: "one",
  sshTarget: "dev@example.com",
  port: 5432,
  remoteHost: "127.0.0.1",
  compression: true,
  lastUsedAt: 1,
};

describe("Quick SSH connection", () => {
  test("builds local forwarding args with compression by default", () => {
    expect(buildArgs(base)).toEqual([
      "-N",
      "-L",
      "5432:127.0.0.1:5432",
      "-C",
      "-o",
      "BatchMode=yes",
      "-o",
      "ExitOnForwardFailure=yes",
      "-o",
      "ServerAliveInterval=30",
      "-o",
      "ServerAliveCountMax=3",
      "dev@example.com",
    ]);
  });

  test("deduplicates by every connection parameter", () => {
    expect(connectionKey(base)).toBe(connectionKey({ ...base, id: "two" }));
    expect(connectionKey(base)).not.toBe(
      connectionKey({ ...base, compression: false }),
    );
  });

  test("validates target, port, and remote host", () => {
    expect(validateConnection({ ...base, sshTarget: "" }).join(" ")).toContain(
      "SSH target",
    );
    expect(validateConnection({ ...base, port: 0 }).join(" ")).toContain("Port");
    expect(validateConnection({ ...base, remoteHost: "bad host" }).join(" ")).toContain(
      "Remote host",
    );
    expect(validateConnection(base)).toEqual([]);
  });

  test("formats an automatic label", () => {
    expect(formatConnection(base)).toBe(
      "dev@example.com · 5432 → 127.0.0.1",
    );
  });
});
