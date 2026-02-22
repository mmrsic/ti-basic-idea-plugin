# TI-Basic IDEA Plugin

An IntelliJ IDEA plugin that adds language support for **TI-Basic** and **TI Extended Basic** —
the BASIC dialects of the Texas Instruments TI-99/4 and TI-99/4A home computers.

## Features

- Syntax highlighting for TI-Basic source files (`.ti-basic`, `.tibasic`, `.ti.bas`)
- Line-based parser: each valid line consists of a line number (1–32767) followed by a `PRINT` statement
- Lines that do not match the grammar are treated as comment lines
- Leading and trailing whitespace on valid lines is recognized and ignored
- Code completion for TI-Basic keywords
- Custom file icon for TI-Basic source files

## Project structure

```
src/
├── main/
│   ├── kotlin/com/github/mmrsic/idea/plugins/tibasic/
│   │   ├── TiBasicLanguage.kt              Language singleton
│   │   ├── TiBasicFileType.kt              LanguageFileType object
│   │   ├── TiBasicLexer.kt                 Line-based lexer
│   │   ├── TiBasicParser.kt                PsiParser implementation
│   │   ├── TiBasicParserDefinition.kt      ParserDefinition
│   │   ├── TiBasicTokenTypes.kt            Token and node element types
│   │   ├── TiBasicSyntaxHighlighter.kt     Syntax highlighter
│   │   ├── TiBasicSyntaxHighlighterFactory.kt
│   │   ├── TiBasicCompletionContributor.kt Keyword completion
│   │   ├── TiBasicKeywords.kt              Keyword list
│   │   ├── TiBasicFileIconProvider.kt      File icon
│   │   └── psi/
│   │       ├── TiBasicFile.kt              PSI file element
│   │       └── TiBasicPsiElements.kt       PSI node elements (Line, PrintStatement, CommentLine)
│   └── resources/META-INF/plugin.xml       Plugin descriptor
└── test/
    └── kotlin/com/github/mmrsic/idea/plugins/tibasic/
        ├── TiBasicParserTest.kt            Parser happy-path and error tests
        ├── TiBasicSyntaxHighlightingTest.kt Lexer / highlighter tests
        ├── TiBasicCompletionTest.kt        Keyword completion tests
        └── IconLoadTest.kt                 Icon loading tests
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

## Grammar specification

A **valid line** matches:

```
[whitespace] <lineNumber> <whitespace> PRINT [whitespace] [argument] [whitespace]
```

- `lineNumber` — integer in the range **1–32767** (case-insensitive `PRINT` keyword)
- Leading and trailing whitespace on a valid line is tokenized as `WHITE_SPACE` and ignored by the parser
- Every other line is tokenized as a single `COMMENT` token and wrapped in a `CommentLine` PSI node

## Running tests

```bash
./gradlew test
```

Test reports are written to `build/reports/tests/test/index.html`.

## Contributing

1. Fork the repository and create a feature branch.
2. Follow the coding conventions in [`.github/copilot-instructions.md`](.github/copilot-instructions.md).
3. Add or update tests for every non-trivial change.
4. Ensure `./gradlew build` and `./gradlew verifyPlugin` pass before opening a pull request.
5. Keep commits small and focused; prefix messages with `feat:`, `fix:`, `chore:`, etc.

## License

This project is licensed under the **GNU General Public License v3.0**.
See the [LICENSE](LICENSE) file for details.
