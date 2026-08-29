package com.quicksshtunnel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lightweight connection storage using SharedPreferences + org.json.
 * Same JSON format as the CLI's connections.json.
 */
object ConnectionStore {

    private const val PREFS_NAME = "quick_ssh_tunnel"
    private const val KEY_CONNECTIONS = "connections"
    private const val MAX_HISTORY = 50

    // ── Read ─────────────────────────────────────────────────────────────────

    /** Load all stored connections, newest first by lastUsedAt. */
    fun loadConnections(context: Context): List<Connection> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CONNECTIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                try {
                    arr.getJSONObject(i).toConnection()
                } catch (_: Exception) {
                    null
                }
            }.sortedByDescending { it.lastUsedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Write ────────────────────────────────────────────────────────────────

    /** Add or update a connection (dedup via connectionKey), update lastUsedAt. */
    fun saveConnection(context: Context, conn: Connection): Connection {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = loadConnections(context).toMutableList()

        // Update lastUsedAt
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

        prefs.edit().putString(KEY_CONNECTIONS, trimmed.toJSON().toString()).apply()
        return updated
    }

    /** Delete a connection by id. */
    fun removeConnection(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = loadConnections(context).filterNot { it.id == id }
        prefs.edit().putString(KEY_CONNECTIONS, existing.toJSON().toString()).apply()
    }

    /** Find a connection whose connectionKey matches. */
    fun findConnectionByKey(context: Context, key: String): Connection? {
        return loadConnections(context).firstOrNull { connectionKey(it) == key }
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

    private fun JSONObject.toConnection(): Connection {
        return Connection(
            id = getString("id"),
            mode = ConnectionMode.valueOf(getString("mode")),
            sshTarget = getString("sshTarget"),
            port = getInt("port"),
            remoteHost = getString("remoteHost"),
            compression = getBoolean("compression"),
            lastUsedAt = getLong("lastUsedAt"),
        )
    }

    private fun Connection.toJSON(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("mode", mode.name)
            put("sshTarget", sshTarget)
            put("port", port)
            put("remoteHost", remoteHost)
            put("compression", compression)
            put("lastUsedAt", lastUsedAt)
        }
    }

    private fun List<Connection>.toJSON(): JSONArray {
        return JSONArray().apply {
            forEach { put(it.toJSON()) }
        }
    }

    /** Generate a new unique connection ID. */
    fun newId(): String = UUID.randomUUID().toString()
}
