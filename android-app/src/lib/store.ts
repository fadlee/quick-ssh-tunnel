import AsyncStorage from "@react-native-async-storage/async-storage";
import { connectionKey } from "@shared/core";
import type { Connection } from "@shared/types";

const CONNECTIONS_KEY = "connections";
const STATE_KEY = "state";

export const MAX_HISTORY = 50;

export type StateEntry = {
  pid: number;
  spec: string;
  startedAt: number;
};

/** Generate a RFC-4122 v4 UUID (no crypto.randomUUID in RN hermes). */
export function newId(): string {
  const hex = "0123456789abcdef";
  let uuid = "";
  for (let i = 0; i < 36; i++) {
    if (i === 8 || i === 13 || i === 18 || i === 23) {
      uuid += "-";
    } else if (i === 14) {
      uuid += "4";
    } else if (i === 19) {
      uuid += hex[(Math.random() * 4) | (0 + 8)];
    } else {
      uuid += hex[(Math.random() * 16) | 0];
    }
  }
  return uuid;
}

export function cloneConnection(connection: Connection): Connection {
  return { ...connection, id: newId(), lastUsedAt: Date.now() };
}

export async function loadConnections(): Promise<Connection[]> {
  try {
    const raw = await AsyncStorage.getItem(CONNECTIONS_KEY);
    if (!raw) return [];
    const data = JSON.parse(raw);
    return Array.isArray(data.connections)
      ? (data.connections as Connection[])
      : [];
  } catch {
    return [];
  }
}

async function saveConnections(connections: Connection[]): Promise<void> {
  await AsyncStorage.setItem(
    CONNECTIONS_KEY,
    JSON.stringify({ connections }, null, 2),
  );
}

export async function saveConnection(
  connection: Connection,
): Promise<Connection[]> {
  const key = connectionKey(connection);
  const connections = (await loadConnections()).filter(
    (item) => item.id !== connection.id && connectionKey(item) !== key,
  );
  connections.unshift(connection);
  const updated = connections.slice(0, MAX_HISTORY);
  await saveConnections(updated);
  return updated;
}

export async function findConnectionByKey(
  key: string,
): Promise<Connection | undefined> {
  const connections = await loadConnections();
  return connections.find(
    (connection) => connectionKey(connection) === key,
  );
}

export async function removeConnection(id: string): Promise<Connection[]> {
  const connections = (await loadConnections()).filter(
    (connection) => connection.id !== id,
  );
  await saveConnections(connections);
  return connections;
}

export async function readState(): Promise<Record<string, StateEntry>> {
  try {
    const raw = await AsyncStorage.getItem(STATE_KEY);
    if (!raw) return {};
    return JSON.parse(raw) as Record<string, StateEntry>;
  } catch {
    return {};
  }
}

export async function writeState(
  state: Record<string, StateEntry>,
): Promise<void> {
  await AsyncStorage.setItem(STATE_KEY, JSON.stringify(state, null, 2));
}

export async function updateState<T>(
  fn: (state: Record<string, StateEntry>) => T,
): Promise<T> {
  const state = await readState();
  const result = fn(state);
  await writeState(state);
  return result;
}
