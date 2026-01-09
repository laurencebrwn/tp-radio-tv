@file:OptIn(ExperimentalTvMaterial3Api::class)

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

@Composable
fun CornerMarker(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    size: Dp = 14.dp,
    stroke: Dp = 2.dp,
    up: Boolean = false,
    down: Boolean = false,
    left: Boolean = false,
    right: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val t = stroke.toPx()
        val c = s / 2f   // center of the square
        val l = s / 2f   // length of each line from the center

        drawRect(color, topLeft = Offset(c - t/2, c - t/2), size = Size(t, t))

        // vertical lines
        if (up) drawRect(color, topLeft = Offset(c - t / 2, 0f), size = Size(t, c))
        if (down) drawRect(color, topLeft = Offset(c - t / 2, c), size = Size(t, c))

        // horizontal lines
        if (left) drawRect(color, topLeft = Offset(0f, c - t / 2), size = Size(c, t))
        if (right) drawRect(color, topLeft = Offset(c, c - t / 2), size = Size(c, t))
    }
}
