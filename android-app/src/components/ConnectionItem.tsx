import React from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { formatConnection } from "@shared/core";
import type { Connection } from "@shared/types";
import type { Status } from "../lib/tunnel";
import { StatusBadge } from "./StatusBadge";

type Props = {
  connection: Connection;
  status: Status;
  startedAt?: number;
  onPress: () => void;
};

function formatUptime(startedAt: number): string {
  const seconds = Math.floor((Date.now() - startedAt) / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ${seconds % 60}s`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ${minutes % 60}m`;
}

export function ConnectionItem({
  connection,
  status,
  startedAt,
  onPress,
}: Props) {
  const isRunning = status === "running";
  return (
    <Pressable
      style={({ pressed }) => [
        styles.container,
        pressed && styles.pressed,
      ]}
      onPress={onPress}
      android_ripple={{ color: "rgba(255,255,255,0.05)" }}
    >
      <View style={styles.left}>
        <View
          style={[
            styles.indicator,
            isRunning ? styles.indicatorOn : styles.indicatorOff,
          ]}
        />
        <View style={styles.info}>
          <Text style={styles.title} numberOfLines={1}>
            {formatConnection(connection)}
          </Text>
          <Text style={styles.subtitle} numberOfLines={1}>
            {connection.mode === "socks5"
              ? `SOCKS5 · :${connection.port}`
              : `Local Forward · :${connection.port} → ${connection.remoteHost}`}
            {connection.compression ? " · -C" : ""}
          </Text>
          {isRunning && startedAt ? (
            <Text style={styles.uptime}>↑ {formatUptime(startedAt)}</Text>
          ) : null}
        </View>
      </View>
      <StatusBadge status={status} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#2a2a2a",
  },
  pressed: {
    backgroundColor: "rgba(255,255,255,0.03)",
  },
  left: {
    flexDirection: "row",
    alignItems: "center",
    flex: 1,
    gap: 12,
  },
  indicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  indicatorOn: {
    backgroundColor: "#4caf50",
  },
  indicatorOff: {
    backgroundColor: "#444",
  },
  info: {
    flex: 1,
    gap: 2,
  },
  title: {
    color: "#e0e0e0",
    fontSize: 15,
    fontWeight: "500",
  },
  subtitle: {
    color: "#777",
    fontSize: 12,
  },
  uptime: {
    color: "#4caf50",
    fontSize: 11,
    marginTop: 2,
  },
});
