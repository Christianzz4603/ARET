package app.aret.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aret.MainViewModel

@Composable
fun HomeScreen(vm: MainViewModel, onOpen: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        AretLogo(68.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ARET", style = MaterialTheme.typography.headlineLarge)
            Text("AI Reverse Engineering Studio", style = MaterialTheme.typography.titleMedium, color = c.primary)
        }
        Text(
            "Disassemble, decompile and understand native code \u2014 entirely on your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
        )

        Button(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open binary", fontWeight = FontWeight.SemiBold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("ELF", c.primary); Pill("PE", c.primary); Pill("Mach-O", c.primary)
            Pill("ARM64", c.tertiary); Pill("ARM", c.tertiary); Pill("x86", c.tertiary)
        }

        vm.error?.let { msg ->
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = c.error)
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
        }

        FeatureCard(Icons.Default.Build, "Real decompiler", "Ghidra-grade pseudocode from the Rust engine, running on-device.", c.primary)
        FeatureCard(Icons.Default.Lock, "Private by design", "Binaries are analyzed from a private copy and never leave your phone.", c.tertiary)
        FeatureCard(
            Icons.Default.Star,
            "Offline AI assistant",
            "Not installed in this build. When added, it will explain functions using only the engine's evidence.",
            c.secondary,
            badge = "COMING",
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, text: String, tint: Color, badge: String? = null) {
    val c = MaterialTheme.colorScheme
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = tint) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (badge != null) Pill(badge, tint)
                }
                Text(text, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
}
