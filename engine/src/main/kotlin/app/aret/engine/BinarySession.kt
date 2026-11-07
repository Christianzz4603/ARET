package app.aret.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class EngineException(message: String) : Exception(message)

private fun String?.unwrap(): JSONObject {
    if (this == null) throw EngineException("native call returned null")
    val o = JSONObject(this)
    if (!o.getBoolean("ok")) throw EngineException(o.optString("error", "unknown engine error"))
    return o.getJSONObject("data")
}

/** Addresses cross the JNI boundary as "0x.." strings (unsigned 64-bit safe). */
fun parseAddress(s: String): Long = java.lang.Long.parseUnsignedLong(s.removePrefix("0x"), 16)

data class FunctionInfo(val name: String, val symbol: String?, val address: String, val source: String)

data class FunctionPage(val total: Int, val items: List<FunctionInfo>)

object Engine {
    /** Opens a binary read-only. [language] = null lets the engine detect it from the file header. */
    suspend fun open(path: String, language: String? = null): BinarySession =
        withContext(Dispatchers.Default) {
            val d = NativeEngine.nativeOpen(path, language ?: "").unwrap()
            BinarySession(d.getLong("handle"))
        }
}

class BinarySession internal constructor(private val handle: Long) : AutoCloseable {
    private suspend fun <T> native(block: () -> T): T = withContext(Dispatchers.Default) { block() }

    suspend fun info(): JSONObject = native { NativeEngine.nativeInfo(handle).unwrap() }

    suspend fun functions(offset: Int, limit: Int): FunctionPage = native {
        val d = NativeEngine.nativeFunctions(handle, offset, limit).unwrap()
        val arr = d.getJSONArray("items")
        FunctionPage(
            d.getInt("total"),
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                FunctionInfo(
                    o.getString("name"),
                    if (o.isNull("symbol")) null else o.getString("symbol"),
                    o.getString("address"),
                    o.getString("source"),
                )
            },
        )
    }

    suspend fun disassemble(address: String, count: Int): JSONObject =
        native { NativeEngine.nativeDisassemble(handle, parseAddress(address), count).unwrap() }

    suspend fun disassembleFunction(address: String): JSONObject =
        native { NativeEngine.nativeDisassembleFunction(handle, parseAddress(address)).unwrap() }

    suspend fun decompile(address: String): JSONObject =
        native { NativeEngine.nativeDecompile(handle, parseAddress(address)).unwrap() }

    suspend fun strings(offset: Int, limit: Int): JSONObject =
        native { NativeEngine.nativeStrings(handle, offset, limit).unwrap() }

    suspend fun dataSymbols(offset: Int, limit: Int): JSONObject =
        native { NativeEngine.nativeDataSymbols(handle, offset, limit).unwrap() }

    suspend fun relocations(offset: Int, limit: Int): JSONObject =
        native { NativeEngine.nativeRelocations(handle, offset, limit).unwrap() }

    suspend fun referencesFrom(address: String): JSONObject =
        native { NativeEngine.nativeReferencesFrom(handle, parseAddress(address)).unwrap() }

    suspend fun callRelations(address: String): JSONObject =
        native { NativeEngine.nativeCallRelations(handle, parseAddress(address)).unwrap() }

    override fun close() {
        NativeEngine.nativeClose(handle)
    }
}
