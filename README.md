# TI-Basic IDEA Plugin

An IntelliJ IDEA plugin that adds language support for **TI-Basic** and **TI Extended Basic** —
the BASIC dialects of the Texas Instruments TI-99/4 and TI-99/4A home computers.

## Features

### Language support

- Recognizes TI-Basic source files with extensions `.ti-basic`, `.tibasic`, `.ti.bas`
- Custom file icon for TI-Basic source files
- Each source line must start with a **line number** in the range **1–32767**; lines without a number are flagged as
  errors

### Supported statements

| Statement                  | Description                                                                                           |
|----------------------------|-------------------------------------------------------------------------------------------------------|
| `LET`                      | Assign a value to a variable (`LET` keyword is optional)                                              |
| `PRINT`                    | Output values or text to screen, printer, or file; multiple expressions separated by `;`, `,`, or `:` |
| `DISPLAY`                  | Output values or text to screen only; identical syntax to `PRINT`                                     |
| `INPUT`                    | Read keyboard input into one or more variables (optional string prompt)                               |
| `READ`                     | Read values from DATA statements into one or more variables                                           |
| `DATA`                     | Supply a comma-separated list of values for `READ` statements                                         |
| `RESTORE`                  | Reset the DATA pointer (optionally to a specific line number)                                         |
| `CALL`                     | Invoke a built-in TI-Basic subprogram (see table below)                                               |
| `REM`                      | Remark / comment                                                                                      |
| `END`                      | Halt program execution (by convention the last line)                                                  |
| `STOP`                     | Halt program execution (by convention used mid-program)                                               |
| `RANDOMIZE`                | Seed the random-number generator; optional numeric expression (integer part used as seed)             |
| `GOTO` / `GO TO`           | Unconditional branch to the given line number                                                         |
| `ON … GOTO` / `ON … GO TO` | Computed branch to one of several line numbers                                                        |
| `IF … THEN … [ELSE …]`     | Conditional branch; numeric expression selects target                                                 |
| `FOR … TO … [STEP …]`      | Counted loop with numeric control variable, start, limit, and optional step                           |
| `NEXT`                     | Marks the end of the counted loop body                                                                |
| `DELETE`                   | Delete a string expression                                                                            |
| `BREAK` / `UNBREAK`        | Set or clear breakpoints at given line numbers                                                        |
| `TRACE` / `UNTRACE`        | Enable or disable execution tracing at given line numbers                                             |

Lines whose keyword is not one of the above are flagged as unknown statements.

### CALL subprograms

| Subprogram                   | Signature                                     | Description                                          |
|------------------------------|-----------------------------------------------|------------------------------------------------------|
| `CALL CLEAR`                 | No arguments                                  | Clear the screen                                     |
| `CALL SCREEN(color)`         | 1 numeric                                     | Set the screen background color                      |
| `CALL COLOR(set,fg,bg)`      | 3 numerics                                    | Set foreground/background colors for a character set |
| `CALL HCHAR(row,col,ch[,n])` | 3 numerics; 4th optional (default 1)          | Print character horizontally (optionally n times)    |
| `CALL VCHAR(row,col,ch[,n])` | 3 numerics; 4th optional (default 1)          | Print character vertically (optionally n times)      |
| `CALL GCHAR(row,col,var)`    | 3 numerics                                    | Read character at position into a variable           |
| `CALL CHAR(code,pattern$)`   | 1 numeric, 1 string                           | Define a custom character pattern                    |
| `CALL KEY(unit,key,status)`  | 3 numerics                                    | Read keyboard input                                  |
| `CALL JOYST(unit,x,y)`       | 3 numerics                                    | Read joystick input                                  |
| `CALL SOUND(dur,freq,vol…)`  | 3, 5, 7, or 9 numerics (groups of 3 per tone) | Play one to three tones simultaneously               |

### Expressions

- **Numeric literals** — integers, decimals, scientific notation (e.g. `1.5E-3`)
- **String literals** — double-quoted (e.g. `"HELLO"`)
- **Numeric variables** — 1–15 characters; starts with a letter (or `@`, `[`, `]`, `\`, `_`); remaining characters are
  letters, digits, `@`, or `_` (e.g. `A`, `X1`, `COUNTER`)
- **String variables** — same naming rules as numeric, ending with `$` (e.g. `A$`, `STR$`)
- **Array subscripts** — up to 3 dimensions (e.g. `A(1)`, `B(1,2)`, `C(1,2,3)`)
- **Operators** — `+`, `-`, `*`, `/`, `^` (power), `&` (string concatenation)
- **Comparisons** — `=`, `<>`, `<`, `>`, `<=`, `>=`
- Parentheses for grouping; `^` is right-associative, all others left-associative

### Built-in functions

Built-in functions appear directly inside expressions and return a value (unlike `CALL` subprograms, which are
stand-alone statements).

| Function             | Arguments            | Returns | Description                              |
|----------------------|----------------------|---------|------------------------------------------|
| `ABS(x)`             | 1 numeric            | numeric | Absolute value                           |
| `ATN(x)`             | 1 numeric            | numeric | Arctangent in radians                    |
| `COS(x)`             | 1 numeric            | numeric | Cosine in radians                        |
| `EXP(x)`             | 1 numeric            | numeric | *e* to the power *x*                     |
| `INT(x)`             | 1 numeric            | numeric | Greatest integer ≤ *x*                   |
| `LOG(x)`             | 1 numeric            | numeric | Natural logarithm                        |
| `RND`                | none                 | numeric | Random number in [0, 1)                  |
| `SGN(x)`             | 1 numeric            | numeric | Sign of *x*                              |
| `SIN(x)`             | 1 numeric            | numeric | Sine in radians                          |
| `SQR(x)`             | 1 numeric            | numeric | Square root                              |
| `TAN(x)`             | 1 numeric            | numeric | Tangent in radians                       |
| `ASC(s$)`            | 1 string             | numeric | ASCII code of first character            |
| `LEN(s$)`            | 1 string             | numeric | Length of string                         |
| `POS(s$,t$,n)`       | 2 strings, 1 numeric | numeric | Position of *t$* in *s$* starting at *n* |
| `VAL(s$)`            | 1 string             | numeric | Numeric value of string                  |
| `CHR$(n)`            | 1 numeric            | string  | Character with ASCII code *n*            |
| `SEG$(s$,start,len)` | 1 string, 2 numerics | string  | Substring of *s$*                        |
| `STR$(x)`            | 1 numeric            | string  | String representation of *x*             |

### Error and warning annotations

The annotator inspects every file and highlights:

| Severity | Check                                                                                                                                 |
|----------|---------------------------------------------------------------------------------------------------------------------------------------|
| Error    | Line number out of range (< 1 or > 32767)                                                                                             |
| Error    | Duplicate line numbers                                                                                                                |
| Warning  | Line numbers not in ascending order                                                                                                   |
| Error    | Line without a line number                                                                                                            |
| Error    | Unknown statement keyword                                                                                                             |
| Error    | Variable name that is a reserved keyword or command                                                                                   |
| Error    | Conflicting variable usage (scalar vs. array)                                                                                         |
| Error    | Empty subscript or more than 3 subscript dimensions                                                                                   |
| Error    | Type mismatch (numeric value where string is required, or vice versa)                                                                 |
| Error    | String-number mismatch in LET assignment (variable type differs from expression)                                                      |
| Error    | LET with an invalid variable name (Bad variable name)                                                                                 |
| Error    | LET with trailing tokens after the expression (Incorrect statement)                                                                   |
| Error    | `END` or `STOP` with trailing content (Incorrect statement)                                                                           |
| Warning  | Reference to an undefined line number in BREAK/UNBREAK/TRACE/UNTRACE                                                                  |
| Error    | `GOTO` / `GO TO` without a numeric line number, or with extra content (Incorrect statement)                                           |
| Warning  | `GOTO` / `GO TO` reference to an undefined line number                                                                                |
| Error    | `ON … GOTO` with string expression (String-number mismatch)                                                                           |
| Error    | `ON … GOTO` missing expression, GOTO keyword, or line numbers (Incorrect statement)                                                   |
| Error    | `ON … GOTO` line number out of range 1–32767 (Bad line number)                                                                        |
| Warning  | `ON … GOTO` reference to an undefined line number                                                                                     |
| Error    | `IF … THEN` with string expression (String-number mismatch)                                                                           |
| Error    | `IF … THEN` missing expression, THEN keyword, or THEN line number (Incorrect statement)                                               |
| Error    | `IF … THEN` / `ELSE` line number out of range 1–32767 (Bad line number)                                                               |
| Warning  | `IF … THEN` / `ELSE` reference to an undefined line number                                                                            |
| Error    | `FOR` missing `=`, `TO`, control variable, or a required expression (Incorrect statement)                                             |
| Error    | `FOR` or `NEXT` control variable is a string variable (Numeric variable expected)                                                     |
| Error    | `FOR` initial value, limit, or step is a string expression (String-number mismatch)                                                   |
| Warning  | Unequal number of `FOR` and `NEXT` statements — surplus occurrences flagged (FOR-NEXT-ERROR)                                          |
| Error    | `NEXT` without a control variable (Incorrect statement)                                                                               |
| Error    | `INPUT` without a variable list (Incorrect statement)                                                                                 |
| Error    | `INPUT` with a bad variable name (Bad variable name)                                                                                  |
| Error    | `READ` without a variable list (Incorrect statement)                                                                                  |
| Error    | `READ` with a bad variable name (Bad variable name)                                                                                   |
| Error    | `PRINT` or `DISPLAY` with two adjacent expressions missing a separator (Separator expected between expressions)                       |
| Error    | `PRINT` or `DISPLAY` with an invalid token that is not an expression or separator (PRINT argument must be an expression)              |
| Error    | `RESTORE` with invalid argument — not a single numeric literal (Incorrect statement)                                                  |
| Warning  | `RESTORE` references a line number that does not exist in the program                                                                 |
| Error    | `CALL` with unknown subprogram name                                                                                                   |
| Error    | `CALL SCREEN`, `HCHAR`, `VCHAR`, `GCHAR`, `COLOR` with wrong argument count or type — will cause run-time error `INCORRECT STATEMENT` |
| Error    | `CALL` with wrong number of arguments for other subprograms                                                                           |
| Warning  | `CALL` with a type mismatch in any argument for `CHAR`, `KEY`, `JOYST`, `SOUND`                                                       |
| Error    | `CALL CLEAR` with any trailing tokens on the same line — will cause run-time error `BAD NAME`                                         |
| Error    | Built-in function with wrong argument count — will cause run-time error `INCORRECT STATEMENT`                                         |
| Error    | Built-in function with a type-mismatched argument — will cause run-time error `INCORRECT STATEMENT`                                   |

### Code actions

**Format File** (`Tools › TI-Basic › Format`)

- Converts all keywords outside string literals to uppercase
- Removes extraneous whitespace outside string literals
- Normalizes exactly one space between the line number and the first keyword
- Can be applied to the whole file or to the current selection

**Resequence Line Numbers** (`Tools › TI-Basic › Resequence`)

- Renumbers all lines with a configurable start number and step
- A dialog lets you choose both values before applying
- Also available as a quick-fix on duplicate-line-number and out-of-order warnings

### Editor assistance

- **Keyword and variable completion** — on-demand autocomplete (Ctrl+Space) for all TI-Basic keywords and all variables defined in the current file (case-insensitive); keywords and variables appear in separate groups
- **CALL subprogram completion** — when the cursor is immediately after `CALL`, autocomplete (Ctrl+Space) lists all 10 built-in subprogram names in a dedicated group
- **Built-in function completion** — autocomplete (Ctrl+Space) suggests all built-in function names in a dedicated group
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
│   │   │   ├── ASTNodeExtensions.kt            Kotlin extensions on ASTNode
│   │   │   ├── AnnotationHolderExtensions.kt   Kotlin extensions on AnnotationHolder
│   │   │   └── PsiElementExtensions.kt         Kotlin extensions on PsiElement
│   │   ├── highlight/
│   │   │   ├── TiBasicAnnotator.kt             Semantic error/warning annotations
│   │   │   ├── TiBasicSyntaxHighlighter.kt     Token-based syntax colours
│   │   │   └── TiBasicSyntaxHighlighterFactory.kt
│   │   ├── lang/
│   │   │   ├── TiBasicBuiltInFunctions.kt      Built-in expression function signatures and registry
│   │   │   ├── TiBasicCallSubprograms.kt        CALL subprogram signatures and registry
│   │   │   ├── TiBasicFileIconProvider.kt       Custom file icon
│   │   │   ├── TiBasicFileType.kt               LanguageFileType object
│   │   │   ├── TiBasicKeywords.kt               Keyword and command lists
│   │   │   └── TiBasicLanguage.kt               Language singleton
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
