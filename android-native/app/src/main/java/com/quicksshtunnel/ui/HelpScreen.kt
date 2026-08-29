package com.quicksshtunnel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Help & Setup",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) {
                Text("Back", color = ActiveColor, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle("Sync with Termux CLI")
        BodyText("This app shares connection data with the CLI via a single JSON file on shared storage. Both read and write the same file:")
        CodeBlock("/sdcard/quick-ssh-tunnel/connections.json")
        BodyText("To sync with Termux, create a symlink so the CLI reads the same file:")
        CodeBlock("mkdir -p ~/.config/quick-ssh-tunnel\nln -s /sdcard/quick-ssh-tunnel/connections.json \\\n  ~/.config/quick-ssh-tunnel/connections.json")
        BodyText("After that, connections created in the app appear in the CLI, and vice versa.")

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle("CLI Usage")
        BodyText("Run the CLI directly without installing:")
        CodeBlock("npx quick-ssh-tunnel")
        BodyText("Or install globally:")
        CodeBlock("npm install -g quick-ssh-tunnel\nquick-ssh-tunnel")
        BodyText("The CLI provides interactive fuzzy search, connect, disconnect, edit, clone, and delete — all synced with this app.")

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle("SSH Authentication")
        BodyText("This app supports password-based authentication. Enter the SSH password when creating or editing a connection.")
        BodyText("For key-based auth, place your private keys in the app's data directory (requires root or file import). Password auth is recommended for Android.")

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle("Data Location")
        BodyText("Connection data is stored in shared storage so both the app and Termux can access it:")
        CodeBlock("/sdcard/quick-ssh-tunnel/connections.json")
        BodyText("Format: JSON with a \"connections\" array. Same format as the CLI's ~/.config/quick-ssh-tunnel/connections.json.")

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle("Permissions")
        BodyText("The app needs storage permission to read/write the shared connections file. If you denied it on first launch, go to Settings → Apps → Quick SSH Tunnel → Permissions → Allow all files access.")

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = ActiveColor,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = TextPrimary,
        lineHeight = 20.sp,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun CodeBlock(code: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceColor)
            .padding(12.dp),
    ) {
        Text(
            text = code,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = ActiveColor,
            lineHeight = 18.sp,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}
