package app.aret.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

class CodePalette(
    val keyword: Color,
    val type: Color,
    val number: Color,
    val string: Color,
    val comment: Color,
    val call: Color,
)

@Composable
fun rememberCodePalette(): CodePalette {
    val c = MaterialTheme.colorScheme
    return remember(c) {
        val dark = c.background.luminance() < 0.5f
        CodePalette(
            keyword = c.tertiary,
            type = c.secondary,
            number = if (dark) Color(0xFFF2A07B) else Color(0xFFB4532A),
            string = if (dark) Color(0xFF9BD18B) else Color(0xFF2E7D32),
            comment = c.onSurfaceVariant.copy(alpha = 0.8f),
            call = if (dark) Color(0xFF7CB8FF) else Color(0xFF1C5FB8),
        )
    }
}

private val KEYWORDS = setOf(
    "if", "else", "while", "for", "do", "switch", "case", "default", "break", "continue", "return",
    "goto", "sizeof", "struct", "union", "enum", "typedef", "static", "extern", "const", "volatile",
    "unsigned", "signed",
)
private val TYPES = setOf(
    "void", "int", "char", "short", "long", "float", "double", "bool", "byte", "uint", "ulong",
    "ushort", "uchar", "longlong", "ulonglong", "size_t", "code", "wchar_t",
)
private val FIXED_WIDTH = Regex("""(?:u?int\d+_t|undefined\d*)""")

private val C_TOKEN = Regex(
    """(//.*|/\*.*?\*/)|("(?:\\.|[^"\\])*")|('(?:\\.|[^'\\])*')|\b(0[xX][0-9a-fA-F]+|\d+)[uUlL]*\b|\b([A-Za-z_][A-Za-z0-9_]*)\b""",
)

fun highlightC(line: String, p: CodePalette): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in C_TOKEN.findAll(line)) {
        if (m.range.first > last) append(line.substring(last, m.range.first))
        val t = m.value
        val g = m.groups
        val style: SpanStyle? = when {
            g[1] != null -> SpanStyle(color = p.comment, fontStyle = FontStyle.Italic)
            g[2] != null || g[3] != null -> SpanStyle(color = p.string)
            g[4] != null -> SpanStyle(color = p.number)
            t in KEYWORDS -> SpanStyle(color = p.keyword, fontWeight = FontWeight.Bold)
            t in TYPES || FIXED_WIDTH.matches(t) -> SpanStyle(color = p.type)
            line.getOrNull(m.range.last + 1) == '(' -> SpanStyle(color = p.call)
            else -> null
        }
        if (style != null) withStyle(style) { append(t) } else append(t)
        last = m.range.last + 1
    }
    if (last < line.length) append(line.substring(last))
}

class AsmPalette(
    val dim: Color,
    val call: Color,
    val branch: Color,
    val ret: Color,
    val mem: Color,
    val reg: Color,
    val imm: Color,
    val plain: Color,
)

@Composable
fun rememberAsmPalette(): AsmPalette {
    val c = MaterialTheme.colorScheme
    return remember(c) {
        AsmPalette(
            dim = c.onSurfaceVariant.copy(alpha = 0.7f),
            call = c.primary,
            branch = c.tertiary,
            ret = c.error,
            mem = c.secondary,
            reg = c.primary.copy(alpha = 0.85f),
            imm = c.secondary,
            plain = c.onSurface,
        )
    }
}

private val CALLS = setOf("bl", "blr", "blx", "call", "blraa", "blrab")
private val ARM_BRANCH = setOf("b", "br", "bx", "cbz", "cbnz", "tbz", "tbnz", "braa", "brab")
private val MEM_PREFIX = listOf("ldr", "str", "ldp", "stp", "ldu", "stu", "ldx", "stx", "ldar", "stlr")

private fun mnemonicColor(m: String, p: AsmPalette): Color {
    val s = m.lowercase()
    return when {
        s in CALLS -> p.call
        s.startsWith("ret") -> p.ret
        s in ARM_BRANCH || s.startsWith("b.") || s.startsWith("j") -> p.branch
        s == "nop" -> p.dim
        s == "push" || s == "pop" || MEM_PREFIX.any { s.startsWith(it) } -> p.mem
        else -> p.plain
    }
}

private val ASM_TOKEN = Regex(
    """(#?-?0[xX][0-9a-fA-F]+|#-?\d+|\b\d+\b)|(\b[xwvqdsb]\d{1,2}\b|\b(?:sp|lr|fp|xzr|wzr|pc)\b|\b[re]?(?:ax|bx|cx|dx|si|di|sp|bp|ip)\b|\br\d{1,2}[dwb]?\b|\b[xy]mm\d+\b)""",
)

fun highlightAsm(i: app.aret.AsmLine, p: AsmPalette): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = p.dim)) { append(i.address.removePrefix("0x").padStart(8, '0')); append("  ") }
    withStyle(SpanStyle(color = p.dim.copy(alpha = 0.55f))) {
        append(i.bytes.chunked(2).joinToString(" ").padEnd(11)); append("  ")
    }
    withStyle(SpanStyle(color = mnemonicColor(i.mnemonic, p), fontWeight = FontWeight.SemiBold)) {
        append(i.mnemonic.padEnd(7)); append(' ')
    }
    val ops = i.operands
    var last = 0
    for (m in ASM_TOKEN.findAll(ops)) {
        if (m.range.first > last) append(ops.substring(last, m.range.first))
        val color = if (m.groups[1] != null) p.imm else p.reg
        withStyle(SpanStyle(color = color)) { append(m.value) }
        last = m.range.last + 1
    }
    if (last < ops.length) append(ops.substring(last))
}
