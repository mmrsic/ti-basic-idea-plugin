# Architecture

This document describes the internal structure of the TI-Basic IntelliJ IDEA plugin for contributors.

## Package overview

All source code lives under `com.github.mmrsic.idea.plugins.tibasic` (abbreviated `tibasic` below).

| Package                     | Responsibility                                                                                                                                         |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tibasic.lang`              | Language/file-type registration: `TiBasicLanguage`, `TiBasicFileType`, `TiBasicFileIconProvider`, `TiBasicKeywords`                                    |
| `tibasic.lexer`             | Tokenisation: `TiBasicLexer`, `TiBasicTokenTypes` (token element types), `TiBasicElementType`                                                          |
| `tibasic.parser`            | Syntax analysis: `TiBasicParser`, `TiBasicParserDefinition`, `TiBasicNodeTypes` (composite node types)                                                 |
| `tibasic.psi`               | Program structure interface: `TiBasicFile` (root PSI element), all PSI node classes, `VALID_LINE_NUMBER_RANGE`                                         |
| `tibasic.highlight`         | Syntax colours (`TiBasicSyntaxHighlighter`, `TiBasicSyntaxHighlighterFactory`) and semantic annotations (`TiBasicAnnotator`)                           |
| `tibasic.editor`            | Editor assistance: keyword completion (`TiBasicCompletionContributor`) and Shift+Enter handling (`TiBasicShiftEnterHandler`)                           |
| `tibasic.action.format`     | Format action and formatting logic (`FormatAction`, `FormatCode`)                                                                                      |
| `tibasic.action.resequence` | Resequence action, logic, options dialog, and quick-fix (`ResequenceAction`, `ResequenceLineNumbers`, `ResequenceOptionsDialog`, `ResequenceQuickFix`) |
| `tibasic.action`            | Abstract base for all TI-Basic actions (`TiBasicFileAction`)                                                                                           |
| `tibasic.util`              | Document write-action helpers (`PsiFileUtils`)                                                                                                         |
| `tibasic.ext`               | Kotlin extensions on framework classes (`ASTNodeExtensions`)                                                                                           |

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
    DATA_STATEMENT, RESTORE_STATEMENT, LET_STATEMENT, REM_STATEMENT,
    END_STATEMENT, STOP_STATEMENT, GOTO_STATEMENT, ON_GOTO_STATEMENT,
    IF_STATEMENT, FOR_STATEMENT, NEXT_STATEMENT, DELETE_STATEMENT,
    LINE_NUMBER_LIST_STATEMENT, UNKNOWN_STATEMENT,
    INVALID_LINE, EXPRESSION, VARIABLE_ACCESS, TAB_FUNCTION.
    │
    ▼
PSI tree              (tibasic.psi)
    IntelliJ wraps each composite node via TiBasicParserDefinition.createElement().
    Typed PSI classes (TiBasicLine, TiBasicPrintStatement, …) provide
    convenience accessors (e.g., TiBasicLine.lineNumber(),
    TiBasicFile.lines(), TiBasicVariableAccess.subscriptDimCount()).
    │
    ├──▶ TiBasicSyntaxHighlighter   (tibasic.highlight)
    │       Token-level colouring: keywords, literals, operators, comments.
    │
    ├──▶ TiBasicAnnotator           (tibasic.highlight)
    │       Semantic checks on the PSI tree (see Annotator section below).
    │       Attaches error/warning annotations and quick-fixes.
    │
    ├──▶ TiBasicCompletionContributor (tibasic.editor)
    │       Provides keyword suggestions from TiBasicKeywords.
    │
    └──▶ Actions                    (tibasic.action.*)
            FormatAction  — reformats the document text via FormatCode.
            ResequenceAction — renumbers lines via ResequenceLineNumbers.
            Both actions modify the PSI document inside a WriteAction.
```

## Annotator checks

`TiBasicAnnotator` dispatches on the PSI element type and applies the following checks:

| Element type                                                                                            | Checks                                                                                                                                                                                                                                                                |
|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TiBasicFile`                                                                                           | Duplicate line numbers (error + quick-fix), non-ascending line numbers (warning + quick-fix), variable-name conflicts (scalar vs. array), FOR-NEXT balance (warning on surplus occurrences)                                                                           |
| `TiBasicLine`                                                                                           | Line number out of `VALID_LINE_NUMBER_RANGE` (1–32767)                                                                                                                                                                                                                |
| `TiBasicLetStatement`                                                                                   | Invalid variable name (Bad variable name); trailing tokens after expression (Incorrect statement); type mismatch between variable and expression (String-number mismatch)                                                                                             |
| `TiBasicScreenPrintStatement` (abstract base for `TiBasicPrintStatement` and `TiBasicDisplayStatement`) | Invalid variable names, type mismatches (string vs. numeric); consecutive expressions or TAB functions without separator (Separator expected between expressions)                                                                                                     |
| `TiBasicTabFunction`                                                                                    | Missing parentheses and argument (TAB requires a numeric argument in parentheses); missing argument inside parens (TAB requires a numeric argument); TAB outside PRINT or DISPLAY (TAB is only valid in a PRINT or DISPLAY statement — detected via leaf token check) |
| `TiBasicEndStatement`                                                                                   | Trailing non-whitespace content after END keyword (Incorrect statement)                                                                                                                                                                                               |
| `TiBasicStopStatement`                                                                                  | Trailing non-whitespace content after STOP keyword (Incorrect statement)                                                                                                                                                                                              |
| `TiBasicInputStatement`                                                                                 | No variable list (Incorrect statement); invalid variable name (Bad variable name)                                                                                                                                                                                     |
| `TiBasicReadStatement`                                                                                  | No variable list (Incorrect statement); invalid variable name (Bad variable name)                                                                                                                                                                                     |
| `TiBasicDataStatement`                                                                                  | Empty data list — no items and no commas (Incorrect statement)                                                                                                                                                                                                        |
| `TiBasicRestoreStatement`                                                                               | Argument is not a single numeric literal (Incorrect statement); target line undefined in file (warning)                                                                                                                                                               |
| `TiBasicDeleteStatement`                                                                                | Numeric literal/variable where string expression is required                                                                                                                                                                                                          |
| `TiBasicLineNumberListStatement`                                                                        | Missing line numbers, extra tokens, trailing comma, undefined line number references (warning)                                                                                                                                                                        |
| `TiBasicGotoStatement`                                                                                  | Missing or non-numeric line number; line number out of range or undefined                                                                                                                                                                                             |
| `TiBasicOnGotoStatement`                                                                                | Missing expression or GOTO keyword; string expression (error); bad/undefined line numbers                                                                                                                                                                             |
| `TiBasicIfStatement`                                                                                    | Missing expression, THEN keyword, or THEN line number; string expression; line numbers out of range or undefined                                                                                                                                                      |
| `TiBasicForStatement`                                                                                   | Missing `=`, `TO`, variable, or required expressions (Incorrect statement); string control variable (Numeric variable expected); string expression in numeric position (String-number mismatch)                                                                       |
| `TiBasicNextStatement`                                                                                  | Missing control variable (Incorrect statement); string control variable (Numeric variable expected)                                                                                                                                                                   |
| `TiBasicUnknownStatement`                                                                               | Command used as statement vs. fully unknown identifier                                                                                                                                                                                                                |
| `TiBasicInvalidLine`                                                                                    | Line without a leading line number                                                                                                                                                                                                                                    |
| `TiBasicVariableAccess`                                                                                 | Empty subscript parens, subscript count > 3                                                                                                                                                                                                                           |
| `TiBasicExpression`                                                                                     | Numeric literal/variable in string-only position                                                                                                                                                                                                                      |

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
