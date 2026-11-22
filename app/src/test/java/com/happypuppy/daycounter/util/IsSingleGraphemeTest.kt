package com.happypuppy.daycounter.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [isSingleGrapheme] covering empty input, ASCII, single-codepoint emojis,
 * variation-selector emojis, regional flags, skin-tone modifiers, and ZWJ sequences.
 */
class IsSingleGraphemeTest {

    @Test
    fun `empty string is allowed`() {
        assertTrue(isSingleGrapheme(""))
    }

    @Test
    fun `single ASCII character is allowed`() {
        assertTrue(isSingleGrapheme("a"))
    }

    @Test
    fun `multiple ASCII characters are rejected`() {
        assertFalse(isSingleGrapheme("ab"))
    }

    @Test
    fun `basic emoji is allowed`() {
        assertTrue(isSingleGrapheme("🎉"))
    }

    @Test
    fun `variation selector emojis are allowed`() {
        assertTrue(isSingleGrapheme("❤️"))
        assertTrue(isSingleGrapheme("☀️"))
    }

    @Test
    fun `regional indicator flag emoji is allowed`() {
        assertTrue(isSingleGrapheme("🇺🇸"))
    }

    @Test
    fun `skin tone modifier emoji is allowed`() {
        assertTrue(isSingleGrapheme("👍🏽"))
    }

    @Test
    fun `ZWJ family sequence emoji is allowed`() {
        assertTrue(isSingleGrapheme("👨‍👩‍👧"))
    }

    @Test
    fun `two emojis or mixed text and emoji are rejected`() {
        assertFalse(isSingleGrapheme("🎉🎊"))
        assertFalse(isSingleGrapheme("🇺🇸🇬🇧"))
        assertFalse(isSingleGrapheme("a❤️"))
    }
}
