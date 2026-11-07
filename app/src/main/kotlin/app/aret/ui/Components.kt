package app.aret.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    if (onClick == null) {
        Surface(modifier, shape = shape, color = c.surfaceContainer, border = BorderStroke(1.dp, c.outlineVariant)) {
            Column(content = content)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = c.surfaceContainer,
            border = BorderStroke(1.dp, c.outlineVariant),
        ) { Column(content = content) }
    }
}

/** App mark drawn in code: a "</>" glyph on a rounded tile. */
@Composable
fun AretLogo(size: Dp = 56.dp) {
    val c = MaterialTheme.colorScheme
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val corner = CornerRadius(s * 0.28f)
        drawRoundRect(
            brush = Brush.linearGradient(listOf(c.surfaceContainerHigh, c.surfaceContainerHighest)),
            cornerRadius = corner,
        )
        drawRoundRect(color = c.outline, cornerRadius = corner, style = Stroke(width = s * 0.02f))
        val w = s * 0.07f
        val left = Path().apply {
            moveTo(0.38f * s, 0.32f * s); lineTo(0.22f * s, 0.50f * s); lineTo(0.38f * s, 0.68f * s)
        }
        val right = Path().apply {
            moveTo(0.62f * s, 0.32f * s); lineTo(0.78f * s, 0.50f * s); lineTo(0.62f * s, 0.68f * s)
        }
        val st = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(left, c.primary, style = st)
        drawPath(right, c.primary, style = st)
        drawLine(c.secondary, Offset(0.55f * s, 0.27f * s), Offset(0.45f * s, 0.73f * s), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: @Composable () -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, fontFamily = FontFamily.Monospace)
            }
        }
        action()
    }
}
