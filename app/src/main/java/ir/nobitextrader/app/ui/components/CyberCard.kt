package ir.nobitextrader.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable terminal panel for dashboard and scanner surfaces.
 *
 * Colors are parameters so the screen theme can supply its own palette
 * without coupling this component to a particular theme implementation.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF101B2B),
    contentColor: Color = Color(0xFFEAFBFC),
    borderColor: Color = Color(0xFF244452),
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = shadowElevation
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
