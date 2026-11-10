package com.arch1based.lensy.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.arch1based.lensy.core.Mood
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Bloodshot veins as (x, y) fractions of the eye box; mirrored for the right side.
private val Veins = listOf(
    listOf(0.02f to 0.50f, 0.14f to 0.42f, 0.24f to 0.47f),
    listOf(0.06f to 0.68f, 0.16f to 0.62f, 0.23f to 0.66f),
    listOf(0.08f to 0.33f, 0.18f to 0.37f, 0.25f to 0.33f),
)

/**
 * Lensy the eye. Blinks on its own, looks around, bounces when tapped, and gets
 * droopier and more bloodshot as the lenses age.
 */
@Composable
fun EyeMascot(mood: Mood, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val blink = remember { Animatable(0f) }
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 5200))
            blink.animateTo(1f, tween(90))
            blink.animateTo(0f, tween(150))
        }
    }
    val look by rememberInfiniteTransition(label = "look").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "look",
    )
    val iris by animateColorAsState(moodColor(mood), label = "iris")
    val open by animateFloatAsState(
        when (mood) {
            Mood.FRESH -> 1f
            Mood.OK -> 0.92f
            Mood.TIRED -> 0.68f
            Mood.DUE -> 0.58f
            Mood.OVERDUE -> 0.5f
        },
        label = "open",
    )
    val redness by animateFloatAsState(
        when (mood) {
            Mood.TIRED -> 0.35f
            Mood.DUE -> 0.6f
            Mood.OVERDUE -> 1f
            else -> 0f
        },
        label = "redness",
    )
    val lid = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.onSurface
    val browTilt = when (mood) {
        Mood.FRESH, Mood.OK -> 0.03f
        Mood.TIRED -> -0.03f
        Mood.DUE, Mood.OVERDUE -> -0.07f
    }

    Canvas(
        modifier
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                scope.launch {
                    bounce.animateTo(1.12f, tween(90))
                    bounce.animateTo(1f, spring(dampingRatio = 0.35f))
                }
            }
    ) {
        val w = size.width
        val h = w * 0.62f
        val top = (size.height - h) / 2f
        val box = Rect(0f, top, w, top + h)
        val stroke = w * 0.035f

        // Eyebrow: relaxed when fresh, worried when late.
        drawLine(
            outline,
            Offset(w * 0.22f, top - h * 0.14f + w * browTilt),
            Offset(w * 0.78f, top - h * 0.14f - w * browTilt),
            strokeWidth = stroke * 1.3f,
            cap = StrokeCap.Round,
        )

        clipPath(Path().apply { addOval(box) }) {
            drawRect(Color(1f, 1f - 0.12f * redness, 1f - 0.14f * redness), box.topLeft, box.size)

            if (redness > 0f) {
                val vein = Color(0xFFE5383B).copy(alpha = redness)
                for (mirror in listOf(false, true)) {
                    for (points in Veins) {
                        val path = points.map { (x, y) ->
                            Offset(box.left + (if (mirror) 1f - x else x) * w, top + y * h)
                        }
                        drawLine(vein, path[0], path[1], strokeWidth = stroke * 0.4f, cap = StrokeCap.Round)
                        drawLine(vein, path[1], path[2], strokeWidth = stroke * 0.3f, cap = StrokeCap.Round)
                    }
                }
            }

            val r = h * 0.36f
            val c = box.center + Offset(look * w * 0.05f, h * 0.12f * (1f - open))
            drawCircle(iris, r, c)
            drawCircle(Color.Black.copy(alpha = 0.16f), r * 0.91f, c, style = Stroke(r * 0.18f))
            drawCircle(Color(0xFF111827), r * 0.48f, c)
            drawCircle(Color.White, r * 0.17f, c + Offset(-r * 0.3f, -r * 0.32f))
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.08f, c + Offset(r * 0.28f, r * 0.26f))

            // Upper eyelid: droops with tiredness, closes fully on a blink.
            val lidHeight = h * (1f - open * (1f - blink.value))
            if (lidHeight > 0.5f) {
                drawRect(lid, box.topLeft, Size(w, lidHeight))
                drawLine(outline, Offset(0f, top + lidHeight), Offset(w, top + lidHeight), strokeWidth = stroke)
            }
        }
        drawOval(outline, box.topLeft, box.size, style = Stroke(stroke))

        if (redness > 0f) {
            // Bag under the eye.
            drawArc(
                outline.copy(alpha = 0.45f * redness + 0.15f),
                startAngle = 25f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(w * 0.15f, top + h * 0.74f),
                size = Size(w * 0.7f, h * 0.4f),
                style = Stroke(stroke * 0.7f, cap = StrokeCap.Round),
            )
        }
        if (mood == Mood.FRESH) {
            // Sparkles.
            val pulse = 0.5f + abs(look) * 0.5f
            drawCircle(iris, w * 0.028f * pulse, Offset(w * 0.93f, top - h * 0.02f))
            drawCircle(iris, w * 0.018f * (1.5f - pulse), Offset(w * 0.06f, top + h * 0.02f))
            drawCircle(iris, w * 0.022f * pulse, Offset(w * 0.88f, top + h * 1.08f))
        }
    }
}

/** Battery-style ring: full with fresh lenses, drains to empty on change day. */
@Composable
fun ProgressRing(remaining: Float, color: Color, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(remaining, tween(900), label = "ring")
    val c by animateColorAsState(color, label = "ringColor")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val stroke = size.minDimension * 0.06f
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val arc = Size(size.width - stroke, size.height - stroke)
        drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
        drawArc(c, -90f, 360f * p, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

private class Bit(val angle: Float, val speed: Float, val color: Color, val size: Float, val spin: Float)

private val ConfettiColors = listOf(
    Color(0xFF19C9A3), Color(0xFF2FA8F5), Color(0xFFF5A623), Color(0xFFFF7A59), Color(0xFFB57BFF), Color(0xFFFFD93D),
)

/** One confetti burst each time [trigger] changes to a new non-zero value. */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val t = remember(trigger) { Animatable(0f) }
    val bits = remember(trigger) {
        List(90) {
            Bit(
                angle = Random.nextFloat() * 6.2832f,
                speed = 0.25f + Random.nextFloat() * 0.75f,
                color = ConfettiColors.random(),
                size = 6f + Random.nextFloat() * 10f,
                spin = Random.nextFloat() * 720f - 360f,
            )
        }
    }
    LaunchedEffect(trigger) { t.animateTo(1f, tween(1900, easing = LinearOutSlowInEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val p = t.value
        if (p >= 1f) return@Canvas
        val cx = size.width / 2f
        val cy = size.height * 0.36f
        for (bit in bits) {
            val distance = bit.speed * p * size.width * 0.75f
            val x = cx + cos(bit.angle) * distance
            val y = cy + sin(bit.angle) * distance + p * p * size.height * 0.35f
            val s = bit.size.dp.toPx()
            rotate(bit.spin * p, Offset(x, y)) {
                drawRect(bit.color.copy(alpha = 1f - p), Offset(x - s / 2f, y - s / 2f), Size(s, s * 0.6f))
            }
        }
    }
}
