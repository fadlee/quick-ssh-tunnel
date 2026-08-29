package com.quicksshtunnel

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages JSch SSH sessions for local port forwarding.
 * Singleton — holds active sessions keyed by connectionId.
 */
object SshTunnelManager {

    private const val TAG = "SshTunnelManager"

    /** Active JSch sessions keyed by connectionId. */
    private val sessions = ConcurrentHashMap<String, Session>()

    /** Start a tunnel for the given connection. Blocks until connected or fails. */
    fun startTunnel(connection: Connection): Result<Unit> {
        return try {
            if (sessions.containsKey(connection.id)) {
                return Result.failure(IllegalStateException("Tunnel is already running for ${connection.id}"))
            }

            val (user, host) = parseSshTarget(connection.sshTarget)
            val jsch = JSch()

            // Try to load default SSH keys (~/.ssh/id_rsa, id_ed25519, etc.)
            val home = System.getProperty("user.home") ?: "/data/data/com.quicksshtunnel/files"
            val sshDir = "$home/.ssh"
            listOf("id_rsa", "id_ed25519", "id_ecdsa", "id_dsa").forEach { name ->
                val keyPath = "$sshDir/$name"
                try {
                    jsch.addIdentity(keyPath)
                    Log.i(TAG, "Loaded identity: $keyPath")
                } catch (_: Exception) {
                    // key may not exist — skip
                }
            }

            // Default SSH port is 22; allow host:port syntax
            val (actualHost, sshPort) = parseHostPort(host)

            val session = jsch.getSession(user, actualHost, sshPort)
            session.setConfig("StrictHostKeyChecking", "no")
            if (connection.compression) {
                session.setConfig("compression", "zlib@openssh.com,zlib,none")
            }

            // UserInfo — key-based auth only; password prompts not implemented.
            session.userInfo = object : UserInfo {
                override fun getPassphrase(): String? = null
                override fun getPassword(): String? = null
                override fun promptPassword(message: String?): Boolean = false
                override fun promptPassphrase(message: String?): Boolean = false
                override fun promptYesNo(message: String?): Boolean = true
                override fun showMessage(message: String?) {}
            }

            session.connect()

            when (connection.mode) {
                ConnectionMode.SOCKS5 -> {
                    // JSch 0.1.55 doesn't support dynamic forwarding (-D).
                    session.disconnect()
                    return Result.failure(
                        Exception("SOCKS5 dynamic forwarding is not supported by JSch. Use local forward (-L) instead.")
                    )
                }
                ConnectionMode.FORWARD -> {
                    session.setPortForwardingL(connection.port, connection.remoteHost, connection.port)
                    Log.i(TAG, "Local forward :${connection.port} -> ${connection.remoteHost}:${connection.port} via $user@$actualHost")
                }
            }

            sessions[connection.id] = session
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tunnel", e)
            Result.failure(e)
        }
    }

    /** Stop and remove the tunnel for the given connectionId. */
    fun stopTunnel(connectionId: String) {
        val session = sessions.remove(connectionId)
        if (session != null) {
            session.disconnect()
            Log.i(TAG, "Tunnel stopped for $connectionId")
        }
    }

    /** Whether a tunnel is currently connected for the given connectionId. */
    fun isRunning(connectionId: String): Boolean =
        sessions[connectionId]?.isConnected == true

    /** Set of connectionIds with active (connected) tunnels. */
    fun getRunningIds(): Set<String> =
        sessions.filter { it.value.isConnected }.keys

    /** Number of active tunnels. */
    fun activeCount(): Int = sessions.count { it.value.isConnected }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Parse "user@host" → (user, host). Falls back to OS user or "root". */
    private fun parseSshTarget(target: String): Pair<String, String> {
        val parts = target.split("@", limit = 2)
        return if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            val user = System.getProperty("user.name") ?: "root"
            user to target
        }
    }

    /** Parse "host:port" → (host, port). Defaults port to 22. */
    private fun parseHostPort(host: String): Pair<String, Int> {
        val parts = host.split(":", limit = 2)
        return if (parts.size == 2) {
            parts[0] to (parts[1].toIntOrNull() ?: 22)
        } else {
            host to 22
        }
    }
}
