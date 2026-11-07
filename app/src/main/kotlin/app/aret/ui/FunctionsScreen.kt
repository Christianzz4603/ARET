package app.aret.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aret.MainViewModel

@Composable
private fun sourceBadge(source: String): Pair<String, Color> {
    val c = MaterialTheme.colorScheme
    return when (source) {
        "SymbolTable" -> "SYM" to c.primary
        "DynamicSymbol" -> "DYN" to c.tertiary
        "ImportStub" -> "IMP" to c.secondary
        "Discovered" -> "AUTO" to c.onSurfaceVariant
        else -> source.take(4).uppercase() to c.onSurfaceVariant
    }
}

@Composable
fun FunctionsScreen(vm: MainViewModel) {
    val c = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val searching = query.isNotBlank()
    LaunchedEffect(searching) { if (searching) vm.loadAll() }

    val shown = remember(query, vm.functions.size) {
        if (!searching) vm.functions.toList()
        else {
            val q = query.trim().lowercase()
            vm.functions.filter { it.name.lowercase().contains(q) || it.address.lowercase().contains(q) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Functions", subtitle = "${vm.total} found", onBack = { vm.back() })

        SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(vm.language.substringBefore(':').ifEmpty { "binary" }, c.primary)
                    Pill("${vm.total} functions", c.tertiary)
                }
                if (vm.description.isNotBlank()) {
                    Text(vm.description, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search name or address") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searching) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surfaceContainerHigh,
                unfocusedContainerColor = c.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.height(8.dp))

        if (searching && shown.isEmpty()) {
            Text(
                if (vm.functions.size < vm.total) "Searching\u2026 ${vm.functions.size} of ${vm.total} loaded" else "No matching functions",
                Modifier.padding(24.dp),
                color = c.onSurfaceVariant,
            )
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
            itemsIndexed(shown) { index, f ->
                if (!searching && index == shown.size - 1) LaunchedEffect(index) { vm.loadMore() }
                val (label, tint) = sourceBadge(f.source)
                Surface(
                    onClick = { vm.selectFunction(f.address) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = c.surfaceContainerLow,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(f.name, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(f.address, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        }
                        Pill(label, tint)
                    }
                }
            }
        }
    }
}
