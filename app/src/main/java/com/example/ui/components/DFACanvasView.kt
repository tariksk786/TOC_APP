package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DFA
import kotlin.math.*

@Composable
fun DFACanvasView(
    dfa: DFA,
    modifier: Modifier = Modifier,
    activeStateId: String? = null,
    highlightTransition: Pair<String, String>? = null // (from, symbol)
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dfa_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.6f, 2.5f)
                        offset += pan
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + offset.x
            val centerY = height / 2f + offset.y

            val states = dfa.states
            if (states.isEmpty()) return@Canvas

            // Compute layout positions for states
            val nodeRadius = 26f * density * scale
            val statePositions = mutableMapOf<String, Offset>()

            val n = states.size
            if (n == 1) {
                statePositions[states[0].id] = Offset(centerX, centerY)
            } else if (n == 2) {
                val spread = (min(width, height) * 0.35f) * scale
                statePositions[states[0].id] = Offset(centerX - spread, centerY)
                statePositions[states[1].id] = Offset(centerX + spread, centerY)
            } else {
                val radius = (min(width, height) * 0.32f) * scale
                for (i in 0 until n) {
                    val angle = (2 * PI * i / n) - (PI / 2)
                    val x = centerX + radius * cos(angle).toFloat()
                    val y = centerY + radius * sin(angle).toFloat()
                    statePositions[states[i].id] = Offset(x, y)
                }
            }

            // Draw Transitions
            // Group transitions between same pair to combine labels e.g. "a, b"
            val groupedTransitions = dfa.transitions.groupBy { Pair(it.from, it.to) }

            for ((pair, transList) in groupedTransitions) {
                val (fromId, toId) = pair
                val fromPos = statePositions[fromId] ?: continue
                val toPos = statePositions[toId] ?: continue
                val symbolsText = transList.joinToString(", ") { it.symbol }
                val isHighlighted = highlightTransition != null &&
                        highlightTransition.first == fromId &&
                        transList.any { it.symbol == highlightTransition.second }

                if (fromId == toId) {
                    // Self-loop
                    drawSelfLoop(
                        center = fromPos,
                        radius = nodeRadius,
                        label = symbolsText,
                        isHighlighted = isHighlighted,
                        scale = scale,
                        density = density
                    )
                } else {
                    // Check if reverse transition exists
                    val hasReverse = groupedTransitions.containsKey(Pair(toId, fromId))
                    drawTransitionArrow(
                        from = fromPos,
                        to = toPos,
                        nodeRadius = nodeRadius,
                        label = symbolsText,
                        curved = hasReverse,
                        isHighlighted = isHighlighted,
                        scale = scale,
                        density = density
                    )
                }
            }

            // Draw States
            for (state in states) {
                val pos = statePositions[state.id] ?: continue
                val isActive = state.id == activeStateId
                drawStateNode(
                    pos = pos,
                    radius = nodeRadius,
                    stateId = state.id,
                    isStart = state.isStart,
                    isFinal = state.isFinal,
                    isActive = isActive,
                    scale = scale,
                    density = density
                )
            }
        }

        // Overlay Zoom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledTonalIconButton(
                onClick = { scale = (scale + 0.2f).coerceAtMost(2.5f) },
                modifier = Modifier.size(36.dp).testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
            }
            FilledTonalIconButton(
                onClick = { scale = (scale - 0.2f).coerceAtLeast(0.6f) },
                modifier = Modifier.size(36.dp).testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
            }
            FilledTonalIconButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier.size(36.dp).testTag("fit_diagram_button")
            ) {
                Icon(Icons.Default.FitScreen, contentDescription = "Fit Diagram", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun DrawScope.drawStateNode(
    pos: Offset,
    radius: Float,
    stateId: String,
    isStart: Boolean,
    isFinal: Boolean,
    isActive: Boolean,
    scale: Float,
    density: Float
) {
    val nodeFillColor = when {
        isActive -> Color(0xFFFDE047) // Bright amber active highlight
        else -> Color.White
    }
    val strokeColor = when {
        isActive -> Color(0xFFD97706)
        else -> Color(0xFF2563EB)
    }

    // Outer circle
    drawCircle(
        color = nodeFillColor,
        radius = radius,
        center = pos
    )
    drawCircle(
        color = strokeColor,
        radius = radius,
        center = pos,
        style = Stroke(width = if (isActive) 3.5f * density else 2.5f * density)
    )

    // Inner concentric circle for final / accepting states
    if (isFinal) {
        drawCircle(
            color = strokeColor,
            radius = radius - (5f * density * scale).coerceAtLeast(4f),
            center = pos,
            style = Stroke(width = 1.8f * density)
        )
    }

    // Start state arrow
    if (isStart) {
        val arrowStart = Offset(pos.x - radius - 30f * density * scale, pos.y)
        val arrowEnd = Offset(pos.x - radius, pos.y)
        drawLine(
            color = Color(0xFF0EA5E9),
            start = arrowStart,
            end = arrowEnd,
            strokeWidth = 2.5f * density,
            cap = StrokeCap.Round
        )
        // Arrow head
        val headSize = 8f * density * scale
        val path = Path().apply {
            moveTo(arrowEnd.x, arrowEnd.y)
            lineTo(arrowEnd.x - headSize, arrowEnd.y - headSize * 0.6f)
            lineTo(arrowEnd.x - headSize, arrowEnd.y + headSize * 0.6f)
            close()
        }
        drawPath(path, color = Color(0xFF0EA5E9))
    }

    // State Label text (using native canvas for crisp vector text)
    val textSize = (14f * density * scale).coerceIn(12f * density, 24f * density)
    val paint = android.graphics.Paint().apply {
        color = if (isActive) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#0F172A")
        isAntiAlias = true
        this.textSize = textSize
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.drawText(
        stateId,
        pos.x,
        pos.y + (textSize * 0.35f),
        paint
    )
}

private fun DrawScope.drawSelfLoop(
    center: Offset,
    radius: Float,
    label: String,
    isHighlighted: Boolean,
    scale: Float,
    density: Float
) {
    val loopRadius = radius * 0.75f
    val loopCenter = Offset(center.x, center.y - radius - loopRadius * 0.7f)
    val strokeColor = if (isHighlighted) Color(0xFFD97706) else Color(0xFF0EA5E9)

    drawCircle(
        color = strokeColor,
        radius = loopRadius,
        center = loopCenter,
        style = Stroke(width = if (isHighlighted) 3f * density else 2f * density)
    )

    // Arrow head on self-loop pointing to right side of state
    val arrowPos = Offset(center.x + radius * 0.6f, center.y - radius * 0.7f)
    val headSize = 7f * density * scale
    val path = Path().apply {
        moveTo(arrowPos.x, arrowPos.y)
        lineTo(arrowPos.x + headSize, arrowPos.y - headSize)
        lineTo(arrowPos.x + headSize * 0.3f, arrowPos.y - headSize * 1.5f)
        close()
    }
    drawPath(path, color = strokeColor)

    // Label on top of loop
    val textSize = (12f * density * scale).coerceIn(10f * density, 20f * density)
    val paint = android.graphics.Paint().apply {
        color = if (isHighlighted) android.graphics.Color.parseColor("#B45309") else android.graphics.Color.parseColor("#1E293B")
        isAntiAlias = true
        this.textSize = textSize
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.drawText(
        label,
        loopCenter.x,
        loopCenter.y - loopRadius - 4f * density,
        paint
    )
}

private fun DrawScope.drawTransitionArrow(
    from: Offset,
    to: Offset,
    nodeRadius: Float,
    label: String,
    curved: Boolean,
    isHighlighted: Boolean,
    scale: Float,
    density: Float
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < 1f) return

    val strokeColor = if (isHighlighted) Color(0xFFD97706) else Color(0xFF475569)
    val strokeWidth = if (isHighlighted) 3f * density else 2f * density

    val unitX = dx / dist
    val unitY = dy / dist

    if (!curved) {
        // Straight arrow
        val startX = from.x + unitX * nodeRadius
        val startY = from.y + unitY * nodeRadius
        val endX = to.x - unitX * nodeRadius
        val endY = to.y - unitY * nodeRadius

        drawLine(
            color = strokeColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Arrow head
        drawArrowHead(Offset(endX, endY), Offset(unitX, unitY), strokeColor, scale, density)

        // Label at midpoint
        val midX = (startX + endX) / 2f
        val midY = (startY + endY) / 2f
        // Offset label perpendicular
        val perpX = -unitY * (14f * density)
        val perpY = unitX * (14f * density)
        drawTextLabel(label, midX + perpX, midY + perpY, isHighlighted, scale, density)
    } else {
        // Curved quadratic bezier
        val perpX = -unitY * (35f * density * scale)
        val perpY = unitX * (35f * density * scale)

        val ctrlX = (from.x + to.x) / 2f + perpX
        val ctrlY = (from.y + to.y) / 2f + perpY

        // Approximate tangent at endpoint
        val endX = to.x - unitX * nodeRadius + perpX * 0.15f
        val endY = to.y - unitY * nodeRadius + perpY * 0.15f
        val startX = from.x + unitX * nodeRadius + perpX * 0.15f
        val startY = from.y + unitY * nodeRadius + perpY * 0.15f

        val path = Path().apply {
            moveTo(startX, startY)
            quadraticTo(ctrlX, ctrlY, endX, endY)
        }
        drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))

        // Arrow head tangent from control point to end point
        val tdx = endX - ctrlX
        val tdy = endY - ctrlY
        val tdist = max(1f, sqrt(tdx * tdx + tdy * tdy))
        drawArrowHead(Offset(endX, endY), Offset(tdx / tdist, tdy / tdist), strokeColor, scale, density)

        // Label at curve peak
        drawTextLabel(label, ctrlX, ctrlY - 4f * density, isHighlighted, scale, density)
    }
}

private fun DrawScope.drawArrowHead(
    tip: Offset,
    direction: Offset,
    color: Color,
    scale: Float,
    density: Float
) {
    val headLen = 9f * density * scale
    val perp = Offset(-direction.y, direction.x)
    val base = Offset(tip.x - direction.x * headLen, tip.y - direction.y * headLen)
    val left = Offset(base.x + perp.x * headLen * 0.5f, base.y + perp.y * headLen * 0.5f)
    val right = Offset(base.x - perp.x * headLen * 0.5f, base.y - perp.y * headLen * 0.5f)

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawTextLabel(
    text: String,
    x: Float,
    y: Float,
    isHighlighted: Boolean,
    scale: Float,
    density: Float
) {
    val textSize = (12f * density * scale).coerceIn(11f * density, 19f * density)
    val paint = android.graphics.Paint().apply {
        color = if (isHighlighted) android.graphics.Color.parseColor("#B45309") else android.graphics.Color.parseColor("#0F172A")
        isAntiAlias = true
        this.textSize = textSize
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    // Background pill for label legibility
    val bounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val pad = 4f * density
    drawRoundRect(
        color = Color(0xEEFFFFFF),
        topLeft = Offset(x - bounds.width() / 2f - pad, y - bounds.height() - pad),
        size = androidx.compose.ui.geometry.Size(bounds.width() + pad * 2, bounds.height() + pad * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * density, 4f * density)
    )
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
