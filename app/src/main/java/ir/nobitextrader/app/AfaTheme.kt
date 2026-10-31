package ir.nobitextrader.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Shared visual system for NOBITEX TRADER A.F.A SNIPER.
 * Market-state colors are intentionally distinct from the brand accent.
 */
object AfaColorSchemes {
    private val cyan = Color(0xFF35DDE0)
    private val darkBackground = Color(0xFF070D18)
    private val darkSurface = Color(0xFF101B2B)
    private val darkSurfaceVariant = Color(0xFF19283A)
    private val darkText = Color(0xFFEAFBFC)
    private val mutedText = Color(0xFFA9C2CC)

    fun create(dark: Boolean) = if (dark) {
        darkColorScheme(
            primary = cyan,
            onPrimary = Color(0xFF00191B),
            primaryContainer = Color(0xFF12383D),
            onPrimaryContainer = Color(0xFFBDFBFC),
            secondary = Color(0xFF8CECEF),
            tertiary = Color(0xFFB7A0FF),
            background = darkBackground,
            surface = darkSurface,
            surfaceVariant = darkSurfaceVariant,
            onBackground = darkText,
            onSurface = darkText,
            onSurfaceVariant = mutedText,
            outline = Color(0xFF31515A),
            outlineVariant = Color(0xFF243B4A),
            error = Color(0xFFFF647C)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF007F88),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD1F5F5),
            onPrimaryContainer = Color(0xFF00363A),
            secondary = Color(0xFF087E88),
            tertiary = Color(0xFF6550A5),
            background = Color(0xFFF3FAFB),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE1F2F3),
            onBackground = Color(0xFF102329),
            onSurface = Color(0xFF102329),
            onSurfaceVariant = Color(0xFF526B72),
            outline = Color(0xFFC9E0E2),
            outlineVariant = Color(0xFFD8E8E9),
            error = Color(0xFFBA1A1A)
        )
    }

    val profit = Color(0xFF35D6B0)
    val loss = Color(0xFFFF647C)
    val warning = Color(0xFFFFC66D)
    val info = Color(0xFF35DDE0)
}

private val AfaTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.35).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

private val AfaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.sp),
    small = RoundedCornerShape(12.sp),
    medium = RoundedCornerShape(16.sp),
    large = RoundedCornerShape(20.sp),
    extraLarge = RoundedCornerShape(26.sp)
)

@Composable
fun AfaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AfaColorSchemes.create(darkTheme),
        typography = AfaTypography,
        shapes = AfaShapes,
        content = content
    )
}
