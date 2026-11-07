package app.aret

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aret.ui.DetailScreen
import app.aret.ui.FunctionsScreen
import app.aret.ui.HomeScreen

@Composable
fun AretApp(vm: MainViewModel = viewModel()) {
    val c = MaterialTheme.colorScheme
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri)
    }
    BackHandler(enabled = vm.screen != Screen.Home) { vm.back() }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Box(
            Modifier.fillMaxWidth().height(320.dp)
                .background(Brush.verticalGradient(listOf(c.primary.copy(alpha = 0.14f), Color.Transparent))),
        )
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            if (vm.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.primary, trackColor = Color.Transparent)
            } else {
                Box(Modifier.height(4.dp))
            }
            AnimatedContent(
                targetState = vm.screen,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "screen",
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(vm) { picker.launch(arrayOf("*/*")) }
                    Screen.Functions -> FunctionsScreen(vm)
                    Screen.Detail -> DetailScreen(vm)
                }
            }
        }
    }
}
