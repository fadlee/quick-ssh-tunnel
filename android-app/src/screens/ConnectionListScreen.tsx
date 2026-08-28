import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Modal,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";
import { formatSshCommand } from "@shared/core";
import type { Connection } from "@shared/types";
import { ConnectionItem } from "../components/ConnectionItem";
import { useConnections } from "../hooks/useConnections";
import { readState, removeConnection, updateState } from "../lib/store";
import {
  getRunningTunnels,
  startTunnel,
  stopTunnel,
  type Status,
} from "../lib/tunnel";

type RootStackParamList = {
  ConnectionList: undefined;
  ConnectionForm: { connection?: Connection };
};

type Props = NativeStackScreenProps<RootStackParamList, "ConnectionList">;

export function ConnectionListScreen({ navigation }: Props) {
  const { connections, loading, refresh } = useConnections();
  const [search, setSearch] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [runningIds, setRunningIds] = useState<Set<string>>(new Set());
  const [startedAtMap, setStartedAtMap] = useState<Record<string, number>>({});
  const [selected, setSelected] = useState<Connection | null>(null);

  const pollStatus = useCallback(async () => {
    try {
      const ids = await getRunningTunnels();
      setRunningIds(new Set(ids));
      const state = await readState();
      const map: Record<string, number> = {};
      for (const id of ids) {
        if (state[id]) map[id] = state[id].startedAt;
      }
      setStartedAtMap(map);
    } catch {
      // native module not ready yet
    }
  }, []);

  useEffect(() => {
    pollStatus();
    const interval = setInterval(pollStatus, 3000);
    return () => clearInterval(interval);
  }, [pollStatus]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await refresh();
    await pollStatus();
    setRefreshing(false);
  }, [refresh, pollStatus]);

  const filtered = useMemo(() => {
    const sorted = [...connections].sort((a, b) => {
      const aRunning = runningIds.has(a.id) ? 0 : 1;
      const bRunning = runningIds.has(b.id) ? 0 : 1;
      if (aRunning !== bRunning) return aRunning - bRunning;
      return b.lastUsedAt - a.lastUsedAt;
    });
    if (!search.trim()) return sorted;
    const q = search.toLowerCase();
    return sorted.filter(
      (c) =>
        c.sshTarget.toLowerCase().includes(q) ||
        String(c.port).includes(q) ||
        c.remoteHost.toLowerCase().includes(q) ||
        c.mode.includes(q),
    );
  }, [connections, search, runningIds]);

  const handleStart = useCallback(
    async (conn: Connection) => {
      try {
        await startTunnel(conn);
        await updateState((state) => {
          state[conn.id] = {
            pid: 0,
            spec: formatSshCommand(conn),
            startedAt: Date.now(),
          };
          return state;
        });
        await pollStatus();
      } catch (err) {
        console.error("Failed to start tunnel:", err);
      }
      setSelected(null);
    },
    [pollStatus],
  );

  const handleStop = useCallback(
    async (conn: Connection) => {
      try {
        await stopTunnel(conn);
        await updateState((state) => {
          delete state[conn.id];
          return state;
        });
        await pollStatus();
      } catch (err) {
        console.error("Failed to stop tunnel:", err);
      }
      setSelected(null);
    },
    [pollStatus],
  );

  const handleDelete = useCallback(
    async (conn: Connection) => {
      if (runningIds.has(conn.id)) {
        await stopTunnel(conn);
      }
      await removeConnection(conn.id);
      await refresh();
      await pollStatus();
      setSelected(null);
    },
    [refresh, pollStatus, runningIds],
  );

  const handleEdit = useCallback((conn: Connection) => {
    navigation.navigate("ConnectionForm", { connection: conn });
    setSelected(null);
  }, [navigation]);

  const handleCopyCommand = useCallback((conn: Connection) => {
    // Clipboard copy — React Native Clipboard is not in the dep list,
    // so we log it. Users can long-press to copy from the modal text.
    console.log(formatSshCommand(conn));
    setSelected(null);
  }, []);

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color="#4caf50" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TextInput
          style={styles.search}
          placeholder="Search connections…"
          placeholderTextColor="#555"
          value={search}
          onChangeText={setSearch}
        />
        <Pressable
          style={styles.newBtn}
          onPress={() => navigation.navigate("ConnectionForm", {})}
        >
          <Text style={styles.newBtnText}>+ New</Text>
        </Pressable>
      </View>

      <FlatList
        data={filtered}
        keyExtractor={(item) => item.id}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor="#4caf50"
          />
        }
        renderItem={({ item }) => (
          <ConnectionItem
            connection={item}
            status={runningIds.has(item.id) ? "running" : "stopped"}
            startedAt={startedAtMap[item.id]}
            onPress={() => setSelected(item)}
          />
        )}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyText}>No connections yet</Text>
            <Text style={styles.emptyHint}>
              Tap + New to create your first SSH tunnel
            </Text>
          </View>
        }
        contentContainerStyle={
          filtered.length === 0 ? styles.emptyList : undefined
        }
      />

      {/* Action sheet */}
      <Modal
        visible={selected !== null}
        transparent
        animationType="slide"
        onRequestClose={() => setSelected(null)}
      >
        <Pressable style={styles.overlay} onPress={() => setSelected(null)}>
          <Pressable style={styles.sheet} onPress={(e) => e.stopPropagation()}>
            {selected && (
              <>
                <Text style={styles.sheetTitle} numberOfLines={1}>
                  {selected.sshTarget}
                </Text>
                <Text style={styles.sheetSubtitle} numberOfLines={2}>
                  {formatSshCommand(selected)}
                </Text>
                <View style={styles.sheetDivider} />

                {runningIds.has(selected.id) ? (
                  <SheetButton
                    label="Stop Tunnel"
                    color="#f44336"
                    onPress={() => handleStop(selected)}
                  />
                ) : (
                  <SheetButton
                    label="Start Tunnel"
                    color="#4caf50"
                    onPress={() => handleStart(selected)}
                  />
                )}
                <SheetButton
                  label="Edit"
                  color="#2196f3"
                  onPress={() => handleEdit(selected)}
                />
                <SheetButton
                  label="Copy SSH Command"
                  color="#9e9e9e"
                  onPress={() => handleCopyCommand(selected)}
                />
                <SheetButton
                  label="Delete"
                  color="#f44336"
                  onPress={() => handleDelete(selected)}
                />
                <SheetButton
                  label="Cancel"
                  color="#888"
                  onPress={() => setSelected(null)}
                />
              </>
            )}
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

function SheetButton({
  label,
  color,
  onPress,
}: {
  label: string;
  color: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      style={({ pressed }) => [styles.sheetBtn, pressed && styles.sheetBtnPressed]}
      onPress={onPress}
    >
      <Text style={[styles.sheetBtnText, { color }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#1a1a1a",
  },
  center: {
    flex: 1,
    backgroundColor: "#1a1a1a",
    justifyContent: "center",
    alignItems: "center",
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 10,
    gap: 8,
  },
  search: {
    flex: 1,
    backgroundColor: "#252525",
    color: "#e0e0e0",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 8,
    fontSize: 14,
  },
  newBtn: {
    backgroundColor: "#4caf50",
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
  },
  newBtnText: {
    color: "#fff",
    fontWeight: "600",
    fontSize: 14,
  },
  empty: {
    alignItems: "center",
    gap: 8,
  },
  emptyText: {
    color: "#666",
    fontSize: 16,
    fontWeight: "500",
  },
  emptyHint: {
    color: "#444",
    fontSize: 13,
  },
  emptyList: {
    flex: 1,
    justifyContent: "center",
  },
  overlay: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(0,0,0,0.5)",
  },
  sheet: {
    backgroundColor: "#252525",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    padding: 20,
    paddingBottom: 34,
  },
  sheetTitle: {
    color: "#e0e0e0",
    fontSize: 18,
    fontWeight: "600",
    marginBottom: 4,
  },
  sheetSubtitle: {
    color: "#666",
    fontSize: 12,
    fontFamily: "monospace",
    marginBottom: 12,
  },
  sheetDivider: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: "#333",
    marginBottom: 8,
  },
  sheetBtn: {
    paddingVertical: 14,
    paddingHorizontal: 4,
  },
  sheetBtnPressed: {
    backgroundColor: "rgba(255,255,255,0.05)",
  },
  sheetBtnText: {
    fontSize: 16,
    fontWeight: "500",
  },
});
