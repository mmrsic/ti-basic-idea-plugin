# Architecture

This document describes the internal structure of the TI-Basic IntelliJ IDEA plugin for contributors.

## Package overview

All source code lives under `com.github.mmrsic.idea.plugins.tibasic` (abbreviated `tibasic` below).

| Package                     | Responsibility                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tibasic.lang`              | Language/file-type registration: `TiBasicLanguage`, `TiBasicFileType`, `TiBasicFileIconProvider`, `TiBasicKeywords`, `TiBasicCallSubprograms` (signatures for all 10 built-in subprograms), `TiBasicBuiltInFunctions` (signatures for all built-in expression functions)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `tibasic.lexer`             | Tokenisation: `TiBasicLexer`, `TiBasicTokenTypes` (token element types), `TiBasicElementType`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `tibasic.parser`            | Syntax analysis: `TiBasicParser`, `TiBasicParserDefinition`, `TiBasicNodeTypes` (composite node types)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `tibasic.psi`               | PSI root and shared file-level extensions: `TiBasicFile`, `containingTiBasicFile`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `tibasic.psi.statement`     | Statement PSI classes (e.g., `TiBasicLine`, `TiBasicPrintStatement`, `TiBasicOpenStatement`, `TiBasicDefStatement`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `tibasic.psi.expression`    | Expression PSI classes (`TiBasicExpression`, `TiBasicVariableAccess`, `TiBasicFunctionCall`, `TiBasicCallStatement`, `TiBasicTabFunction`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `tibasic.psi.contracts`     | Shared PSI contracts for file/record number statements (`TiBasicFileNumberStatement`, `TiBasicRecordNumberStatement`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `tibasic.psi.common`        | Shared PSI constants (`VALID_LINE_NUMBER_RANGE`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `tibasic.highlight`         | Syntax colours (`TiBasicSyntaxHighlighter`, `TiBasicSyntaxHighlighterFactory`) and semantic annotations (`TiBasicAnnotator`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `tibasic.editor`            | Editor assistance: keyword completion (`TiBasicCompletionContributor`), character-code and `DATA` hex-pattern quick documentation (`TiBasicCharacterCodeDocumentationProvider`), paired-character typing for parentheses and string quotes plus auto-spacing after bare line numbers (`TiBasicPairedCharacterTypedHandler`), Shift+Enter handling (`TiBasicShiftEnterHandler`), Ctrl+D duplicate-line renumbering (`TiBasicDuplicateLineHandler`), paste line-number renumbering (`TiBasicPastePreProcessor`), TI-99/4A display column guides (`TiBasicDisplayColumnGuideController`, `TiBasicDisplayColumnGuideRenderer`), line-number declaration navigation (`TiBasicGotoDeclarationHandler`), CALL CHAR/COLOR/SCREEN gutter previews, CALL SOUND gutter playback, and inbound line-reference markers (`TiBasicLineReferenceLineMarkerProvider`) |
| `tibasic.action.format`     | Format action and formatting logic (`FormatAction`, `FormatCode`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `tibasic.action.resequence` | Resequence action, logic, options dialog, and quick-fix (`ResequenceAction`, `ResequenceLineNumbers`, `ResequenceOptionsDialog`, `ResequenceQuickFix`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `tibasic.action`            | Abstract base for all TI-Basic actions (`TiBasicFileAction`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `tibasic.util`              | Document write-action helpers (`PsiFileUtils`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `tibasic.ext`               | Kotlin extensions on framework classes (`ASTNodeExtensions`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |

## Data flow

```
Source text
    │
    ▼
TiBasicLexer          (tibasic.lexer)
    Reads lines, classifies them, emits typed tokens.
    Each source line is classified as:
      VALID_STATEMENT, LINE_NUMBER_ONLY, LET_IMPLICIT_STATEMENT, UNKNOWN_STATEMENT, or NO_LINE_NUMBER.
    │
    ▼
TiBasicParser         (tibasic.parser)
    Driven by PsiBuilder; builds a composite AST.
    Produces nodes: LINE, PRINT_STATEMENT, DISPLAY_STATEMENT, INPUT_STATEMENT, READ_STATEMENT,
    DATA_STATEMENT, RESTORE_STATEMENT, LET_STATEMENT, DEF_STATEMENT, DIM_STATEMENT, OPTION_BASE_STATEMENT,
    REM_STATEMENT, END_STATEMENT, STOP_STATEMENT, GOTO_STATEMENT, GOSUB_STATEMENT, RETURN_STATEMENT,
    ON_GOTO_STATEMENT, ON_GOSUB_STATEMENT,
    IF_STATEMENT, FOR_STATEMENT, NEXT_STATEMENT, DELETE_STATEMENT,
    LINE_NUMBER_LIST_STATEMENT, UNKNOWN_STATEMENT,
    INVALID_LINE, EXPRESSION, VARIABLE_ACCESS, TAB_FUNCTION, CALL_STATEMENT,
    FUNCTION_CALL.
    │
    ▼
PSI tree              (tibasic.psi + subpackages)
    IntelliJ wraps each composite node via TiBasicParserDefinition.createElement().
    Typed PSI classes (TiBasicLine, TiBasicPrintStatement, …) provide
    convenience accessors (e.g., TiBasicLine.lineNumber(),
    TiBasicFile.lines(), TiBasicFile.variableAccesses(), TiBasicFile.callStatements(),
    TiBasicVariableAccess.subscriptDimCount()).
    │
    ├──▶ TiBasicSyntaxHighlighter   (tibasic.highlight)
    │       Token-level colouring: keywords, literals, operators, comments.
    │
    ├──▶ TiBasicAnnotator           (tibasic.highlight)
    │       Semantic checks on the PSI tree (see Annotator section below).
    │       Attaches error/warning annotations and quick-fixes.
    │
    ├──▶ TiBasicCompletionContributor (tibasic.editor)
    │       Provides on-demand keyword suggestions (from TiBasicKeywords),
    │       variable suggestions (all variables defined in the current file),
    │       CALL subprogram name suggestions (from TiBasicCallSubprograms) when
    │       the cursor is at the subprogram name position (directly after the
    │       CALL keyword or on a CALL_SUBPROGRAM_NAME token), and built-in
    │       function name suggestions (from TiBasicBuiltInFunctions) in
    │       expression context. Inside CALL argument lists the general
    │       completion (variables + functions + keywords) is offered.
    │       Triggered by Ctrl+Space only; auto-popup is disabled. If exactly
    │       one match remains after filtering, it is inserted immediately
    │       without showing the lookup popup. At the beginning of the current
    │       eligible unnumbered last line, completion also offers the same
    │       auto-generated line number that Shift+Enter would insert there.
    │       Built-in functions, the TAB keyword-function, and CALL subprograms
    │       with arguments insert `()`, while generated line numbers insert a
    │       trailing space and place the caret after it.
    │
    ├──▶ TiBasicCharacterCodeDocumentationProvider (tibasic.editor)
    │       Provides Quick Documentation (Ctrl+Q) for character-code positions
    │       in `CALL CHAR`, `CALL HCHAR`, `CALL VCHAR`, and `CHR$`, plus
    │       all argument positions of `CALL COLOR`.
    │       It also recognizes `CALL CHAR` pattern definitions, hexadecimal
    │       `DATA` items, and generic string literals, and renders the same
    │       8x8 pattern preview used by `CALL CHAR`.
    │       The provider resolves numeric literals and constant numeric
    │       variables, reports the matching ASCII character, computes the
    │       TI-Basic character group for codes 32..159, summarizes any
    │       file-local `CALL CHAR` overrides for the same code, and for
    │       `CALL COLOR` shows the resolved value plus the derived set range,
    │       ASCII characters, or TI color name.
    │
    ├──▶ TiBasicPairedCharacterTypedHandler (tibasic.editor)
    │       Intercepts typed `(`, `)` and `"` characters in TI-Basic files.
    │       It inserts matching closing delimiters for opening characters,
    │       skips over an existing closing parenthesis or quote when it should
    │       be reused, and inserts doubled quotes inside existing strings.
    │       For all other non-digit characters, when the caret is at the end of
    │       a line that consists only of a valid line number, it inserts the
    │       required separating space before the typed character.
    │
    ├──▶ TiBasicDisplayColumnGuideController (tibasic.editor)
    │       Installs one zero-width range highlighter per TI-Basic editor and
    │       repaints thin vertical guides at every 28th character column
    │       required by the longest line in the file via
    │       TiBasicDisplayColumnGuideRenderer. The guides are drawn as an
    │       overlay, so they do not alter the editor's text layout.
    │
    ├──▶ GotoDeclarationHandler      (tibasic.editor)
    │       TiBasicGotoDeclarationHandler recognizes numeric literals that are
    │       already classified as line-number references by
    │       PsiElement.lineNumberReferenceNodes().
    │
    │       The handler resolves file-locally via TiBasicFile.lineByNumber()
    │       so Ctrl+B / Ctrl+Click on GOTO, GOSUB, ON ... GOTO/GOSUB,
    │       IF ... THEN/ELSE, RESTORE, BREAK, UNBREAK, TRACE, and UNTRACE
    │       jumps to the referenced target line.
    │
    ├──▶ LineMarkerProvider(s)      (tibasic.editor)
    │       Provide gutter icons for CALL CHAR, CALL COLOR, CALL SCREEN, CALL SOUND, and
    │       inbound line references.
    │       The SOUND provider resolves a single `CALL SOUND(dur,pitch,vol)`
    │       tone from literals, file-local constant numeric variables, or
    │       other simple statically resolvable numeric expressions and
    │       triggers square-wave playback through the shared JVM audio adapter,
    │       which is intended to work on Linux, macOS, and Windows.
    │
    │       TiBasicLineReferenceLineMarkerProvider is triggered on the
    │       LINE_NUMBER leaf token of a TiBasicLine. It uses
    │       TiBasicInboundLineReferenceCollector, which scans the file's lines
    │       and reuses PsiElement.lineNumberReferenceNodes() to collect all
    │       inbound jumps per target line number. If at least one other line
    │       refers to the current line, the provider adds a gutter icon with a
    │       compact tooltip summary and standard click navigation to the
    │       referring lines.
    │
    └──▶ Actions                    (tibasic.action.*)
            FormatAction  — reformats the document text via FormatCode.
            ResequenceAction — renumbers lines via ResequenceLineNumbers.
            Both actions modify the PSI document inside a WriteAction.
```

## Variables tool window (tibasic.toolwindow)

The Variables tool window lists all scalar and array variables plus user-defined functions
in the active TI-Basic file. It refreshes automatically after every committed document change.

### Data model

| Class                       | Responsibility                                                                                                                                                                  |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TiBasicVariableOccurrence` | Single PSI occurrence: `lineNumber`, `offset`, `accessType` (READ/WRITE/NONE), `writtenConstant` (literal value if the write is a bare literal in a LET statement, else `null`) |
| `TiBasicVariableEntry`      | Aggregated entry: `name`, `type`, `occurrences`; derived properties: `reads`, `writes`, `lineNumbers`, `constValue`                                                             |
| `TiBasicVariableType`       | Enum: NUMERIC, STRING, NUMERIC_ARRAY, STRING_ARRAY, DIM_DECLARATION, USER_FUNCTION                                                                                              |

### Const-value detection

`TiBasicVariableEntry.constValue` is computed from the occurrences at data-access time:

- Only `NUMERIC` and `STRING` scalar types can have a `constValue` (arrays, DIM, DEF → `null`).
- **Never written** (`writes == 0`): `constValue = "0"` (NUMERIC) or `"\"\""` (STRING).
- **All writes use the same literal**: `constValue = <that literal text>`.
- **Any write is non-literal** (INPUT/READ/FOR/CALL, or a compound expression): `constValue = null`.
- **Writes use different literals**: `constValue = null`.

Literal detection in `TiBasicVariableCollector.extractLetConstant`: checks that the RHS
`EXPRESSION` node of the `TiBasicLetStatement` has exactly one non-whitespace child whose
element type is `NUMERIC_LITERAL` or `STRING_LITERAL`.

### Access type classification

`TiBasicVariableCollector.determineAccessType` maps each `TiBasicVariableAccess` to an `AccessType`:

| Parent PSI element                                | Access type                                                                                         |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `TiBasicInputStatement`                           | WRITE                                                                                               |
| `TiBasicReadStatement`                            | WRITE                                                                                               |
| `TiBasicLetStatement`                             | WRITE if first `VARIABLE_ACCESS` child (LHS), READ otherwise                                        |
| `TiBasicForStatement`                             | WRITE if first `VARIABLE_ACCESS` child (loop control variable), READ for start/end/step expressions |
| `TiBasicNextStatement`                            | READ (NEXT reads the loop counter to determine whether the loop continues)                          |
| `TiBasicExpression` inside `TiBasicCallStatement` | WRITE for the third `GCHAR` argument and the second/third `KEY`/`JOYST` arguments; READ otherwise   |
| any other parent                                  | READ                                                                                                |

| Index             | Name   | Content                                                                      |
|-------------------|--------|------------------------------------------------------------------------------|
| 0                 | Name   | Variable name                                                                |
| 1                 | Type   | `TiBasicVariableType.displayName`                                            |
| 2 (WRITES_COLUMN) | Writes | `List<TiBasicVariableOccurrence>` — rendered as clickable line-number badges |
| 3 (READS_COLUMN)  | Reads  | `List<TiBasicVariableOccurrence>` — rendered as clickable line-number badges |
| 4 (CONST_COLUMN)  | Const  | `String?` — constant value or empty                                          |

## Character definitions tool window (tibasic.toolwindow)

The Character Definitions tool window lists all statically resolvable `CALL CHAR`
definitions in the active TI-Basic file. Visually identical definitions are collapsed
into one table row when they share the same normalized pattern and the same derived
foreground/background color variants.

### Shared static call traversal and collectors

`editor/TiBasicStaticCallStatementTraversal.kt` provides the conservative file traversal
used by both `CALL CHAR` and `CALL COLOR` collection. It performs the shared linear trace
through `READ`/`DATA`, `RESTORE`, simple statically resolvable `FOR`/`NEXT` loops, and
simple statically decidable `IF ... THEN [ELSE]` jumps, and records each encountered
`TiBasicCallStatement` together with the current `READ`-derived variable bindings.

On top of that traversal, `editor/TiBasicCallCharDefinitions.kt` and
`editor/TiBasicCallColorAssignments.kt` provide the cached file-level collectors used by
the tool window:

| Type / function                 | Responsibility                                                                                             |
|---------------------------------|------------------------------------------------------------------------------------------------------------|
| `TiBasicStaticCallStatement`    | One traversed `CALL` statement plus the active `READ`-derived variable values at that execution point      |
| `collectCallCharDefinitions()`  | Cached file-level collection of all statically resolvable `CALL CHAR` definitions, sorted by code/line     |
| `resolveCallCharDefinition()`   | Resolves one `TiBasicCallStatement` into a char definition when both code and pattern are statically known |
| `collectCallColorAssignments()` | Cached file-level collection of all statically resolvable `CALL COLOR` assignments                         |
| `resolveCallColorAssignment()`  | Resolves one `TiBasicCallStatement` into a color assignment when set, foreground, and background are known |

Resolution reuses the existing helpers `resolveConstantNumericValue`,
`resolveConstantStringValue`, `normalizeHexPattern`, `asciiCharacterName`,
`callColorCharacterSetRange`, and `tiColorAt`. This lets the tool window collect both
direct literal/constant-variable definitions, simple statically resolvable numeric code
expressions, and color assignments that can be traced conservatively through the same static
control flow. Definitions or color assignments whose required values are still not statically
determinable are omitted.

### Tool-window table

| Index                          | Name    | Content                                                                                                |
|--------------------------------|---------|--------------------------------------------------------------------------------------------------------|
| 0 (`CHARACTER_CODE_COLUMN`)    | Code    | Integer character code                                                                                 |
| 1 (`CHARACTER_ASCII_COLUMN`)   | ASCII   | ASCII name/character or empty                                                                          |
| 2 (`CHARACTER_PATTERN_COLUMN`) | Pattern | Normalized 16-digit hex pattern                                                                        |
| 3 (`CHARACTER_ICON_COLUMN`)    | Icon    | Base `TiBasicCharPatternIcon` plus distinct `TiBasicColoredCharPatternIcon` variants from `CALL COLOR` |
| 4 (`CHARACTER_LINE_COLUMN`)    | Line    | Source program line; click navigates                                                                   |

## Annotator checks

`TiBasicAnnotator` dispatches on the PSI element type and applies the following checks:

| Element type                                                                                            | Checks                                                                                                                                                                                                                                                                                                                                                                                  |
|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TiBasicFile`                                                                                           | Duplicate line numbers (error + quick-fix), non-ascending line numbers (warning + quick-fix), variable-name conflicts (scalar vs. array), FOR-NEXT balance per control variable (warning on all FOR and NEXT statements of an unbalanced variable), duplicate DEF names (warning), DEF self-reference (warning), duplicate DIM array names (error), DIM after first array use (warning) |
| `TiBasicLine`                                                                                           | Line number out of `VALID_LINE_NUMBER_RANGE` (1–32767)                                                                                                                                                                                                                                                                                                                                  |
| `TiBasicLetStatement`                                                                                   | Invalid variable name (Bad variable name); trailing tokens after expression (Incorrect statement); type mismatch between variable and expression (String-number mismatch)                                                                                                                                                                                                               |
| `TiBasicDefStatement`                                                                                   | Missing function name or `=` or body expression (Incorrect statement); invalid function/parameter name (Bad variable name); type mismatch between function name type and body expression (String-number mismatch); parameter used with subscripts in body (Incorrect statement)                                                                                                         |
| `TiBasicDimStatement`                                                                                   | No array entries (Incorrect statement); trailing comma (Incorrect statement); invalid variable name (Bad variable name); missing parentheses (Incorrect statement); variable/float/expression used as dimension (specific errors per kind)                                                                                                                                              |
| `TiBasicOptionBaseStatement`                                                                            | Missing or excess tokens (Incorrect statement); variable as value; float as value; integer value not 0 or 1 (specific errors per kind)                                                                                                                                                                                                                                                  |
| `TiBasicScreenPrintStatement` (abstract base for `TiBasicPrintStatement` and `TiBasicDisplayStatement`) | Invalid variable names, type mismatches (string vs. numeric); consecutive expressions or TAB functions without separator (Separator expected between expressions)                                                                                                                                                                                                                       |
| `TiBasicTabFunction`                                                                                    | Missing parentheses and argument (TAB requires a numeric argument in parentheses); missing argument inside parens (TAB requires a numeric argument); TAB outside PRINT or DISPLAY (TAB is only valid in a PRINT or DISPLAY statement — detected via leaf token check)                                                                                                                   |
| `TiBasicEndStatement`                                                                                   | Trailing non-whitespace content after END keyword (warning)                                                                                                                                                                                                                                                                                                                             |
| `TiBasicStopStatement`                                                                                  | Trailing non-whitespace content after STOP keyword (warning)                                                                                                                                                                                                                                                                                                                            |
| `TiBasicGotoStatement`                                                                                  | Missing or non-numeric line number target (Incorrect statement); out-of-range target (Bad line number, error); undefined target (Bad line number, warning)                                                                                                                                                                                                                              |
| `TiBasicGosubStatement`                                                                                 | Same checks as `TiBasicGotoStatement` (Incorrect statement / Bad line number)                                                                                                                                                                                                                                                                                                           |
| `TiBasicReturnStatement`                                                                                | Trailing non-whitespace content after RETURN keyword (warning)                                                                                                                                                                                                                                                                                                                          |
| `TiBasicOnGotoStatement`                                                                                | Missing expression or GOTO keyword (Incorrect statement); string expression (String-number mismatch); missing or malformed line number list (Incorrect statement / Bad line number); undefined targets (Bad line number, warning)                                                                                                                                                       |
| `TiBasicOnGosubStatement`                                                                               | Same checks as `TiBasicOnGotoStatement` but after GOSUB keyword                                                                                                                                                                                                                                                                                                                         |
| `TiBasicInputStatement`                                                                                 | No variable list (Incorrect statement); invalid variable name (Bad variable name)                                                                                                                                                                                                                                                                                                       |
| `TiBasicReadStatement`                                                                                  | No variable list (Incorrect statement); invalid variable name (Bad variable name)                                                                                                                                                                                                                                                                                                       |
| `TiBasicDataStatement`                                                                                  | Empty data list — no items and no commas (Incorrect statement)                                                                                                                                                                                                                                                                                                                          |
| `TiBasicRestoreStatement`                                                                               | Argument is not a single numeric literal (Incorrect statement); target line undefined in file (warning)                                                                                                                                                                                                                                                                                 |
| `TiBasicDeleteStatement`                                                                                | Numeric literal/variable where string expression is required                                                                                                                                                                                                                                                                                                                            |
| `TiBasicLineNumberListStatement`                                                                        | Missing line numbers, extra tokens, trailing comma, undefined line number references (warning)                                                                                                                                                                                                                                                                                          |
| `TiBasicGotoStatement`                                                                                  | Missing or non-numeric line number; line number out of range or undefined                                                                                                                                                                                                                                                                                                               |
| `TiBasicOnGotoStatement`                                                                                | Missing expression or GOTO keyword; string expression (error); bad/undefined line numbers                                                                                                                                                                                                                                                                                               |
| `TiBasicIfStatement`                                                                                    | Missing expression, THEN keyword, or THEN line number; string expression; line numbers out of range or undefined                                                                                                                                                                                                                                                                        |
| `TiBasicForStatement`                                                                                   | Missing `=`, `TO`, variable, or required expressions (Incorrect statement); string control variable (Numeric variable expected); string expression in numeric position (String-number mismatch)                                                                                                                                                                                         |
| `TiBasicNextStatement`                                                                                  | Missing control variable (Incorrect statement); string control variable (Numeric variable expected)                                                                                                                                                                                                                                                                                     |
| `TiBasicCallStatement`                                                                                  | Unknown subprogram name (error on name token); wrong argument count (error on statement); `GCHAR` third argument must be a numeric variable target (Incorrect statement); type mismatch in any other argument (warning on expression)                                                                                                                                                   |
| `TiBasicFunctionCall`                                                                                   | Unknown function name (error on name token); wrong argument count → `INCORRECT STATEMENT` runtime error; type mismatch in any argument → `INCORRECT STATEMENT` runtime error                                                                                                                                                                                                            |
| `TiBasicUnknownStatement`                                                                               | Command used as statement vs. fully unknown identifier                                                                                                                                                                                                                                                                                                                                  |
| `TiBasicInvalidLine`                                                                                    | Line without a leading line number                                                                                                                                                                                                                                                                                                                                                      |
| `TiBasicVariableAccess`                                                                                 | Empty subscript parens, subscript count > 3                                                                                                                                                                                                                                                                                                                                             |
| `TiBasicExpression`                                                                                     | Numeric literal/variable in string-only position                                                                                                                                                                                                                                                                                                                                        |

## Threading model

| Context                                             | Rule                                                                                                                                                                        |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Annotator, SyntaxHighlighter, CompletionContributor | Read-only PSI access — always on a read-allowed thread; no explicit `ReadAction` needed because IntelliJ provides the read lock.                                            |
| Actions (`FormatAction`, `ResequenceAction`)        | All PSI modifications happen inside `WriteAction` (via `PsiFileUtils.withWriteCommand`). Menu actions execute on the EDT; the write action must be called from there.       |
| Dialogs (`ResequenceOptionsDialog`)                 | Created and shown on the EDT.                                                                                                                                               |
| No background tasks yet                             | All current operations are fast enough for the EDT. Use `ProgressManager.runBackgroundableTask` if any future operation is potentially slow (e.g., multi-file refactoring). |

## Key design decisions

- **Lexer classifies lines, parser structures them.** The lexer does the heavy lifting of recognizing valid lines using
  regexes; the parser only needs to deal with pre-classified token streams. This keeps the parser simple but means the
  lexer regex patterns are the primary grammar artifact.
- **Semi-permissive expression parsing.** The parser consumes type-mismatched tokens (e.g., a `STRING_VARIABLE` in a
  numeric expression) rather than rejecting them. This gives the annotator material to produce specific error messages
  instead of generic parse errors.
- **`VALID_LINE_NUMBER_RANGE` as a single constant.** Defined in `psi/TiBasicPsiElements.kt`; referenced by both the
  annotator and the resequence logic to avoid scattered magic numbers.
- **Split token/node types.** `TiBasicTokenTypes` (leaf tokens) lives in `tibasic.lexer`; `TiBasicNodeTypes`
  (composite nodes) lives in `tibasic.parser`. This reflects IntelliJ's own conventions and avoids circular imports
  between lexer and parser.
- **Kotlin extensions on framework types.** Verbose framework calls (e.g., `node.getChildren(null)`) are wrapped in
  extensions (`node.allChildren`) collected in `tibasic.ext`. See [`extension-points.md`](extension-points.md) and
  the coding conventions in `.github/copilot-instructions.md`.

## Built-in expression functions

TI-Basic provides a fixed set of built-in functions usable inside expressions (e.g., `ABS(X)`, `SIN(A)`, `CHR$(65)`).
These are distinct from `CALL` subprograms: they appear inside an expression and return a value, whereas `CALL`
subprograms are stand-alone statements.

### Function registry (`tibasic.lang` — `TiBasicBuiltInFunctions.kt`)

```kotlin
enum class FunctionReturnType { NUMERIC, STRING }

data class BuiltInFunctionSignature(
    val argCount: Int,                  // 0 = no parentheses (RND), ≥1 = parenthesised args
    val argTypes: List<CallArgType>,    // reuses CallArgType from TiBasicCallSubprograms
    val returnType: FunctionReturnType,
)

object TiBasicBuiltInFunctions {
    // single source of truth for all built-in function names and signatures
    fun numericFunctionNames(): Set<String>  // NUMERIC_FUNCTION_KEYWORD candidates
    fun stringFunctionNames(): Set<String>  // STRING_FUNCTION_KEYWORD candidates (CHR$, SEG$, STR$)
    fun byName(name: String?): BuiltInFunctionSignature?
}
```

To add a new function, add **one entry** to the map inside `TiBasicBuiltInFunctions`. No other change is needed
in the lexer, parser, or annotator. Currently implemented: `ABS`, `ATN`, `COS`, `EXP`, `INT`, `LOG`, `RND`, `SGN`,
`SIN`, `SQR`, `TAN` (numeric, returning numeric), `ASC`, `LEN`, `VAL`, `POS` (string-arg, returning numeric), `CHR$`,
`SEG$`, `STR$` (returning string).

### Token types

| Token                      | Covers                                                                                                                                          |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `NUMERIC_FUNCTION_KEYWORD` | All numeric and mixed-return functions: `ABS`, `ATN`, `COS`, `EXP`, `INT`, `LOG`, `RND`, `SGN`, `SIN`, `SQR`, `TAN`, `ASC`, `LEN`, `VAL`, `POS` |
| `STRING_FUNCTION_KEYWORD`  | All string-returning functions: `CHR$`, `SEG$`, `STR$`                                                                                          |

The lexer's `tokenizeIdentifier` checks function names (from `TiBasicBuiltInFunctions`) before
falling back to the variable/keyword classification. `STRING_FUNCTION_KEYWORD` is defined from the
start but only wired into the string-expression parser when string functions are implemented.

### Grammar extension

```ebnf
numericPrimary = NUMERIC_LITERAL
               | variableAccess
               | STRING_LITERAL | stringVariableAccess   (* type mismatch — for error reporting *)
               | LPAREN numericComparison RPAREN
               | numericFunctionCall ;

numericFunctionCall = NUMERIC_FUNCTION_KEYWORD LPAREN expressionList RPAREN ;
                    (* argCount=0 functions (RND) omit the parentheses *)

stringFunctionCall  = STRING_FUNCTION_KEYWORD LPAREN expressionList RPAREN ;
                    (* added to stringOperand once string functions are implemented *)

expressionList = expression { COMMA expression } ;
```

Both `numericFunctionCall` and `stringFunctionCall` produce a `FUNCTION_CALL` composite node
(same node type, function name token distinguishes numeric vs. string return type).

### Extensibility checklist

To add a new built-in function:

1. Add one entry to `TiBasicBuiltInFunctions.signatures`.
2. That is all — the lexer, parser, annotator, completion, and syntax highlighter pick it up automatically.
