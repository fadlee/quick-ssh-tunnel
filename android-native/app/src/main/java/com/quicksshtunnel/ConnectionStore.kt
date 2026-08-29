package com.quicksshtunnel

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Connection storage using a shared-storage JSON file.
 *
 * Reads/writes /sdcard/quick-ssh-tunnel/connections.json in the same format
 * as the CLI: {"connections": [...]}. Both the Android app and the Termux CLI
 * access this same file. User symlinks in Termux:
 *   ln -s /sdcard/quick-ssh-tunnel/connections.json ~/.config/quick-ssh-tunnel/connections.json
 */
object ConnectionStore {

    private const val TAG = "ConnectionStore"
    private const val MAX_HISTORY = 50

    // Shared storage path — accessible by both Android app and Termux CLI
    private const val SHARED_DIR = "/sdcard/quick-ssh-tunnel"
    private const val SHARED_FILE = "$SHARED_DIR/connections.json"

    private fun dataFile(): File {
        val dir = File(SHARED_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(SHARED_FILE)
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    /** Load all stored connections, newest first by lastUsedAt. */
    fun loadConnections(context: Context): List<Connection> {
        val file = dataFile()
        if (!file.exists()) return emptyList()
        return try {
            val raw = file.readText()
            val obj = JSONObject(raw)
            val arr = obj.getJSONArray("connections")
            (0 until arr.length()).mapNotNull { i ->
                try {
                    arr.getJSONObject(i).toConnection()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse connection at index $i", e)
                    null
                }
            }.sortedByDescending { it.lastUsedAt }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load connections", e)
            emptyList()
        }
    }

    // ── Write ────────────────────────────────────────────────────────────────

    /** Add or update a connection (dedup via connectionKey), update lastUsedAt. */
    fun saveConnection(context: Context, conn: Connection): Connection {
        val existing = loadConnections(context).toMutableList()
        val updated = conn.copy(lastUsedAt = System.currentTimeMillis())

        // Find existing by id first, then by connectionKey for dedup
        val idxById = existing.indexOfFirst { it.id == updated.id }
        if (idxById >= 0) {
            existing[idxById] = updated
        } else {
            // Dedup by connectionKey — replace if same key exists
            val idxByKey = existing.indexOfFirst { connectionKey(it) == connectionKey(updated) }
            if (idxByKey >= 0) {
                existing[idxByKey] = updated
            } else {
                existing.add(0, updated)
            }
        }

        // Enforce MAX_HISTORY — drop oldest by lastUsedAt
        val trimmed = existing
            .sortedByDescending { it.lastUsedAt }
            .take(MAX_HISTORY)

        saveConnections(context, trimmed)
        return updated
    }

    /** Delete a connection by id. */
    fun removeConnection(context: Context, id: String) {
        val existing = loadConnections(context).filterNot { it.id == id }
        saveConnections(context, existing)
    }

    /** Find a connection whose connectionKey matches. */
    fun findConnectionByKey(context: Context, key: String): Connection? {
        return loadConnections(context).firstOrNull { connectionKey(it) == key }
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

    private fun saveConnections(context: Context, connections: List<Connection>) {
        val file = dataFile()
        val arr = JSONArray()
        connections.forEach { arr.put(it.toJSON()) }
        val obj = JSONObject().apply { put("connections", arr) }
        file.writeText(obj.toString(2))
    }

    private fun JSONObject.toConnection(): Connection {
        return Connection(
            id = getString("id"),
            mode = parseMode(getString("mode")),
            sshTarget = getString("sshTarget"),
            port = getInt("port"),
            remoteHost = getString("remoteHost"),
            compression = getBoolean("compression"),
            lastUsedAt = getLong("lastUsedAt"),
            password = optString("password").takeIf { it.isNotEmpty() },
        )
    }

    private fun Connection.toJSON(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            // Use lowercase string to match CLI format
            put("mode", when (mode) {
                ConnectionMode.FORWARD -> "forward"
                ConnectionMode.SOCKS5 -> "socks5"
            })
            put("sshTarget", sshTarget)
            put("port", port)
            put("remoteHost", remoteHost)
            put("compression", compression)
            put("lastUsedAt", lastUsedAt)
            if (!password.isNullOrEmpty()) {
                put("password", password)
            }
        }
    }

    private fun parseMode(s: String): ConnectionMode {
        return when (s.lowercase()) {
            "socks5", "socks" -> ConnectionMode.SOCKS5
            else -> ConnectionMode.FORWARD
        }
    }

    /** Generate a new unique connection ID. */
    fun newId(): String = UUID.randomUUID().toString()
}
