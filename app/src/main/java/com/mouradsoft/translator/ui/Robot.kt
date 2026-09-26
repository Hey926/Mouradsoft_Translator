package com.mouradsoft.translator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics

enum class RobotMood { Welcome, Thinking, Celebrate, Explain }

/** Original vector-like Compose illustration. Geometry is decorative, never read by TalkBack. */
@Composable
fun Robot(mood: RobotMood, modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val scale = minOf(size.width / 280f, size.height / 280f)
        withTransform({ translate((size.width - 280 * scale) / 2, (size.height - 280 * scale) / 2); scale(scale, scale, Offset.Zero) }) {
            drawCircle(Palette.Aqua, 109f, Offset(140f, 137f))
            drawCircle(Color.White.copy(alpha = .65f), 81f, Offset(140f, 137f))
            drawOval(Palette.Navy.copy(alpha = .08f), Offset(84f, 250f), Size(115f, 13f))
            fun line(a: Offset, b: Offset, color: Color = Palette.Teal, width: Float = 9f) =
                drawLine(color, a, b, width, StrokeCap.Round)
            // Feet, neck, and antenna.
            line(Offset(119f, 228f), Offset(113f, 248f), Palette.TealDark, 15f)
            line(Offset(161f, 228f), Offset(169f, 248f), Palette.TealDark, 15f)
            drawRoundRect(Palette.Navy, Offset(96f, 244f), Size(31f, 12f), CornerRadius(6f))
            drawRoundRect(Palette.Navy, Offset(154f, 244f), Size(31f, 12f), CornerRadius(6f))
            drawRoundRect(Palette.TealDark, Offset(126f, 155f), Size(28f, 25f), CornerRadius(6f))
            line(Offset(140f, 44f), Offset(140f, 62f), Palette.TealDark, 5f)
            drawCircle(Palette.Yellow, 9f, Offset(140f, 37f))
            drawCircle(Color.White.copy(alpha = .7f), 3f, Offset(137f, 34f))
            // One hand waves; the same arms change pose for each expression.
            val leftHand = if (mood == RobotMood.Celebrate) Offset(67f, 157f) else Offset(81f, 219f)
            val rightHand = when (mood) {
                RobotMood.Welcome, RobotMood.Celebrate -> Offset(219f, 128f)
                RobotMood.Thinking -> Offset(189f, 161f)
                RobotMood.Explain -> Offset(224f, 185f)
            }
            line(Offset(103f, 189f), leftHand, Palette.Teal, 13f)
            line(Offset(178f, 188f), Offset(204f, 176f), Palette.Teal, 13f)
            line(Offset(204f, 176f), rightHand, Palette.Teal, 13f)
            drawCircle(Palette.Shell, 13f, leftHand)
            drawCircle(Palette.Shell, 14f, rightHand)
            // Compact body and M badge.
            drawRoundRect(Palette.Teal, Offset(99f, 172f), Size(83f, 63f), CornerRadius(23f))
            drawRoundRect(Palette.Shell, Offset(103f, 171f), Size(75f, 57f), CornerRadius(21f))
            drawCircle(Palette.Cream, 17f, Offset(140f, 198f))
            drawPath(Path().apply { moveTo(131f, 205f); lineTo(131f, 190f); lineTo(140f, 199f); lineTo(149f, 190f); lineTo(149f, 205f) },
                Palette.Navy, style = Stroke(3.5f, cap = StrokeCap.Round))
            // Earpieces and rounded shell.
            drawRoundRect(Palette.Teal, Offset(52f, 99f), Size(21f, 34f), CornerRadius(9f))
            drawRoundRect(Palette.Teal, Offset(207f, 99f), Size(21f, 34f), CornerRadius(9f))
            drawRoundRect(Palette.Teal, Offset(64f, 62f), Size(152f, 102f), CornerRadius(36f))
            drawRoundRect(Palette.Shell, Offset(64f, 57f), Size(152f, 101f), CornerRadius(36f))
            drawRoundRect(Color.White.copy(alpha = .6f), Offset(80f, 64f), Size(113f, 5f), CornerRadius(3f))
            drawRoundRect(Palette.Navy, Offset(77f, 74f), Size(126f, 71f), CornerRadius(26f))
            // Expressive, large eyes; no bouncing or continuous animation.
            if (mood == RobotMood.Celebrate) {
                drawArc(Color.White, 200f, 140f, false, Offset(96f, 92f), Size(25f, 24f), style = Stroke(5f, cap = StrokeCap.Round))
                drawArc(Color.White, 200f, 140f, false, Offset(159f, 92f), Size(25f, 24f), style = Stroke(5f, cap = StrokeCap.Round))
            } else {
                val glance = if (mood == RobotMood.Thinking) 4f else 0f
                listOf(109f, 171f).forEach { x ->
                    drawOval(Color.White, Offset(x - 12f, 88f), Size(25f, 29f))
                    drawOval(Palette.Teal, Offset(x - 5f + glance, 96f), Size(13f, 18f))
                    drawCircle(Palette.Navy, 4f, Offset(x + 2f + glance, 106f))
                    drawCircle(Color.White, 2.8f, Offset(x + glance, 101f))
                }
            }
            drawArc(Palette.Shell, 15f, 150f, false, Offset(128f, 114f), Size(25f, 16f), style = Stroke(3.5f, cap = StrokeCap.Round))
            drawOval(Palette.Teal, Offset(87f, 120f), Size(13f, 5f))
            drawOval(Palette.Teal, Offset(181f, 120f), Size(13f, 5f))
            // A few warm accents, scaled with the character.
            fun sparkle(x: Float, y: Float) {
                line(Offset(x - 6, y), Offset(x + 6, y), Palette.Yellow, 3f)
                line(Offset(x, y - 6), Offset(x, y + 6), Palette.Yellow, 3f)
            }
            sparkle(43f, 68f)
            if (mood == RobotMood.Celebrate) {
                sparkle(231f, 56f); sparkle(51f, 195f)
                drawCircle(Palette.Teal, 4f, Offset(207f, 31f))
                drawCircle(Palette.Yellow, 4f, Offset(70f, 38f))
            } else drawCircle(Palette.Yellow, 4f, Offset(236f, 76f))
        }
    }
}
