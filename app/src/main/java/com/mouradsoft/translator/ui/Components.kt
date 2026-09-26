package com.mouradsoft.translator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mouradsoft.translator.R

enum class Symbol { Arrow, Back, Mic, Stop, Check, Spark, Bubble }

@Composable
fun Mark(symbol: Symbol, modifier: Modifier = Modifier, tint: Color = Palette.TealDark) {
    Canvas(modifier.size(24.dp).clearAndSetSemantics { }) {
        scale(size.width / 24f, size.height / 24f, Offset.Zero) {
            val stroke = Stroke(2f, cap = StrokeCap.Round)
            fun line(x: Float, y: Float, a: Float, b: Float) = drawLine(tint, Offset(x, y), Offset(a, b), 2f, StrokeCap.Round)
            when (symbol) {
                Symbol.Arrow -> { line(4f,12f,20f,12f); line(14f,6f,20f,12f); line(14f,18f,20f,12f) }
                Symbol.Back -> { line(4f,12f,20f,12f); line(10f,6f,4f,12f); line(10f,18f,4f,12f) }
                Symbol.Check -> { line(5f,12f,10f,17f); line(10f,17f,19f,7f) }
                Symbol.Stop -> drawRoundRect(tint, Offset(5f,5f), Size(14f,14f), androidx.compose.ui.geometry.CornerRadius(3f))
                Symbol.Mic -> {
                    drawRoundRect(tint, Offset(9f,2f), Size(6f,12f), androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                    drawArc(tint, 0f, 180f, false, Offset(5f,6f), Size(14f,12f), style = stroke)
                    line(12f,18f,12f,22f); line(8f,22f,16f,22f)
                }
                Symbol.Spark -> {
                    drawPath(Path().apply { moveTo(12f,2f); lineTo(15f,9f); lineTo(22f,12f); lineTo(15f,15f); lineTo(12f,22f); lineTo(9f,15f); lineTo(2f,12f); lineTo(9f,9f); close() }, tint, style = stroke)
                }
                Symbol.Bubble -> {
                    drawPath(Path().apply { moveTo(5f,3f); lineTo(19f,3f); quadraticTo(22f,3f,22f,6f); lineTo(22f,14f); quadraticTo(22f,17f,19f,17f); lineTo(10f,17f); lineTo(4f,22f); lineTo(4f,17f); quadraticTo(2f,17f,2f,14f); lineTo(2f,6f); quadraticTo(2f,3f,5f,3f); close() }, tint, style = stroke)
                    line(7f,8f,17f,8f); line(7f,12f,14f,12f)
                }
            }
        }
    }
}

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, enabled: Boolean = true, icon: Symbol = Symbol.Arrow) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = Space.touch), enabled = enabled,
        shape = Corners.medium, contentPadding = PaddingValues(Space.md)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(Space.sm))
        Mark(icon, tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f))
    }
}

@Composable
fun ScreenFrame(step: Int, onBack: (() -> Unit)? = null, bottom: @Composable ColumnScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.Mist).safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Space.maxWidth).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onBack, Modifier.size(48.dp)) {
                        // The accessible label belongs to the button, not its decorative strokes.
                        val back = stringResource(R.string.back)
                        Box(Modifier.semantics { contentDescription = back }) { Mark(Symbol.Back) }
                    }
                    Spacer(Modifier.width(Space.xs))
                } else {
                    Box(Modifier.size(40.dp).background(Palette.Aqua, Corners.small), contentAlignment = Alignment.Center) { Mark(Symbol.Bubble) }
                    Spacer(Modifier.width(Space.sm))
                }
                Text(stringResource(R.string.brand), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                val stepLabel = stringResource(R.string.step, step)
                Row(Modifier.semantics { contentDescription = stepLabel }, horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    repeat(5) { index -> Box(Modifier.size(if (index == step - 1) 16.dp else 5.dp, 5.dp).background(
                        if (index < step) Palette.Teal else Palette.Line, Corners.small)) }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.lg, vertical = Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.lg), content = content)
            Column(Modifier.fillMaxWidth().padding(horizontal = Space.lg).padding(top = Space.sm, bottom = Space.md),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs), content = bottom)
        }
    }
}

@Composable
fun Heading(text: String, large: Boolean = false) {
    Text(text, style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() })
}

@Composable
fun DemoLabel() {
    Surface(color = Palette.Cream, shape = Corners.small) {
        Row(Modifier.padding(horizontal = Space.sm, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            Mark(Symbol.Spark, Modifier.size(18.dp), Palette.Navy)
            Spacer(Modifier.width(Space.xs))
            Text(stringResource(R.string.demo_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun Notice(text: String, error: Boolean = false, extra: @Composable ColumnScope.() -> Unit = {}) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else Palette.Aqua, shape = Corners.medium) {
        Column(Modifier.fillMaxWidth().padding(Space.md).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = if (error) Palette.Error else Palette.Navy)
            extra()
        }
    }
}
