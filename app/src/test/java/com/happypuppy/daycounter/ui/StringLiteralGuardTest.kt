package com.happypuppy.daycounter.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guard against user-facing text creeping back into code (PLAN.md UX-7): UI strings live in
 * res/values/strings.xml. Fails when a Compose `Text("...")`, `text = "..."`,
 * `contentDescription = "..."` and similar literals with a letter in them appear in app/src/main.
 *
 * Narrow allow-list ([ALLOWED_LITERALS]): animation `label` values, which are Compose tooling
 * names and never shown. Known limit: a literal that reaches the UI through a variable or an
 * `if`/`when` expression is not seen; review those by eye.
 */
class StringLiteralGuardTest {

    @Test
    fun `no user-facing string literals in app main sources`() {
        val root = File("src/main/java")
        assertTrue("run from the app module directory, missing ${root.absolutePath}", root.isDirectory)

        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> findUserFacingLiterals(file.readText()).map { "${file.path}: $it" } }
            .toList()

        assertEquals(
            "Move these to res/values/strings.xml (or extend the allow-list with a reason):\n" +
                violations.joinToString("\n"),
            emptyList<String>(),
            violations
        )
    }

    @Test
    fun `detector flags text and content description literals`() {
        val source = """
            Text("Hello")
            Text(
                "Wrapped"
            )
            Text(text = "Named")
            Icon(Icons.Default.Add, contentDescription = "Add")
            DropdownMenuItem(text = { Text("Item") })
            Modifier.semantics { onClick(label = "Do it") { true } }
            CustomAccessibilityAction("Delete") { true }
            Surface(title = "T")
            Snackbar(actionLabel = "Undo")
            label = "Tooling name"
        """.trimIndent()

        val found = findUserFacingLiterals(source)

        assertEquals(
            listOf("Hello", "Wrapped", "Named", "Add", "Item", "Do it", "Delete", "T", "Undo", "Tooling name"),
            found
        )
    }

    @Test
    fun `detector ignores resources, symbols and the allow-list`() {
        val source = """
            Text(stringResource(R.string.main_title))
            Text(text = categoryDisplayName(category))
            Text("+")
            Text("")
            contentDescription = null
            label = "Alpha Animation"
            Log.d("Tag", "message with words")
            val key = "Personal"
        """.trimIndent()

        assertEquals(emptyList<String>(), findUserFacingLiterals(source))
    }

    private companion object {
        /** Compose animation labels (debug tooling names, never displayed). */
        val ALLOWED_LITERALS = setOf("Alpha Animation", "SwipeDeleteBackground")

        // A literal counts only if it contains a letter: "+" or "" are symbols, not copy.
        const val LITERAL = "\"([^\"\\n]*[A-Za-z][^\"\\n]*)\""
        val PATTERNS = listOf(
            Regex("""\bText\(\s*$LITERAL"""),
            Regex("""\b(?:text|title|description|placeholder|message|actionLabel|onClickLabel|contentDescription|label|supportingText|hint)\s*=\s*$LITERAL"""),
            Regex("""\bonClick\(\s*label\s*=\s*$LITERAL"""),
            Regex("""\bCustomAccessibilityAction\(\s*$LITERAL""")
        )

        fun findUserFacingLiterals(source: String): List<String> =
            PATTERNS.flatMap { pattern -> pattern.findAll(source).map { it.groups[1]!!.range.first to it.groupValues[1] } }
                .distinct()
                .sortedBy { it.first }
                .map { it.second }
                .filterNot { it in ALLOWED_LITERALS }
    }
}
