package app.aret

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.aret.ai.ContextBuilder
import app.aret.ai.LlmRuntime
import app.aret.ai.NoModelRuntime
import app.aret.engine.BinarySession
import app.aret.engine.Engine
import app.aret.engine.FunctionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { Home, Functions, Detail }

data class DetailState(
    val title: String,
    val decompiled: String,
    val disassembly: String,
    val aiContext: String,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    var screen by mutableStateOf(Screen.Home); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var infoText by mutableStateOf(""); private set
    var total by mutableIntStateOf(0); private set
    var detail by mutableStateOf<DetailState?>(null); private set
    val functions = mutableStateListOf<FunctionInfo>()

    /** AI is optional; until a model is bundled this honestly reports NotInstalled. */
    val runtime: LlmRuntime = NoModelRuntime()

    private var session: BinarySession? = null
    private var paging = false

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (t: Throwable) {
                error = t.message ?: t.toString()
            } finally {
                busy = false
            }
        }
    }

    fun open(uri: Uri) = launchBusy {
        // Work on a private copy: the original binary is never touched.
        val file = withContext(Dispatchers.IO) { copyToCache(uri) }
        session?.close()
        functions.clear()
        val s = Engine.open(file.absolutePath)
        session = s
        val info = s.info()
        infoText = info.getString("language") + " \u00b7 " + info.getInt("function_count") + " functions\n" +
            info.optString("description")
        val page = s.functions(0, PAGE)
        total = page.total
        functions.addAll(page.items)
        screen = Screen.Functions
    }

    fun loadMore() {
        val s = session ?: return
        if (paging || functions.size >= total) return
        paging = true
        viewModelScope.launch {
            try {
                val page = s.functions(functions.size, PAGE)
                total = page.total
                functions.addAll(page.items)
            } catch (t: Throwable) {
                error = t.message ?: t.toString()
            } finally {
                paging = false
            }
        }
    }

    fun select(f: FunctionInfo) = launchBusy {
        val s = session ?: return@launchBusy
        val ctx = ContextBuilder.forFunction(s, f.address)
        detail = DetailState(
            title = (ctx.name ?: f.name) + " @ " + f.address,
            decompiled = ctx.decompiled ?: ("Decompiler error: " + (ctx.decompileError ?: "unknown")),
            disassembly = ctx.asmLines.joinToString("\n").ifEmpty { "No disassembly available." },
            aiContext = ctx.text,
        )
        screen = Screen.Detail
    }

    fun back() {
        screen = when (screen) {
            Screen.Detail -> Screen.Functions
            else -> Screen.Home
        }
    }

    private fun copyToCache(uri: Uri): File {
        val ctx = getApplication<Application>()
        val dir = File(ctx.cacheDir, "binaries").apply { mkdirs() }
        val out = File(dir, "binary_${System.currentTimeMillis()}.bin")
        val input = requireNotNull(ctx.contentResolver.openInputStream(uri)) { "Cannot read the selected file" }
        input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
        return out
    }

    override fun onCleared() {
        session?.close()
        runtime.close()
    }

    private companion object {
        const val PAGE = 200
    }
}
