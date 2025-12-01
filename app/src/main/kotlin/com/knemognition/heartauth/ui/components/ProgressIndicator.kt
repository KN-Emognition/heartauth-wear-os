package com.knemognition.heartauth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GradientCircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 6.dp,
    colors: List<Color> = listOf(Color(0xffb31d1d), Color(0xFF2a0a3d),Color(0xffb31d1d)),
    trackColor: Color = Color.Transparent,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val stroke = remember(strokePx) { Stroke(width = strokePx, cap = StrokeCap.Round) }

    Canvas(modifier) {
        val diameter = size.minDimension
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val center = Offset(topLeft.x + arcSize.width / 2f, topLeft.y + arcSize.height / 2f)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )

        val sweep = Brush.sweepGradient(colors = colors, center = center)
        withTransform({
            rotate(degrees = -89f, pivot = center)
        }) {
            drawArc(
                brush = sweep,
                startAngle = 6f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
    }
}

