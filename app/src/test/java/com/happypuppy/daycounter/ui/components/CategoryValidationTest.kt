package com.happypuppy.daycounter.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryValidationTest {

    @Test
    fun `normalizeCategoryName trims, collapses spaces, and title-cases words`() {
        assertEquals("Side Project", normalizeCategoryName("  side   project  "))
        assertEquals("Book Club", normalizeCategoryName("\tbook\t  club\n"))
        assertEquals("Travel", normalizeCategoryName("travel"))
        assertEquals("", normalizeCategoryName("   \t  "))
    }

    @Test
    fun `isDuplicateCategory blocks default categories regardless of case or whitespace`() {
        assertTrue(isDuplicateCategory("Personal"))
        assertTrue(isDuplicateCategory("personal"))
        assertTrue(isDuplicateCategory("  WORK  "))
        assertTrue(isDuplicateCategory("hEaLtH"))
        assertTrue(isDuplicateCategory("finance"))
    }

    @Test
    fun `isDuplicateCategory blocks existing custom categories case-insensitively`() {
        val existing = setOf("Travel", "Book Club")
        assertTrue(isDuplicateCategory("travel", existing))
        assertTrue(isDuplicateCategory("  book   club ", existing))
        assertFalse(isDuplicateCategory("Hobbies", existing))
    }

    @Test
    fun `isDuplicateCategory returns false for blank input`() {
        assertFalse(isDuplicateCategory("", setOf("Travel")))
        assertFalse(isDuplicateCategory("   ", setOf("Travel")))
    }
}
