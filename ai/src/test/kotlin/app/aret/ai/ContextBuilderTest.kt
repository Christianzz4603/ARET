package app.aret.ai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBuilderTest {
    private val info = JSONObject().put("language", "AARCH64:LE:64:v8A:default")

    private fun dis(n: Int): JSONObject {
        val arr = JSONArray()
        for (i in 0 until n) {
            arr.put(JSONObject().put("address", "0x${1000 + i * 4}").put("mnemonic", "mov").put("operands", "x0, x1"))
        }
        return JSONObject().put("instructions", arr).put("gaps", JSONArray())
    }

    private fun rel() = JSONObject().put("callers", JSONArray()).put("callees", JSONArray())

    @Test
    fun reportsDecompilerFailureInsteadOfInventingCode() {
        val ctx = ContextBuilder.build(
            info, "0x1000",
            Result.failure(IllegalStateException("boom")), Result.success(dis(3)), Result.success(rel()),
            ContextBuilder.DEFAULT_BUDGET_CHARS,
        )
        assertTrue(ctx.text.contains("decompiler unavailable: boom"))
        assertEquals(null, ctx.decompiled)
        assertTrue(ctx.text.contains("0x1000: mov x0, x1"))
    }

    @Test
    fun truncatesAndSaysSoWhenBudgetIsSmall() {
        val dec = JSONObject().put("name", "sub_1000").put("c", (1..200).joinToString("\n") { "int v$it = $it;" })
        val ctx = ContextBuilder.build(
            info, "0x1000", Result.success(dec), Result.success(dis(100)), Result.success(rel()), 1200,
        )
        assertTrue(ctx.truncated.isNotEmpty())
        assertTrue(ctx.text.contains("lines omitted"))
        assertTrue(ctx.text.contains("name: sub_1000"))
    }
}
