package lt.zygiai.kompasas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import lt.zygiai.kompasas.data.CompassThemeId

@Immutable
data class CompassPalette(
    val background: Color,
    val surface: Color,
    val dialFace: Color,
    val dialRing: Color,
    val tickMajor: Color,
    val tickMinor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val north: Color,
    val south: Color,
    val target: Color,
    val onCourse: Color,
    val warning: Color,
    val topographicLines: Color,
    val isTopographic: Boolean = false,
    val isClassic: Boolean = false
)

private val hiking = CompassPalette(
    background = Color(0xFF0A0F17), surface = Color(0xFF182330),
    dialFace = Color(0xFF172330), dialRing = Color(0xFFD5DEE8),
    tickMajor = Color(0xFFE8EEF5), tickMinor = Color(0xFF8593A2),
    textPrimary = Color(0xFFF3F6FA), textSecondary = Color(0xFFA6B1BD),
    north = Color(0xFFFF4B50), south = Color(0xFFE7EDF5),
    target = Color(0xFF4AD89B), onCourse = Color(0xFF4AD89B),
    warning = Color(0xFFFFB347), topographicLines = Color(0xFF26394C)
)

val LocalCompassPalette = staticCompositionLocalOf { hiking }

private fun paletteFor(id: CompassThemeId): CompassPalette = when (id) {
    CompassThemeId.HIKING -> hiking
    CompassThemeId.NIGHT -> hiking.copy(
        background = Color(0xFF05080D), surface = Color(0xFF101720),
        dialFace = Color(0xFF121B25), dialRing = Color(0xFFB8C3D1),
        target = Color(0xFF53E3A4), onCourse = Color(0xFF53E3A4)
    )
    CompassThemeId.MINIMAL -> hiking.copy(
        background = Color(0xFFF3F5F7), surface = Color.White,
        dialFace = Color(0xFFE5EAF0), dialRing = Color(0xFF4E5965),
        tickMajor = Color(0xFF1A2128), tickMinor = Color(0xFF7C8792),
        textPrimary = Color(0xFF151B21), textSecondary = Color(0xFF5F6872),
        south = Color(0xFF2C3947), topographicLines = Color.Transparent
    )
    CompassThemeId.TOPOGRAPHIC -> hiking.copy(
        background = Color(0xFF182018), surface = Color(0xFF252F24),
        dialFace = Color(0xFF202A1F), dialRing = Color(0xFFE4D9B9),
        textPrimary = Color(0xFFF1E8CF), textSecondary = Color(0xFFB7AF98),
        topographicLines = Color(0xFF41523C), isTopographic = true
    )
    CompassThemeId.CLASSIC -> hiking.copy(
        background = Color(0xFF25180F), surface = Color(0xFF39261A),
        dialFace = Color(0xFFE9D7AE), dialRing = Color(0xFF8D6330),
        tickMajor = Color(0xFF352419), tickMinor = Color(0xFF8C765A),
        textPrimary = Color(0xFFF8ECD4), textSecondary = Color(0xFFD0BA97),
        north = Color(0xFFC43A34), south = Color(0xFF2D493B), isClassic = true
    )
}

@Composable
fun ZygioKompasasTheme(themeId: CompassThemeId, content: @Composable () -> Unit) {
    val palette = paletteFor(themeId)
    val scheme = if (themeId == CompassThemeId.MINIMAL) {
        lightColorScheme(
            primary = palette.target,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    } else {
        darkColorScheme(
            primary = palette.target,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalCompassPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
