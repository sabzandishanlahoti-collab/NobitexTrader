package ir.nobitextrader.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CyberButtonStyle {
    PRIMARY,
    OUTLINED,
    DANGER
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: CyberButtonStyle = CyberButtonStyle.PRIMARY,
    height: Dp = 48.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val defaultContainer = when (style) {
        CyberButtonStyle.PRIMARY -> Color(0xFF35DDE0)
        CyberButtonStyle.OUTLINED -> Color(0xFF101B2B)
        CyberButtonStyle.DANGER -> Color(0xFF321923)
    }

    val defaultContent = when (style) {
        CyberButtonStyle.PRIMARY -> Color(0xFF07141D)
        CyberButtonStyle.OUTLINED -> Color(0xFF35DDE0)
        CyberButtonStyle.DANGER -> Color(0xFFFF647C)
    }

    val defaultBorder = when (style) {
        CyberButtonStyle.PRIMARY -> Color(0xFF35DDE0)
        CyberButtonStyle.OUTLINED -> Color(0xFF35DDE0)
        CyberButtonStyle.DANGER -> Color(0xFFFF647C)
    }

    val background = containerColor ?: defaultContainer
    val foreground = contentColor ?: defaultContent
    val outline = borderColor ?: defaultBorder

    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = foreground,
            disabledContainerColor = Color(0xFF263442),
            disabledContentColor = Color(0xFF71818C)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) outline else Color(0xFF394957)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}
