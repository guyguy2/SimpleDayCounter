package com.happypuppy.daycounter.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The system splash and the window theme use res colors; the first Compose frame uses the Compose
 * schemes. They must be the same color or the hand-over from splash to content visibly jumps
 * (UX-3). Reads the XML directly, like PreferenceEnumsTest does for strings.xml.
 */
class SplashColorsTest {

    private fun resColor(folder: String, name: String): Int {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$folder/colors.xml"))
        val nodes = doc.getElementsByTagName("color")
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.attributes.getNamedItem("name").nodeValue == name) {
                return Color(node.textContent.trim().removePrefix("#").toLong(16) or 0xFF000000).toArgb()
            }
        }
        error("color $name not found in $folder/colors.xml")
    }

    @Test
    fun `light window background equals the Compose light background`() {
        assertEquals(lightColorScheme().background.toArgb(), resColor("values", "window_background"))
    }

    @Test
    fun `dark window background equals the Compose dark background`() {
        assertEquals(DarkBackground.toArgb(), resColor("values-night", "window_background"))
    }

    @Test
    fun `dark splash icon background equals the dark surface`() {
        assertEquals(DarkSurface.toArgb(), resColor("values-night", "splash_icon_background"))
    }
}
