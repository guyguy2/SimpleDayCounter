package com.happypuppy.daycounter.ui.components

import com.happypuppy.daycounter.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryNamesTest {

    @Test
    fun `every default category has its own display name resource`() {
        val ids = defaultCategories.map { defaultCategoryNameRes(it) }
        assertEquals(defaultCategories.size, ids.filterNotNull().toSet().size)
    }

    @Test
    fun `default categories map by their stored key`() {
        assertEquals(R.string.category_personal, defaultCategoryNameRes("Personal"))
        assertEquals(R.string.category_work, defaultCategoryNameRes("Work"))
        assertEquals(R.string.category_health, defaultCategoryNameRes("Health"))
        assertEquals(R.string.category_finance, defaultCategoryNameRes("Finance"))
    }

    @Test
    fun `custom and unknown categories have no resource and show as stored`() {
        assertNull(defaultCategoryNameRes("Hobbies"))
        assertNull(defaultCategoryNameRes("personal"))
        assertNull(defaultCategoryNameRes(""))
    }
}
