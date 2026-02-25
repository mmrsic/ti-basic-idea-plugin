# TI-Basic IDEA Plugin

An IntelliJ IDEA plugin that adds language support for **TI-Basic** and **TI Extended Basic** —
the BASIC dialects of the Texas Instruments TI-99/4 and TI-99/4A home computers.

## Features

### Language support

- Recognises TI-Basic source files with extensions `.ti-basic`, `.tibasic`, `.ti.bas`
- Custom file icon for TI-Basic source files
- Each source line must start with a **line number** in the range **1–32767**; lines without a number are flagged as
  errors

### Supported statements

| Statement           | Description                                               |
|---------------------|-----------------------------------------------------------|
| `LET`               | Assign a value to a variable (`LET` keyword is optional)  |
| `PRINT`             | Output values or text                                     |
| `REM`               | Remark / comment                                          |
| `END`               | Halt program execution (by convention the last line)      |
| `STOP`              | Halt program execution (by convention used mid-program)   |
| `DELETE`            | Delete a string expression                                |
| `BREAK` / `UNBREAK` | Set or clear breakpoints at given line numbers            |
| `TRACE` / `UNTRACE` | Enable or disable execution tracing at given line numbers |

Lines whose keyword is not one of the above are flagged as unknown statements.

### Expressions

- **Numeric literals** — integers, decimals, scientific notation (e.g. `1.5E-3`)
- **String literals** — double-quoted (e.g. `"HELLO"`)
- **Numeric variables** — single letter optionally followed by a digit, up to 14 characters (e.g. `A`, `X1`)
- **String variables** — same naming rules as numeric, ending with `$` (e.g. `A$`, `STR$`)
- **Array subscripts** — up to 3 dimensions (e.g. `A(1)`, `B(1,2)`, `C(1,2,3)`)
- **Operators** — `+`, `-`, `*`, `/`, `^` (power), `&` (string concatenation)
- **Comparisons** — `=`, `<>`, `<`, `>`, `<=`, `>=`
- Parentheses for grouping; `^` is right-associative, all others left-associative

### Error and warning annotations

The annotator inspects every file and highlights:

| Severity | Check                                                                            |
|----------|----------------------------------------------------------------------------------|
| Error    | Line number out of range (< 1 or > 32767)                                        |
| Error    | Duplicate line numbers                                                           |
| Warning  | Line numbers not in ascending order                                              |
| Error    | Line without a line number                                                       |
| Error    | Unknown statement keyword                                                        |
| Error    | Variable name that is a reserved keyword or command                              |
| Error    | Conflicting variable usage (scalar vs. array)                                    |
| Error    | Empty subscript or more than 3 subscript dimensions                              |
| Error    | Type mismatch (numeric value where string is required, or vice versa)            |
| Error    | String-number mismatch in LET assignment (variable type differs from expression) |
| Warning  | Reference to an undefined line number in BREAK/UNBREAK/TRACE/UNTRACE             |

### Code actions

**Format File** (`Tools › TI-Basic › Format`)

- Converts all keywords outside string literals to uppercase
- Removes extraneous whitespace outside string literals
- Normalises exactly one space between the line number and the first keyword
- Can be applied to the whole file or to the current selection

**Resequence Line Numbers** (`Tools › TI-Basic › Resequence`)

- Renumbers all lines with a configurable start number and step
- A dialog lets you choose both values before applying
- Also available as a quick-fix on duplicate-line-number and out-of-order warnings

### Editor assistance

- **Keyword completion** — autocomplete suggestions for all TI-Basic keywords while typing (case-insensitive)
- **Shift+Enter** — inserts a new line and automatically prepends the next logical line number

## Project structure

```
src/
├── main/
│   ├── kotlin/com/github/mmrsic/idea/plugins/tibasic/
│   │   ├── action/
│   │   │   ├── TiBasicFileAction.kt            Abstract base for TI-Basic menu actions
│   │   │   ├── format/
│   │   │   │   ├── FormatAction.kt             "Format" menu action
│   │   │   │   └── FormatCode.kt               Formatting logic
│   │   │   └── resequence/
│   │   │       ├── ResequenceAction.kt         "Resequence" menu action
│   │   │       ├── ResequenceLineNumbers.kt    Renumbering logic
│   │   │       ├── ResequenceOptionsDialog.kt  Start/step dialog
│   │   │       └── ResequenceQuickFix.kt       Quick-fix integration
│   │   ├── editor/
│   │   │   ├── TiBasicCompletionContributor.kt Keyword completion
│   │   │   └── TiBasicShiftEnterHandler.kt     Shift+Enter new-line handler
│   │   ├── ext/
│   │   │   └── ASTNodeExtensions.kt            Kotlin extensions on framework classes
│   │   ├── highlight/
│   │   │   ├── TiBasicAnnotator.kt             Semantic error/warning annotations
│   │   │   ├── TiBasicSyntaxHighlighter.kt     Token-based syntax colours
│   │   │   └── TiBasicSyntaxHighlighterFactory.kt
│   │   ├── lang/
│   │   │   ├── TiBasicFileIconProvider.kt      Custom file icon
│   │   │   ├── TiBasicFileType.kt              LanguageFileType object
│   │   │   ├── TiBasicKeywords.kt              Keyword and command lists
│   │   │   └── TiBasicLanguage.kt              Language singleton
│   │   ├── lexer/
│   │   │   ├── TiBasicLexer.kt                 Line-based lexer
│   │   │   └── TiBasicTokenTypes.kt            Token element types
│   │   ├── parser/
│   │   │   ├── TiBasicNodeTypes.kt             AST node element types
│   │   │   ├── TiBasicParser.kt                PsiParser implementation
│   │   │   └── TiBasicParserDefinition.kt      ParserDefinition
│   │   ├── psi/
│   │   │   ├── TiBasicFile.kt                  PSI file root element
│   │   │   └── TiBasicPsiElements.kt           PSI node elements (Line, statements, expressions)
│   │   └── util/
│   │       └── PsiFileUtils.kt                 Document write-action helpers
│   └── resources/META-INF/plugin.xml           Plugin descriptor
└── test/
    └── kotlin/com/github/mmrsic/idea/plugins/tibasic/
        ├── TiBasicTestBase.kt                  Shared test base class
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

## Prerequisites

| Tool          | Minimum version          |
|---------------|--------------------------|
| JDK           | 21                       |
| Gradle        | 8.x (via wrapper)        |
| IntelliJ IDEA | 2025.2 (target platform) |

## Building

```bash
# Compile and run all tests
./gradlew build

# Run tests only
./gradlew test

# Launch a sandbox IDE instance with the plugin installed
./gradlew runIde

# Verify plugin compatibility against recommended IDE versions
./gradlew verifyPlugin
```

Test reports are written to `build/reports/tests/test/index.html`.

## Developer documentation

| Document                                               | Contents                                                                                |
|--------------------------------------------------------|-----------------------------------------------------------------------------------------|
| [`docs/architecture.md`](docs/architecture.md)         | Package map, data-flow diagram, annotator checks, threading model, key design decisions |
| [`docs/grammar.md`](docs/grammar.md)                   | Full EBNF grammar, token reference, valid/invalid example lines                         |
| [`docs/extension-points.md`](docs/extension-points.md) | All registered IntelliJ extension points and how to add new ones                        |
| [`docs/testing.md`](docs/testing.md)                   | Test setup, base class, writing parser/annotator/action tests, running the sandbox      |

## Contributing

1. Fork the repository and create a feature branch.
2. Follow the coding conventions in [`.github/copilot-instructions.md`](.github/copilot-instructions.md).
3. Add or update tests for every non-trivial change.
4. Keep the `docs/` files and this README in sync with your changes (see conventions).
5. Ensure `./gradlew build` and `./gradlew verifyPlugin` pass before opening a pull request.
6. Keep commits small and focused; prefix messages with `feat:`, `fix:`, `chore:`, etc.

## License

This project is licensed under the **GNU General Public License v3.0**.
See the [LICENSE](LICENSE) file for details.
