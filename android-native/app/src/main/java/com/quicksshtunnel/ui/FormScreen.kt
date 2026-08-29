package com.quicksshtunnel.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicksshtunnel.Connection
import com.quicksshtunnel.ConnectionMode
import com.quicksshtunnel.ConnectionStore
import com.quicksshtunnel.SshTunnelManager
import com.quicksshtunnel.TunnelService
import com.quicksshtunnel.formatSshCommand
import com.quicksshtunnel.validateConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FormScreen(
    connectionId: String?,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load existing connection if editing
    val existing = remember(connectionId) {
        if (connectionId != null) {
            ConnectionStore.loadConnections(context).firstOrNull { it.id == connectionId }
        } else {
            null
        }
    }

    var mode by remember { mutableStateOf(existing?.mode ?: ConnectionMode.FORWARD) }
    var sshTarget by remember { mutableStateOf(existing?.sshTarget ?: "") }
    var portText by remember { mutableStateOf((existing?.port ?: 8080).toString()) }
    var remoteHost by remember { mutableStateOf(existing?.remoteHost ?: "127.0.0.1") }
    var compression by remember { mutableStateOf(existing?.compression ?: false) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var connecting by remember { mutableStateOf(false) }

    // Save & connect — declared before UI so it's visible in click handlers.
    // Uses plain `return` (local function, not lambda).
    fun onSaveAndConnect() {
        val port = portText.toIntOrNull() ?: 0
        val conn = Connection(
            id = existing?.id ?: ConnectionStore.newId(),
            mode = mode,
            sshTarget = sshTarget.trim(),
            port = port,
            remoteHost = if (mode == ConnectionMode.SOCKS5) "127.0.0.1" else remoteHost.trim(),
            compression = compression,
            lastUsedAt = System.currentTimeMillis(),
        )

        val validationErrors = validateConnection(conn)
        if (validationErrors.isNotEmpty()) {
            errors = validationErrors
            return
        }

        // Port conflict check — is another running tunnel using the same port?
        val allConnections = ConnectionStore.loadConnections(context)
        val conflict = allConnections.any { other ->
            other.id != conn.id &&
                other.port == port &&
                SshTunnelManager.isRunning(other.id)
        }
        if (conflict) {
            errors = listOf("Port $port is already in use by a running tunnel")
            return
        }

        errors = emptyList()
        connecting = true

        scope.launch {
            // Save first
            val saved = ConnectionStore.saveConnection(context, conn)

            // Start tunnel on IO thread
            val result = withContext(Dispatchers.IO) {
                SshTunnelManager.startTunnel(saved)
            }

            connecting = false

            if (result.isSuccess) {
                TunnelService.start(context, SshTunnelManager.activeCount())
                onSaved()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to start tunnel"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                // Still navigate back — connection was saved
                onSaved()
            }
        }
    }

    // Build a preview connection (with placeholder id/lastUsedAt)
    val previewConn = Connection(
        id = "preview",
        mode = mode,
        sshTarget = sshTarget.ifBlank { "user@host" },
        port = portText.toIntOrNull() ?: 0,
        remoteHost = if (mode == ConnectionMode.SOCKS5) "127.0.0.1" else remoteHost.ifBlank { "127.0.0.1" },
        compression = compression,
        lastUsedAt = 0,
    )
    val commandPreview = formatSshCommand(previewConn)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // ── Title ───────────────────────────────────────────────────────
        Text(
            text = if (existing != null) "Edit Connection" else "New Connection",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(20.dp))

        // ── Mode selector ───────────────────────────────────────────────
        Text(
            text = "Mode",
            fontSize = 13.sp,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeButton(
                text = "Local Forward (-L)",
                selected = mode == ConnectionMode.FORWARD,
                modifier = Modifier.weight(1f),
            ) { mode = ConnectionMode.FORWARD }
            ModeButton(
                text = "SOCKS5 (-D)",
                selected = mode == ConnectionMode.SOCKS5,
                modifier = Modifier.weight(1f),
            ) { mode = ConnectionMode.SOCKS5 }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── SSH Target ──────────────────────────────────────────────────
        FormLabel("SSH Target")
        OutlinedTextField(
            value = sshTarget,
            onValueChange = { sshTarget = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("user@host or alias", color = TextSecondary, fontSize = 14.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Local Port ──────────────────────────────────────────────────
        FormLabel("Local Port")
        OutlinedTextField(
            value = portText,
            onValueChange = { newValue ->
                portText = newValue.filter { it.isDigit() }.take(5)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Remote Host (hidden for SOCKS5) ─────────────────────────────
        if (mode == ConnectionMode.FORWARD) {
            FormLabel("Remote Host")
            OutlinedTextField(
                value = remoteHost,
                onValueChange = { remoteHost = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("127.0.0.1", color = TextSecondary, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Compression toggle ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Compression",
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = compression,
                onCheckedChange = { compression = it },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Command preview ─────────────────────────────────────────────
        FormLabel("Command Preview")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor)
                .padding(14.dp),
        ) {
            Text(
                text = commandPreview,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Error messages ──────────────────────────────────────────────
        if (errors.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorColor.copy(alpha = 0.12f))
                    .padding(14.dp),
            ) {
                Column {
                    errors.forEach { err ->
                        Text(
                            text = "• $err",
                            fontSize = 13.sp,
                            color = ErrorColor,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Buttons ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel", fontSize = 15.sp, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (connecting) ActiveColor.copy(alpha = 0.4f) else ActiveColor)
                    .clickable(enabled = !connecting) { onSaveAndConnect() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (connecting) "Connecting…" else "Save & Connect",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ActiveColor.copy(alpha = 0.15f) else SurfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) ActiveColor else TextSecondary,
        )
    }
}
