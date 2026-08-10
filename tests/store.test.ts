import { afterEach, beforeEach, describe, expect, test } from "bun:test";
import fs from "fs";
import os from "os";
import path from "path";

const realHome = os.homedir;
let root = "";

function freshStore() {
  delete require.cache[require.resolve("../src/lib/store")];
  return require("../src/lib/store") as typeof import("../src/lib/store");
}

beforeEach(() => {
  root = fs.mkdtempSync(path.join(os.tmpdir(), "quick-ssh-tunnel-"));
  os.homedir = () => root;
});

afterEach(() => {
  os.homedir = realHome;
  fs.rmSync(root, { recursive: true, force: true });
});

describe("connection history", () => {
  test("keeps newest connection first and caps history at 50", () => {
    const store = freshStore();
    for (let i = 0; i < 51; i += 1) {
      store.saveConnection({
        id: String(i),
        sshTarget: `user@host-${i}`,
        port: 1000 + i,
        remoteHost: "127.0.0.1",
        compression: true,
        lastUsedAt: i,
      });
    }

    const connections = store.loadConnections();
    expect(connections).toHaveLength(50);
    expect(connections[0].id).toBe("50");
    expect(connections.at(-1).id).toBe("1");
  });
});
