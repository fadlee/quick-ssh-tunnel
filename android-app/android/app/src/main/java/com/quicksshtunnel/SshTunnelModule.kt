package com.quicksshtunnel

import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import java.util.concurrent.ConcurrentHashMap

@ReactModule(name = SshTunnelModule.NAME)
class SshTunnelModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "SshTunnelModule"
        private const val TAG = "SshTunnelModule"
    }

    /** Active JSch sessions keyed by connectionId. */
    private val sessions = ConcurrentHashMap<String, Session>()

    override fun getName(): String = NAME

    @ReactMethod
    fun startTunnel(
        connectionId: String,
        sshTarget: String,
        mode: String,
        port: Int,
        remoteHost: String,
        compression: Boolean,
        promise: Promise,
    ) {
        try {
            if (sessions.containsKey(connectionId)) {
                promise.reject("ALREADY_RUNNING", "Tunnel is already running for $connectionId")
                return
            }

            // Parse user@host
            val (user, host) = parseSshTarget(sshTarget)

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
            if (compression) {
                session.setConfig("compression", "zlib@openssh.com,zlib,none")
            }

            // UserInfo for password prompt (not yet implemented — key-based only for now)
            session.userInfo = object : UserInfo {
                override fun getPassphrase(): String? = null
                override fun getPassword(): String? = null
                override fun promptPassword(message: String?): Boolean = false
                override fun promptPassphrase(message: String?): Boolean = false
                override fun promptYesNo(message: String?): Boolean = true
                override fun showMessage(message: String?) {}
            }

            session.connect()

            when (mode) {
                "socks5" -> {
                    // Dynamic port forwarding (-D) is not supported by JSch 0.1.55.
                    // JSch lacks a built-in SOCKS proxy server implementation.
                    promise.reject("SOCKS5_NOT_SUPPORTED", "Dynamic port forwarding (SOCKS5) is not supported by JSch. Use local port forwarding (-L) instead.")
                    session.disconnect()
                    return
                }
                else -> {
                    // Local port forwarding (-L port:remoteHost:port)
                    session.setPortForwardingL(port, remoteHost, port)
                    Log.i(TAG, "Local forward :$port -> $remoteHost:$port via $user@$actualHost")
                }
            }

            sessions[connectionId] = session

            // Start foreground service to keep tunnels alive in background
            TunnelService.start(reactContext, sessions.size)

            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tunnel", e)
            promise.reject("START_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun stopTunnel(connectionId: String, promise: Promise) {
        try {
            val session = sessions.remove(connectionId)
            if (session != null) {
                session.disconnect()
                Log.i(TAG, "Tunnel stopped for $connectionId")
            }

            if (sessions.isEmpty()) {
                TunnelService.stop(reactContext)
            } else {
                TunnelService.updateNotification(reactContext, sessions.size)
            }

            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop tunnel", e)
            promise.reject("STOP_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun getStatus(connectionId: String, promise: Promise) {
        val session = sessions[connectionId]
        val status = if (session != null && session.isConnected) "running" else "stopped"
        promise.resolve(status)
    }

    @ReactMethod
    fun getRunningTunnels(promise: Promise) {
        val ids = Arguments.fromList(ArrayList(sessions.keys))
        promise.resolve(ids)
    }

    private fun parseSshTarget(target: String): Pair<String, String> {
        val parts = target.split("@", limit = 2)
        return if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            // No user specified — use current OS user or "root"
            val user = System.getProperty("user.name") ?: "root"
            user to target
        }
    }

    private fun parseHostPort(host: String): Pair<String, Int> {
        val parts = host.split(":", limit = 2)
        return if (parts.size == 2) {
            parts[0] to (parts[1].toIntOrNull() ?: 22)
        } else {
            host to 22
        }
    }
}
