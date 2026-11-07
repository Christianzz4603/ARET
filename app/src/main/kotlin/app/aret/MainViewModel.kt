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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class Screen { Home, Functions, Detail }

data class AsmLine(val address: String, val bytes: String, val mnemonic: String, val operands: String)

data class CallRef(val name: String, val address: String)

data class DetailState(
    val name: String,
    val address: String,
    val decompiled: String?,
    val decompileError: String?,
    val asm: List<AsmLine>,
    val callers: List<CallRef>,
    val callees: List<CallRef>,
    val aiContext: String,
    val notes: List<String>,
)

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    var screen by mutableStateOf(Screen.Home); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var language by mutableStateOf(""); private set
    var description by mutableStateOf(""); private set
    var total by mutableIntStateOf(0); private set
    var detail by mutableStateOf<DetailState?>(null); private set
    val functions = mutableStateListOf<FunctionInfo>()

    /** AI is optional; until a model is bundled this honestly reports NotInstalled. */
    val runtime: LlmRuntime = NoModelRuntime()

    private var session: BinarySession? = null
    private var paging = false
    private val stack = mutableListOf<DetailState>()

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
        stack.clear()
        val s = Engine.open(file.absolutePath)
        session = s
        val info = s.info()
        language = info.getString("language")
        description = info.optString("description")
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

    /** Loads every remaining page so search can cover the whole binary. */
    fun loadAll() {
        val s = session ?: return
        if (paging || functions.size >= total) return
        paging = true
        viewModelScope.launch {
            try {
                while (functions.size < total) {
                    val page = s.functions(functions.size, PAGE)
                    if (page.items.isEmpty()) break
                    total = page.total
                    functions.addAll(page.items)
                }
            } catch (t: Throwable) {
                error = t.message ?: t.toString()
            } finally {
                paging = false
            }
        }
    }

    fun selectFunction(address: String) = launchBusy {
        val s = session ?: return@launchBusy
        val ctx = ContextBuilder.forFunction(s, address)
        val dis = runCatching { s.disassembleFunction(address) }.getOrNull()
        val rel = runCatching { s.callRelations(address) }.getOrNull()

        val asm = dis?.optJSONArray("instructions")?.objects()?.map {
            AsmLine(it.getString("address"), it.getString("bytes"), it.getString("mnemonic"), it.getString("operands"))
        } ?: emptyList()

        fun refs(key: String): List<CallRef> = rel?.optJSONArray(key)?.objects()?.map {
            val a = it.getString("address")
            CallRef(if (it.isNull("name")) "sub_" + a.removePrefix("0x") else it.getString("name"), a)
        } ?: emptyList()

        if (screen == Screen.Detail) detail?.let { stack.add(it) } else stack.clear()
        detail = DetailState(
            name = ctx.name ?: ("sub_" + address.removePrefix("0x")),
            address = address,
            decompiled = ctx.decompiled,
            decompileError = ctx.decompileError,
            asm = asm,
            callers = refs("callers"),
            callees = refs("callees"),
            aiContext = ctx.text,
            notes = ctx.truncated,
        )
        screen = Screen.Detail
    }

    fun back() {
        when (screen) {
            Screen.Detail -> if (stack.isNotEmpty()) detail = stack.removeAt(stack.lastIndex) else screen = Screen.Functions
            Screen.Functions -> screen = Screen.Home
            Screen.Home -> Unit
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
