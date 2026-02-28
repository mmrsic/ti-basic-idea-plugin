# Testing guide

This document explains how to write, run, and organise tests for the TI-Basic plugin.

## Test framework

Tests use the **IntelliJ Platform Test Framework** (`BasePlatformTestCase` and friends).
This is a light-weight in-process test setup that boots a minimal IntelliJ environment
without a full IDE sandbox.

Gradle dependency is provided by the `org.jetbrains.intellij` Gradle plugin; no explicit
test-framework dependency declaration is required in `build.gradle.kts`.

## Running tests

```bash
# Run all tests
./gradlew test

# Run tests and open the HTML report
./gradlew test && open build/reports/tests/test/index.html
```

## Test base class

All tests extend `TiBasicTestBase` (in the root test package `tibasic`):

```kotlin
abstract class TiBasicTestBase : BasePlatformTestCase() {
    fun configureFile(text: String): TiBasicFile {
        myFixture.configureByText("test.tibasic", text)
        return myFixture.file as TiBasicFile
    }
}
```

`configureFile` creates an in-memory `.tibasic` file, runs the full lexer+parser pipeline,
and returns the typed `TiBasicFile` PSI root. Use it as the starting point for any test
that inspects the PSI tree, annotations, or editor behaviour.

## Test package layout

Test files mirror the main source package structure and live at:

```
src/test/kotlin/com/github/mmrsic/idea/plugins/tibasic/
├── TiBasicTestBase.kt                  (stays at root — shared by all sub-packages)
├── action/
│   ├── format/
│   │   ├── FormatActionTest.kt
│   │   └── FormatCodeTest.kt
│   └── resequence/
│       ├── ResequenceLineNumbersTest.kt
│       └── ResequenceQuickFixTest.kt
├── editor/
│   ├── TiBasicCompletionTest.kt
│   └── TiBasicShiftEnterHandlerTest.kt
├── highlight/
│   ├── TiBasicAnnotatorTest.kt
│   └── TiBasicSyntaxHighlightingTest.kt
├── lang/
│   └── IconLoadTest.kt
└── parser/
    └── TiBasicParserTest.kt
```

Each test file is in the same package as the class(es) it tests. `TiBasicTestBase` stays
at the root because it is imported by tests in multiple sub-packages.

## Writing tests

### Parser tests (`parser/TiBasicParserTest`)

Call `configureFile(text)` and then inspect the PSI tree structure:

```kotlin
class TiBasicParserTest : TiBasicTestBase() {
    fun `test valid print line`() {
        val file = configureFile("100 PRINT \"HELLO\"")
        val lines = file.lines()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
        assertNotNull(lines[0].children.filterIsInstance<TiBasicPrintStatement>().firstOrNull())
    }

    fun `test invalid line without number`() {
        val file = configureFile("PRINT \"HELLO\"")
        assertEquals(0, file.lines().size)
        assertTrue(file.children.any { it is TiBasicInvalidLine })
    }
}
```

### Annotator tests (`highlight/TiBasicAnnotatorTest`)

Use `myFixture.checkHighlighting()` after configuring a file, or collect annotations
manually with `myFixture.doHighlighting()`:

```kotlin
class TiBasicAnnotatorTest : TiBasicTestBase() {
    fun `test duplicate line number`() {
        configureFile("100 PRINT \"A\"\n100 PRINT \"B\"")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description.contains("Duplicate") })
    }

    fun `test line number out of range`() {
        configureFile("99999 PRINT \"X\"")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.severity == HighlightSeverity.ERROR })
    }
}
```

### Syntax highlighting tests (`highlight/TiBasicSyntaxHighlightingTest`)

Use `myFixture.testHighlighting(...)` with inline `<error>`/`<warning>` markers,
or check token highlight attributes directly via `myFixture.doHighlighting()`.

### Action tests (`action/format/`, `action/resequence/`)

Invoke the action via `myFixture.performEditorAction(actionId)` or call the logic
class directly for unit-level testing:

```kotlin
class FormatCodeTest : TiBasicTestBase() {
    fun `test keywords uppercased`() {
        val result = FormatCode.format("100 print \"hello\"")
        assertEquals("100 PRINT \"hello\"", result)
    }
}
```

For `ResequenceLineNumbers`, call the function under test directly and assert on
the returned text.

### Completion tests (`editor/TiBasicCompletionTest`)

```kotlin
class TiBasicCompletionTest : TiBasicTestBase() {
    fun `test print keyword completion`() {
        myFixture.configureByText("test.tibasic", "100 PR<caret>")
        myFixture.completeBasic()
        assertTrue(myFixture.lookupElementStrings?.contains("PRINT") == true)
    }

    fun `test variable completion`() {
        myFixture.configureByText("test.tibasic", "100 LET A=5\n200 <caret>")
        myFixture.completeBasic()
        assertTrue(myFixture.lookupElementStrings?.contains("A") == true)
    }
}
```

### Editor handler tests (`editor/TiBasicShiftEnterHandlerTest`)

```kotlin
class TiBasicShiftEnterHandlerTest : TiBasicTestBase() {
    fun `test shift enter adds next line number`() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertTrue(myFixture.editor.document.text.contains("\n110 "))
    }
}
```

## Minimum test coverage requirements

For every non-trivial change, provide at least:

1. A **happy-path** test (valid input, expected PSI structure or output).
2. An **error case** test (invalid input, expected annotation or error handling).
3. For actions: a test that the action produces the correct output text.

If a change touches the lexer or parser, also add or update a `TiBasicParserTest` case
covering the modified grammar rule.

## Manual sandbox testing

```bash
# Launch an IntelliJ sandbox instance with the plugin installed
./gradlew runIde
```

Open or create a `.tibasic` file and verify syntax highlighting, completion,
annotations, and actions manually before opening a pull request.

## Plugin compatibility verification (CI only)

```bash
./gradlew verifyPlugin
```

Run this in CI to verify compatibility with the target IDE version range specified in
`build.gradle.kts`. Do not run it locally on every change — it is slow.
