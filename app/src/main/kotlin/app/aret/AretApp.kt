package app.aret

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AretApp(vm: MainViewModel = viewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri)
    }
    BackHandler(enabled = vm.screen != Screen.Home) { vm.back() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (vm.screen) {
                            Screen.Home -> "ARET"
                            Screen.Functions -> "Functions"
                            Screen.Detail -> vm.detail?.title ?: "Function"
                        },
                        maxLines = 1,
                    )
                },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (vm.screen) {
                Screen.Home -> Home(vm) { picker.launch(arrayOf("*/*")) }
                Screen.Functions -> Functions(vm)
                Screen.Detail -> Detail(vm)
            }
            if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Home(vm: MainViewModel, onOpen: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AI Reverse Engineering Studio", style = MaterialTheme.typography.headlineSmall)
        Text("Open an ELF, PE or Mach-O binary. Analysis runs locally with the Rust engine.")
        Button(onClick = onOpen) { Text("Open binary") }
        vm.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        Text("AI assistant: no model installed in this build (optional feature, not built yet).")
    }
}

@Composable
private fun Functions(vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text(vm.infoText, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        vm.error?.let { Text("Error: $it", Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.error) }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(vm.functions) { index, f ->
                if (index == vm.functions.size - 1) LaunchedEffect(index) { vm.loadMore() }
                Column(Modifier.fillMaxWidth().clickable { vm.select(f) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(f.name, fontFamily = FontFamily.Monospace)
                    Text("${f.address} \u00b7 ${f.source}", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Detail(vm: MainViewModel) {
    val d = vm.detail ?: return
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Decompiled", "Disassembly", "AI context")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        val body = when (tab) {
            0 -> d.decompiled
            1 -> d.disassembly
            else -> "This is exactly the evidence an AI model would receive (no model is installed yet).\n\n" + d.aiContext
        }
        SelectionContainer {
            Text(
                body,
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
    }
}
