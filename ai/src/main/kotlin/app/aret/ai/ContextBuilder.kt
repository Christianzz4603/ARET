package app.aret.ai

import app.aret.engine.BinarySession
import org.json.JSONObject

data class FunctionContext(
    val name: String?,
    val address: String,
    val decompiled: String?,
    val decompileError: String?,
    val asmLines: List<String>,
    /** Human-readable notes about what was cut to fit the budget. */
    val truncated: List<String>,
    /** Exactly what would be sent to the model as the user-side context. */
    val text: String,
)

/**
 * Turns real engine output into a bounded, evidence-labelled prompt context.
 * Priority when budget is tight: header > call edges > decompiled code > disassembly.
 * It never adds facts the engine did not report.
 */
object ContextBuilder {
    const val DEFAULT_BUDGET_CHARS = 6000

    val SYSTEM_PROMPT: String = listOf(
        "You are a reverse-engineering assistant inside a static analysis app.",
        "Use ONLY the provided context. Never invent symbols, addresses, instructions or behaviour.",
        "Decompiled code is inferred pseudocode, not original source. Call edges are direct calls only.",
        "Separate OBSERVED facts (quote addresses/instructions) from HYPOTHESES (say 'likely' / 'possibly').",
        "If the context is insufficient, say exactly what is missing instead of guessing.",
        "Keep answers short and concrete.",
    ).joinToString("\n")

    suspend fun forFunction(
        session: BinarySession,
        address: String,
        budgetChars: Int = DEFAULT_BUDGET_CHARS,
    ): FunctionContext {
        val info = session.info()
        val dec = runCatching { session.decompile(address) }
        val dis = runCatching { session.disassembleFunction(address) }
        val rel = runCatching { session.callRelations(address) }
        return build(info, address, dec, dis, rel, budgetChars)
    }

    fun build(
        info: JSONObject,
        address: String,
        dec: Result<JSONObject>,
        dis: Result<JSONObject>,
        rel: Result<JSONObject>,
        budgetChars: Int,
    ): FunctionContext {
        val truncated = mutableListOf<String>()
        val decObj = dec.getOrNull()
        val name = decObj?.let { if (it.isNull("name")) null else it.getString("name") }
        val decompiled = decObj?.optString("c")
        val decompileError = dec.exceptionOrNull()?.message

        val asmLines = dis.getOrNull()?.optJSONArray("instructions")?.let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                "${o.getString("address")}: ${o.getString("mnemonic")} ${o.getString("operands")}".trimEnd()
            }
        } ?: emptyList()
        val gaps = dis.getOrNull()?.optJSONArray("gaps")?.length() ?: 0

        fun edges(key: String): List<String> {
            val arr = rel.getOrNull()?.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                val n = if (o.isNull("name")) "unnamed" else o.getString("name")
                "$n (${o.getString("address")})"
            }
        }

        val header = buildString {
            append("## Binary\n")
            append("language: ").append(info.optString("language", "unknown")).append('\n')
            append("## Target function\n")
            append("address: ").append(address).append('\n')
            append("name: ").append(name ?: "(no symbol)").append('\n')
        }
        val notes = buildString {
            append("## Evidence notes\n")
            append("- Pseudocode is inferred by the decompiler; types and names may be wrong.\n")
            append("- Call edges are direct calls only; indirect calls are not resolved.\n")
            if (gaps > 0) append("- Disassembly has $gaps undecodable gap(s).\n")
            if (rel.isFailure) append("- Call relations unavailable: ${rel.exceptionOrNull()?.message}\n")
            append("- Referenced strings and cross-references are not included in this context.\n")
        }

        val remaining = (budgetChars - header.length - notes.length).coerceAtLeast(0)
        val callsBudget = remaining * 15 / 100
        val decBudget = remaining * 50 / 100
        val asmBudget = remaining - callsBudget - decBudget

        val callLines = listOf("callees: " + edges("callees").joinToString(", ").ifEmpty { "none found" }) +
            listOf("callers: " + edges("callers").joinToString(", ").ifEmpty { "none found" })
        val callsText = clip(callLines, callsBudget, "calls", truncated)

        val decText = when {
            decompiled != null -> clip(decompiled.lines(), decBudget, "decompiled", truncated)
            else -> "decompiler unavailable: ${decompileError ?: "unknown"}\n"
        }
        val asmText = if (asmLines.isEmpty()) {
            "disassembly unavailable: ${dis.exceptionOrNull()?.message ?: "no instructions"}\n"
        } else {
            clip(asmLines, asmBudget, "disassembly", truncated)
        }

        val text = buildString {
            append(header)
            append("## Calls (direct edges only)\n").append(callsText)
            append("## Decompiled pseudocode (inferred)\n").append(decText)
            append("## Disassembly\n").append(asmText)
            append(notes)
        }
        return FunctionContext(name, address, decompiled, decompileError, asmLines, truncated, text)
    }

    private fun clip(lines: List<String>, maxChars: Int, label: String, truncated: MutableList<String>): String {
        val sb = StringBuilder()
        var used = 0
        var i = 0
        while (i < lines.size) {
            val l = lines[i]
            if (used + l.length + 1 > maxChars) break
            sb.append(l).append('\n')
            used += l.length + 1
            i++
        }
        if (i < lines.size) {
            truncated.add("$label: ${lines.size - i} of ${lines.size} lines omitted")
            sb.append("[... ${lines.size - i} more lines omitted ...]\n")
        }
        return sb.toString()
    }
}
