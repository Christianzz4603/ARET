package app.aret.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.aret.CallRef
import app.aret.DetailState
import app.aret.MainViewModel

private fun copyTextFor(tab: Int, d: DetailState): String = when (tab) {
    0 -> d.decompiled ?: ""
    1 -> d.asm.joinToString("\n") { "${it.address}: ${it.mnemonic} ${it.operands}".trimEnd() }
    2 -> (d.callers.map { "caller ${it.name} ${it.address}" } + d.callees.map { "callee ${it.name} ${it.address}" }).joinToString("\n")
    else -> d.aiContext
}

@Composable
fun DetailScreen(vm: MainViewModel) {
    val d = vm.detail ?: return
    var tab by remember(d.address) { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = d.name, subtitle = d.address, onBack = { vm.back() }) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(copyTextFor(tab, d))) }) { Text("Copy") }
        }
        TabChips(listOf("Decompiled", "Assembly", "Calls", "AI"), tab) { tab = it }
        Spacer(Modifier.height(10.dp))
        when (tab) {
            0 -> CodeView(d)
            1 -> AsmView(d)
            2 -> CallsView(d, vm)
            else -> AiView(d)
        }
    }
}

@Composable
private fun TabChips(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        titles.forEachIndexed { i, t ->
            val bg by animateColorAsState(if (i == selected) c.primaryContainer else c.surfaceContainerHigh, label = "tab")
            val fg = if (i == selected) c.onPrimaryContainer else c.onSurfaceVariant
            Surface(onClick = { onSelect(i) }, shape = RoundedCornerShape(50), color = bg) {
                Text(t, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = fg, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CodePanel(count: Int, row: @Composable (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = c.surfaceContainerLow,
        border = BorderStroke(1.dp, c.outlineVariant),
    ) {
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            LazyColumn(Modifier.fillMaxHeight().widthIn(min = 360.dp).padding(12.dp)) {
                items(count) { i -> row(i) }
            }
        }
    }
}

@Composable
private fun MessageCard(title: String, body: String, isError: Boolean) {
    val c = MaterialTheme.colorScheme
    SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (isError) c.error else c.onSurface)
            Text(body, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }
}

@Composable
private fun CodeView(d: DetailState) {
    val c = MaterialTheme.colorScheme
    val p = rememberCodePalette()
    val text = d.decompiled
    if (text == null) {
        MessageCard("Decompiler error", d.decompileError ?: "The engine did not return pseudocode for this function.", true)
        return
    }
    val lines = remember(text, p) { text.lines().map { highlightC(it, p) } }
    CodePanel(lines.size) { i ->
        Row {
            Text((i + 1).toString().padStart(3), style = MonoStyle, color = c.outline)
            Spacer(Modifier.width(12.dp))
            Text(lines[i], style = MonoStyle, color = c.onSurface, softWrap = false)
        }
    }
}

@Composable
private fun AsmView(d: DetailState) {
    val p = rememberAsmPalette()
    if (d.asm.isEmpty()) {
        MessageCard("No disassembly", "The engine returned no instructions for this function.", false)
        return
    }
    val lines = remember(d.asm, p) { d.asm.map { highlightAsm(it, p) } }
    CodePanel(lines.size) { i -> Text(lines[i], style = MonoStyle, color = p.plain, softWrap = false) }
}

@Composable
private fun CallsView(d: DetailState, vm: MainViewModel) {
    val c = MaterialTheme.colorScheme
    if (d.callers.isEmpty() && d.callees.isEmpty()) {
        MessageCard("No direct call edges", "Only direct calls are resolved. Indirect calls and calls through pointers are not shown.", false)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        item { SectionLabel("Called by (${d.callers.size})") }
        items(d.callers) { CallRow(it) { vm.selectFunction(it.address) } }
        item { SectionLabel("Calls (${d.callees.size})") }
        items(d.callees) { CallRow(it) { vm.selectFunction(it.address) } }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun CallRow(ref: CallRef, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = c.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ref.name, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            Text(ref.address, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
    }
}

@Composable
private fun AiView(d: DetailState) {
    val c = MaterialTheme.colorScheme
    val context = remember(d.aiContext, c) {
        buildAnnotatedString {
            d.aiContext.lines().forEachIndexed { i, line ->
                if (i > 0) append('\n')
                if (line.startsWith("## ")) {
                    withStyle(SpanStyle(color = c.primary, fontWeight = FontWeight.Bold)) { append(line.removePrefix("## ").uppercase()) }
                } else append(line)
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SoftCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = c.secondary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("AI assistant", style = MaterialTheme.typography.titleMedium)
                            Pill("NOT INSTALLED", c.secondary)
                        }
                        Text(
                            "No model is bundled in this build. Below is exactly the evidence an assistant would receive for this function.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSurfaceVariant,
                        )
                        if (d.notes.isNotEmpty()) {
                            Text("Trimmed to fit: " + d.notes.joinToString("; "), style = MaterialTheme.typography.bodySmall, color = c.secondary)
                        }
                    }
                }
            }
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = c.surfaceContainerLow,
                border = BorderStroke(1.dp, c.outlineVariant),
            ) {
                Box(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
                    Text(context, style = MonoStyle, color = c.onSurface, softWrap = false)
                }
            }
        }
        OutlinedTextField(
            value = "",
            onValueChange = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Ask about this function\u2026 (needs a bundled model)") },
            shape = RoundedCornerShape(16.dp),
        )
    }
}
