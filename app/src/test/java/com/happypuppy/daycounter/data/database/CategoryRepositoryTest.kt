package com.happypuppy.daycounter.data.database

import com.happypuppy.daycounter.ui.screens.FakeCategoryDao
import com.happypuppy.daycounter.ui.screens.FakeItemDao
import com.happypuppy.daycounter.ui.screens.FilterOption
import com.happypuppy.daycounter.ui.screens.FilterState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CategoryRepositoryTest {

    private lateinit var itemDao: FakeItemDao
    private lateinit var categoryDao: FakeCategoryDao
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        itemDao = FakeItemDao()
        categoryDao = FakeCategoryDao(itemDao)
    }

    @Test
    fun `ensureInitialized seeds 4 defaults and imports 0-event legacy custom categories once`() = runBlocking {
        var nextId = 1
        val prefs = InMemoryCategoryMigrationPreferences(
            initialCustomCategories = setOf("Books", "Travel", "  "),
            initialMigrated = false,
            initialFilterState = FilterState(FilterOption.CATEGORY, "Travel")
        )
        // Simulate MIGRATION_4_5 already having promoted "Travel" from v4 items.
        categoryDao.insertCategories(DEFAULT_CATEGORIES)
        categoryDao.insertCategory(Category(id = "migrated-travel", name = "Travel", isDefault = false))

        val repo = CategoryRepository(
            categoryDao = categoryDao,
            itemDao = itemDao,
            migrationPreferences = prefs,
            clock = fixedClock,
            idGenerator = { "gen-${nextId++}" }
        )

        repo.ensureInitialized()
        // Second call must be a no-op for DataStore import.
        repo.ensureInitialized()

        assertTrue(prefs.categoriesMigratedToRoomFlow.first())
        val categories = repo.observeCategories().first()
        assertEquals(6, categories.size)
        assertEquals(listOf("personal", "work", "health", "finance"), categories.take(4).map { it.id })
        assertTrue(categories.any { it.name == "Books" && it.id == "gen-1" })
        assertEquals(1, categories.count { it.name.equals("Travel", ignoreCase = true) })
        // Legacy filter_category name "Travel" resolved to its Room Category.id.
        assertEquals(FilterState(FilterOption.CATEGORY, "migrated-travel"), prefs.filterStateFlow.first())
    }

    @Test
    fun `ensureInitialized resets orphaned legacy filter_category to ALL`() = runBlocking {
        val prefs = InMemoryCategoryMigrationPreferences(
            initialCustomCategories = emptySet(),
            initialMigrated = false,
            initialFilterState = FilterState(FilterOption.CATEGORY, "DeletedInV4")
        )
        val repo = CategoryRepository(categoryDao, itemDao, migrationPreferences = prefs)

        repo.ensureInitialized()

        assertEquals(FilterState(FilterOption.ALL, null), prefs.filterStateFlow.first())
    }

    @Test
    fun `addCustomCategory normalizes name and rejects duplicates case-insensitively`() = runBlocking {
        var nextId = 1
        val repo = CategoryRepository(categoryDao, itemDao, idGenerator = { "custom-${nextId++}" })

        val added = repo.addCustomCategory("  side   projects  ")
        assertTrue(added is AddCategoryResult.Added)
        val created = (added as AddCategoryResult.Added).category
        assertEquals("Side Projects", created.name)
        assertEquals("custom-1", created.id)

        val duplicateCustom = repo.addCustomCategory("SIDE PROJECTS")
        assertTrue(duplicateCustom is AddCategoryResult.Duplicate)
        assertEquals(created.id, (duplicateCustom as AddCategoryResult.Duplicate).existing.id)

        val duplicateDefault = repo.addCustomCategory("work")
        assertTrue(duplicateDefault is AddCategoryResult.Duplicate)
        assertEquals(WORK_CATEGORY_ID, (duplicateDefault as AddCategoryResult.Duplicate).existing.id)

        assertEquals(AddCategoryResult.Invalid, repo.addCustomCategory("   "))
    }

    @Test
    fun `deleteCustomCategory refuses default categories`() = runBlocking {
        val repo = CategoryRepository(categoryDao, itemDao)
        repo.ensureInitialized()

        assertNull(repo.deleteCustomCategory(DEFAULT_CATEGORY_ID))
        assertNull(repo.deleteCustomCategory(WORK_CATEGORY_ID))
        assertEquals(4, categoryDao.currentCategories.size)
    }

    @Test
    fun `deleteCustomCategory reassigns affected items to Personal and restore only moves those items back`() =
        runBlocking {
            var nextId = 1
            val repo = CategoryRepository(categoryDao, itemDao, idGenerator = { "cat-${nextId++}" })
            val travel = (repo.addCustomCategory("Travel") as AddCategoryResult.Added).category

            itemDao.insertItem(Item(id = 1, name = "Flight", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 1)))
            itemDao.insertItem(Item(id = 2, name = "Hotel", categoryId = travel.id, targetDate = LocalDate.of(2026, 7, 2)))
            itemDao.insertItem(Item(id = 3, name = "Existing Personal", categoryId = DEFAULT_CATEGORY_ID, targetDate = LocalDate.of(2026, 7, 3)))

            val snapshot = repo.deleteCustomCategory(travel.id)
            assertNotNull(snapshot)
            assertEquals(setOf(1, 2), snapshot!!.entries.single().affectedItemIds.toSet())
            assertNull(categoryDao.getCategoryById(travel.id))
            assertTrue(itemDao.currentItems.all { it.categoryId == DEFAULT_CATEGORY_ID })

            // Add another Personal item after deletion; Undo must NOT move it to Travel.
            itemDao.insertItem(Item(id = 4, name = "New Personal", categoryId = DEFAULT_CATEGORY_ID, targetDate = LocalDate.of(2026, 7, 4)))

            repo.restoreDeletedCategories(snapshot)

            val restoredTravel = categoryDao.getCategoryById(travel.id)
            assertNotNull(restoredTravel)
            assertEquals("Travel", restoredTravel!!.name)
            assertEquals(travel.id, itemDao.currentItems.first { it.id == 1 }.categoryId)
            assertEquals(travel.id, itemDao.currentItems.first { it.id == 2 }.categoryId)
            assertEquals(DEFAULT_CATEGORY_ID, itemDao.currentItems.first { it.id == 3 }.categoryId)
            assertEquals(DEFAULT_CATEGORY_ID, itemDao.currentItems.first { it.id == 4 }.categoryId)
        }

    @Test
    fun `deleteAllCustomCategories reassigns all custom items and restore brings all categories and assignments back`() =
        runBlocking {
            var nextId = 1
            val repo = CategoryRepository(categoryDao, itemDao, idGenerator = { "cat-${nextId++}" })
            val travel = (repo.addCustomCategory("Travel") as AddCategoryResult.Added).category
            val books = (repo.addCustomCategory("Books") as AddCategoryResult.Added).category

            itemDao.insertItem(Item(id = 10, name = "Trip", categoryId = travel.id, targetDate = LocalDate.of(2026, 8, 1)))
            itemDao.insertItem(Item(id = 20, name = "Novel", categoryId = books.id, targetDate = LocalDate.of(2026, 8, 2)))
            itemDao.insertItem(Item(id = 30, name = "Meeting", categoryId = WORK_CATEGORY_ID, targetDate = LocalDate.of(2026, 8, 3)))

            val snapshot = repo.deleteAllCustomCategories()
            assertNotNull(snapshot)
            assertTrue(snapshot!!.isDeleteAll)
            assertEquals(2, snapshot.totalAffectedItems)
            assertTrue(categoryDao.getCustomCategories().isEmpty())
            assertEquals(DEFAULT_CATEGORY_ID, itemDao.currentItems.first { it.id == 10 }.categoryId)
            assertEquals(DEFAULT_CATEGORY_ID, itemDao.currentItems.first { it.id == 20 }.categoryId)
            assertEquals(WORK_CATEGORY_ID, itemDao.currentItems.first { it.id == 30 }.categoryId)

            repo.restoreDeletedCategories(snapshot)

            assertEquals(2, categoryDao.getCustomCategories().size)
            assertEquals(travel.id, itemDao.currentItems.first { it.id == 10 }.categoryId)
            assertEquals(books.id, itemDao.currentItems.first { it.id == 20 }.categoryId)
            assertEquals(WORK_CATEGORY_ID, itemDao.currentItems.first { it.id == 30 }.categoryId)
        }

    @Test
    fun `addCustomCategory and ensureInitialized reject non-ASCII Unicode duplicates case-insensitively`() =
        runBlocking {
            var nextId = 1
            val prefs = InMemoryCategoryMigrationPreferences(
                initialCustomCategories = setOf("CAFÉ", "café"),
                initialMigrated = false
            )
            val repo = CategoryRepository(
                categoryDao = categoryDao,
                itemDao = itemDao,
                migrationPreferences = prefs,
                clock = fixedClock,
                idGenerator = { "cat-${nextId++}" }
            )

            repo.ensureInitialized()
            val customAfterInit = categoryDao.getCustomCategories()
            assertEquals(1, customAfterInit.size)
            assertEquals("CAFÉ", customAfterInit.single().name)

            val duplicate = repo.addCustomCategory("café")
            assertTrue(duplicate is AddCategoryResult.Duplicate)
            assertEquals(customAfterInit.single().id, (duplicate as AddCategoryResult.Duplicate).existing.id)
            assertEquals(1, categoryDao.getCustomCategories().size)
        }

    @Test
    fun `restoreDeletedCategories merges into re-added same-name category without creating duplicates`() =
        runBlocking {
            var nextId = 1
            val repo = CategoryRepository(categoryDao, itemDao, idGenerator = { "cat-${nextId++}" })
            val oldTravel = (repo.addCustomCategory("Travel") as AddCategoryResult.Added).category
            itemDao.insertItem(Item(id = 1, name = "Old Flight", categoryId = oldTravel.id, targetDate = LocalDate.of(2026, 7, 1)))

            val snapshot = repo.deleteCustomCategory(oldTravel.id)!!
            val newTravel = (repo.addCustomCategory("travel") as AddCategoryResult.Added).category
            itemDao.insertItem(Item(id = 2, name = "New Flight", categoryId = newTravel.id, targetDate = LocalDate.of(2026, 7, 2)))

            val mapping = repo.restoreDeletedCategories(snapshot)

            val customCategories = categoryDao.getCustomCategories()
            assertEquals(1, customCategories.size)
            assertEquals(newTravel.id, customCategories.single().id)
            assertEquals(newTravel.id, mapping[oldTravel.id])
            assertEquals(newTravel.id, itemDao.currentItems.first { it.id == 1 }.categoryId)
            assertEquals(newTravel.id, itemDao.currentItems.first { it.id == 2 }.categoryId)
        }
}
