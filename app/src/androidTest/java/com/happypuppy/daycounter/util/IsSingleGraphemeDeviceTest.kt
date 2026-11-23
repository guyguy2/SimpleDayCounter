package com.happypuppy.daycounter.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the grapheme-cluster cases that fail on a JDK 17 host JVM (java.text.BreakIterator only
 * learned extended grapheme clusters in JDK 20) on the real Android runtime, whose BreakIterator
 * is ICU-backed. Emoji are written as escapes on purpose.
 */
@RunWith(AndroidJUnit4::class)
class IsSingleGraphemeDeviceTest {

    @Test
    fun regionalIndicatorFlagIsOneGrapheme() {
        assertTrue(isSingleGrapheme("\uD83C\uDDFA\uD83C\uDDF8"))
    }

    @Test
    fun skinToneModifierIsOneGrapheme() {
        assertTrue(isSingleGrapheme("\uD83D\uDC4D\uD83C\uDFFD"))
    }

    @Test
    fun zwjFamilyIsOneGrapheme() {
        assertTrue(isSingleGrapheme("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67"))
    }

    @Test
    fun variationSelectorHeartIsOneGrapheme() {
        assertTrue(isSingleGrapheme("\u2764\uFE0F"))
    }

    @Test
    fun twoFlagsAreRejected() {
        assertFalse(isSingleGrapheme("\uD83C\uDDFA\uD83C\uDDF8\uD83C\uDDEC\uD83C\uDDE7"))
    }
}
