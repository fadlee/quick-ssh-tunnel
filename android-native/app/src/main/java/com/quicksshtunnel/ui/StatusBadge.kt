package com.quicksshtunnel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small pill-shaped status badge.
 * Green "Running" or gray "Stopped".
 */
@Composable
fun StatusBadge(running: Boolean, modifier: Modifier = Modifier) {
    val (text, color) = if (running) {
        "Running" to ActiveColor
    } else {
        "Stopped" to InactiveColor
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
        )
    }
}
