package com.happypuppy.daycounter.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins every role of the two brand color schemes. The expected values were read from the schemes
 * as they were before the roles were written out explicitly (UX-10), so with dynamic color off the
 * app keeps exactly the same palette. Changing a color is allowed; do it here on purpose.
 */
class BrandColorSchemeTest {

    private fun roles(s: ColorScheme): Map<String, Int> = mapOf(
        "primary" to s.primary.toArgb(),
        "onPrimary" to s.onPrimary.toArgb(),
        "primaryContainer" to s.primaryContainer.toArgb(),
        "onPrimaryContainer" to s.onPrimaryContainer.toArgb(),
        "inversePrimary" to s.inversePrimary.toArgb(),
        "secondary" to s.secondary.toArgb(),
        "onSecondary" to s.onSecondary.toArgb(),
        "secondaryContainer" to s.secondaryContainer.toArgb(),
        "onSecondaryContainer" to s.onSecondaryContainer.toArgb(),
        "tertiary" to s.tertiary.toArgb(),
        "onTertiary" to s.onTertiary.toArgb(),
        "tertiaryContainer" to s.tertiaryContainer.toArgb(),
        "onTertiaryContainer" to s.onTertiaryContainer.toArgb(),
        "background" to s.background.toArgb(),
        "onBackground" to s.onBackground.toArgb(),
        "surface" to s.surface.toArgb(),
        "onSurface" to s.onSurface.toArgb(),
        "surfaceVariant" to s.surfaceVariant.toArgb(),
        "onSurfaceVariant" to s.onSurfaceVariant.toArgb(),
        "surfaceTint" to s.surfaceTint.toArgb(),
        "inverseSurface" to s.inverseSurface.toArgb(),
        "inverseOnSurface" to s.inverseOnSurface.toArgb(),
        "error" to s.error.toArgb(),
        "onError" to s.onError.toArgb(),
        "errorContainer" to s.errorContainer.toArgb(),
        "onErrorContainer" to s.onErrorContainer.toArgb(),
        "outline" to s.outline.toArgb(),
        "outlineVariant" to s.outlineVariant.toArgb(),
        "scrim" to s.scrim.toArgb(),
        "surfaceBright" to s.surfaceBright.toArgb(),
        "surfaceDim" to s.surfaceDim.toArgb(),
        "surfaceContainer" to s.surfaceContainer.toArgb(),
        "surfaceContainerHigh" to s.surfaceContainerHigh.toArgb(),
        "surfaceContainerHighest" to s.surfaceContainerHighest.toArgb(),
        "surfaceContainerLow" to s.surfaceContainerLow.toArgb(),
        "surfaceContainerLowest" to s.surfaceContainerLowest.toArgb(),
    )

    private val expectedLight = mapOf(
        "primary" to 0xFF6650A4.toInt(),
        "onPrimary" to 0xFFFFFFFF.toInt(),
        "primaryContainer" to 0xFFEADDFF.toInt(),
        "onPrimaryContainer" to 0xFF21005D.toInt(),
        "inversePrimary" to 0xFFD0BCFF.toInt(),
        "secondary" to 0xFF625B71.toInt(),
        "onSecondary" to 0xFFFFFFFF.toInt(),
        "secondaryContainer" to 0xFFE8DEF8.toInt(),
        "onSecondaryContainer" to 0xFF1D192B.toInt(),
        "tertiary" to 0xFF7D5260.toInt(),
        "onTertiary" to 0xFFFFFFFF.toInt(),
        "tertiaryContainer" to 0xFFFFD8E4.toInt(),
        "onTertiaryContainer" to 0xFF31111D.toInt(),
        "background" to 0xFFFEF7FF.toInt(),
        "onBackground" to 0xFF1D1B20.toInt(),
        "surface" to 0xFFFEF7FF.toInt(),
        "onSurface" to 0xFF1D1B20.toInt(),
        "surfaceVariant" to 0xFFE7E0EC.toInt(),
        "onSurfaceVariant" to 0xFF49454F.toInt(),
        "surfaceTint" to 0xFF6650A4.toInt(),
        "inverseSurface" to 0xFF322F35.toInt(),
        "inverseOnSurface" to 0xFFF5EFF7.toInt(),
        "error" to 0xFFB3261E.toInt(),
        "onError" to 0xFFFFFFFF.toInt(),
        "errorContainer" to 0xFFF9DEDC.toInt(),
        "onErrorContainer" to 0xFF410E0B.toInt(),
        "outline" to 0xFF79747E.toInt(),
        "outlineVariant" to 0xFFCAC4D0.toInt(),
        "scrim" to 0xFF000000.toInt(),
        "surfaceBright" to 0xFFFEF7FF.toInt(),
        "surfaceDim" to 0xFFDED8E1.toInt(),
        "surfaceContainer" to 0xFFF3EDF7.toInt(),
        "surfaceContainerHigh" to 0xFFECE6F0.toInt(),
        "surfaceContainerHighest" to 0xFFE6E0E9.toInt(),
        "surfaceContainerLow" to 0xFFF7F2FA.toInt(),
        "surfaceContainerLowest" to 0xFFFFFFFF.toInt(),
    )

    private val expectedDark = mapOf(
        "primary" to 0xFFD0BCFF.toInt(),
        "onPrimary" to 0xFF381E72.toInt(),
        "primaryContainer" to 0xFF4E3A71.toInt(),
        "onPrimaryContainer" to 0xFFEADDFF.toInt(),
        "inversePrimary" to 0xFF6750A4.toInt(),
        "secondary" to 0xFF03DAC6.toInt(),
        "onSecondary" to 0xFF003731.toInt(),
        "secondaryContainer" to 0xFF005047.toInt(),
        "onSecondaryContainer" to 0xFFA3F7EF.toInt(),
        "tertiary" to 0xFFD0BCFF.toInt(),
        "onTertiary" to 0xFF381E72.toInt(),
        "tertiaryContainer" to 0xFF633B48.toInt(),
        "onTertiaryContainer" to 0xFFFFD8E4.toInt(),
        "background" to 0xFF121212.toInt(),
        "onBackground" to 0xFFEEEEEE.toInt(),
        "surface" to 0xFF1E1E1E.toInt(),
        "onSurface" to 0xFFEEEEEE.toInt(),
        "surfaceVariant" to 0xFF323232.toInt(),
        "onSurfaceVariant" to 0xFFBDBDBD.toInt(),
        "surfaceTint" to 0xFFD0BCFF.toInt(),
        "inverseSurface" to 0xFFE6E0E9.toInt(),
        "inverseOnSurface" to 0xFF322F35.toInt(),
        "error" to 0xFFCF6679.toInt(),
        "onError" to 0xFF601410.toInt(),
        "errorContainer" to 0xFF8C1D18.toInt(),
        "onErrorContainer" to 0xFFF9DEDC.toInt(),
        "outline" to 0xFF857A95.toInt(),
        "outlineVariant" to 0xFF49454F.toInt(),
        "scrim" to 0xFF000000.toInt(),
        "surfaceBright" to 0xFF3B383E.toInt(),
        "surfaceDim" to 0xFF141218.toInt(),
        "surfaceContainer" to 0xFF211F26.toInt(),
        "surfaceContainerHigh" to 0xFF2B2930.toInt(),
        "surfaceContainerHighest" to 0xFF36343B.toInt(),
        "surfaceContainerLow" to 0xFF1D1B20.toInt(),
        "surfaceContainerLowest" to 0xFF0F0D13.toInt(),
    )

    private fun assertScheme(expected: Map<String, Int>, actual: Map<String, Int>) {
        assertEquals(expected.keys, actual.keys)
        for ((role, argb) in expected) {
            assertEquals("%s expected #%08X but was #%08X".format(role, argb, actual.getValue(role)), argb, actual.getValue(role))
        }
    }

    @Test
    fun `light scheme keeps every role`() = assertScheme(expectedLight, roles(LightColorScheme))

    @Test
    fun `dark scheme keeps every role`() = assertScheme(expectedDark, roles(DarkColorScheme))
}
