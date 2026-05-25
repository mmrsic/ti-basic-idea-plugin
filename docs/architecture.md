# Architecture

This document describes the internal structure of the TI-Basic IntelliJ IDEA plugin for contributors.

## Package overview

All source code lives under `com.github.mmrsic.idea.plugins.tibasic` (abbreviated `tibasic` below).

| Package                                  | Responsibility                                                                                                                                                                                                                             |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tibasic.ide.language`                   | IntelliJ language registration and entry points: `TiBasicLanguage`, `TiBasicFileType`, `TiBasicFileIconProvider`, `TiBasicParserDefinition`                                                                                                |
| `tibasic.language.model`                 | TI-Basic registries and domain constants: `TiBasicKeywords`, `TiBasicCallSubprograms`, `TiBasicBuiltInFunctions`, `TiColor`                                                                                                                |
| `tibasic.language.values`                | TI-Basic value encoding helpers such as radix-100 number parsing and formatting (`TiBasicRadix100`)                                                                                                                                        |
| `tibasic.language.syntax`                | Small TI-Basic syntax helpers that are not IntelliJ extension points, e.g. parenthesis counting for source lines                                                                                                                           |
| `tibasic.language.syntax.lexer`          | Tokenisation: `TiBasicLexer`, `TiBasicTokenTypes` (token element types)                                                                                                                                                                    |
| `tibasic.language.syntax.parser`         | Syntax analysis core: `TiBasicParser`, `TiBasicNodeTypes` (composite node types)                                                                                                                                                           |
| `tibasic.language.syntax.psi`            | PSI root and shared file-level extensions: `TiBasicFile`, `containingTiBasicFile`                                                                                                                                                          |
| `tibasic.language.syntax.psi.statement`  | Statement PSI classes (e.g., `TiBasicLine`, `TiBasicPrintStatement`, `TiBasicOpenStatement`, `TiBasicDefStatement`)                                                                                                                        |
| `tibasic.language.syntax.psi.expression` | Expression PSI classes (`TiBasicExpression`, `TiBasicVariableAccess`, `TiBasicFunctionCall`, `TiBasicCallStatement`, `TiBasicTabFunction`)                                                                                                 |
| `tibasic.language.syntax.psi.contracts`  | Shared PSI contracts for file/record number statements (`TiBasicFileNumberStatement`, `TiBasicRecordNumberStatement`)                                                                                                                      |
| `tibasic.language.syntax.psi.common`     | Shared PSI constants (`VALID_LINE_NUMBER_RANGE`)                                                                                                                                                                                           |
| `tibasic.highlight`                      | Syntax colours (`TiBasicSyntaxHighlighter`, `TiBasicSyntaxHighlighterFactory`) plus the thin IntelliJ `TiBasicAnnotator` adapter that delegates semantic checks to `tibasic.language.analysis`                                             |
| `tibasic.language.analysis`              | TI-Basic semantic analysis and expression evaluation (`TiBasicSemanticAnnotator`, `TiBasicNumericExpressionResolver`)                                                                                                                      |
| `tibasic.editor`                         | Remaining IntelliJ editor infrastructure and shared editor utilities: paired-character typing, Shift+Enter, duplicate-line and paste renumbering, display column guides, sound/color/character helper logic, and line-reference collection |
| `tibasic.ide.editor.completion`          | Completion contributor entry points (`TiBasicCompletionContributor`)                                                                                                                                                                       |
| `tibasic.ide.editor.documentation`       | Quick documentation entry point (`TiBasicCharacterCodeDocumentationProvider`)                                                                                                                                                              |
| `tibasic.ide.editor.navigation`          | Declaration navigation entry point (`TiBasicGotoDeclarationHandler`)                                                                                                                                                                       |
| `tibasic.ide.editor.markers`             | Gutter marker entry points for CALL CHAR/COLOR/SCREEN/SOUND previews and inbound line references                                                                                                                                           |
| `tibasic.language.analysis.calls`        | Static call traversal plus cached `CALL CHAR` / `CALL COLOR` collectors used by editor and tool-window features                                                                                                                            |
| `tibasic.language.analysis.references`   | Inbound line-reference collection and tooltip text generation                                                                                                                                                                              |
| `tibasic.language.analysis.variables`    | Variable-analysis model and collector (`TiBasicVariableCollector`, entry/occurrence/value types, access classification)                                                                                                                    |
| `tibasic.ide.debug`                      | Debugger session service, frozen program snapshot, inspect evaluation, and line-by-line stepping runtime                                                                                                                                   |
| `tibasic.ide.debug.run`                  | IntelliJ run/debug entry points for TI-Basic debug configurations                                                                                                                                                                          |
| `tibasic.ide.findusages`                 | IntelliJ Find Usages integration and read/write access detection                                                                                                                                                                           |
| `tibasic.language.format`                | Pure formatting and resequencing logic (`FormatCode`, `ResequenceLineNumbers`)                                                                                                                                                             |
| `tibasic.language.runtime.screen`        | Selection-based screen preview evaluation and preview model (`TiBasicScreenPreviewEvaluator`, `TiBasicScreenPreview`)                                                                                                                      |
| `tibasic.ide.actions.format`             | Format action entry points (`FormatAction`, `TiBasicReformatCodeAction`, `TiBasicReformatCodeActionOverrideInitializer`)                                                                                                                   |
| `tibasic.ide.actions.resequence`         | Resequence action entry points (`ResequenceAction`, `ResequenceOptionsDialog`, `ResequenceQuickFix`)                                                                                                                                       |
| `tibasic.ide.actions.preview`            | Screen-preview UI actions and dialog (`TiBasicScreenPreviewAction`, `TiBasicScreenPreviewDialog`)                                                                                                                                          |
| `tibasic.ide.actions`                    | Abstract base for TI-Basic IDE actions (`TiBasicFileAction`)                                                                                                                                                                               |
| `tibasic.common.bundle`                  | Resource bundle access (`TiBasicBundle`)                                                                                                                                                                                                   |
| `tibasic.common.util`                    | Shared IntelliJ-facing utility helpers such as document write helpers (`PsiFileUtils`)                                                                                                                                                     |
| `tibasic.common.ext`                     | Kotlin extensions on framework classes (`ASTNodeExtensions`, `PsiElementExtensions`, `AnnotationHolderExtensions`)                                                                                                                         |

## Data flow

```
Source text
    │
    ▼
TiBasicLexer          (tibasic.language.syntax.lexer)
    Reads lines, classifies them, emits typed tokens.
    Each source line is classified as:
      VALID_STATEMENT, LINE_NUMBER_ONLY, LET_IMPLICIT_STATEMENT, UNKNOWN_STATEMENT, or NO_LINE_NUMBER.
    │
    ▼
TiBasicParser         (tibasic.language.syntax.parser)
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
PSI tree              (tibasic.language.syntax.psi + subpackages)
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
    │       Thin adapter delegating to TiBasicSemanticAnnotator in
    │       `tibasic.language.analysis`.
    │
    ├──▶ TiBasicSemanticAnnotator   (tibasic.language.analysis)
    │       Semantic checks on the PSI tree (see Annotator section below).
    │       Attaches error/warning annotations and quick-fixes.
    │
    ├──▶ TiBasicCompletionContributor (tibasic.ide.editor.completion)
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
    │       After `RESTORE `, it also offers line numbers of all `DATA`
    │       statements in the current file.
    │       Built-in functions, the TAB keyword-function, and CALL subprograms
    │       with arguments insert `()`, while generated line numbers insert a
    │       trailing space and place the caret after it.
    │
    ├──▶ TiBasicCharacterCodeDocumentationProvider (tibasic.ide.editor.documentation)
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
    ├──▶ TiBasicScreenPreviewAction (tibasic.ide.actions.preview)
    │       Opens an explicit dialog-based 32x24 screen preview for the current
    │       editor selection. The action is enabled only when the selection
    │       intersects at least one `CALL HCHAR` or `CALL VCHAR` line. The
    │       evaluator processes selected lines in source order, applies
    │       selection-local `LET` constants, updates global preview state from
    │       selected `CALL CHAR`, `CALL COLOR`, `CALL SCREEN`, and `CALL CLEAR`
    │       statements, and writes selected `CALL HCHAR`/`CALL VCHAR`
    │       character codes into a wrapping 32x24 grid that rolls over from the
    │       last row/column back to the top-left corner. Statements whose values
    │       are not statically resolvable inside the selection are skipped and
    │       surfaced as a partial-preview warning in the dialog.
    │
    ├──▶ TiBasicPairedCharacterTypedHandler (tibasic.editor)
    │       Intercepts typed `(`, `)` and `"` characters in TI-Basic files.
    │       It inserts matching closing delimiters for opening characters,
    │       skips over an existing closing parenthesis or quote when it should
    │       be reused, inserts doubled quotes inside existing strings, and
    │       recognizes TI character-code triggers inside string literals.
    │       Numeric triggers use exactly three digits (for example `\065`
    │       and `\255`) and cover the full code range `000..255`. For all
    │       key combinations defined in `docs/img/TI-BASIC_key_unit_5.png`,
    │       additional aliases are supported: CTRL combinations use forms
    │       such as `\C@`, `\CX`, `\C.`, `\C;`, `\C=`, `\C9`, `\C/`,
    │       `\^X`, `\C-X`, and `\CTRL-X`; FCTN combinations
    │       use forms such as `\F7`, `\F-7`, and `\FCTN-7`. Replacements use
    │       the same source-text representation that direct typing would
    │       require inside strings, so code `034` becomes `""` in source.
    │       For all other non-space characters, when the caret is after a valid
    │       line number or numeric literal and the interpreter would not treat
    │       the typed character as a continuation of that number, it inserts
    │       the required separating space before the typed character.
    │
    ├──▶ TiBasicDisplayColumnGuideController (tibasic.editor)
    │       Installs one zero-width range highlighter per TI-Basic editor and
    │       repaints thin vertical guides at every 28th character column
    │       required by the longest line in the file via
    │       TiBasicDisplayColumnGuideRenderer. The guides are drawn as an
    │       overlay, so they do not alter the editor's text layout.
    │
    ├──▶ GotoDeclarationHandler      (tibasic.ide.editor.navigation)
    │       TiBasicGotoDeclarationHandler recognizes numeric literals that are
    │       already classified as line-number references by
    │       PsiElement.lineNumberReferenceNodes().
    │
    │       The handler resolves file-locally via TiBasicFile.lineByNumber()
    │       so Ctrl+B / Ctrl+Click on GOTO, GOSUB, ON ... GOTO/GOSUB,
    │       IF ... THEN/ELSE, RESTORE, BREAK, UNBREAK, TRACE, and UNTRACE
    │       jumps to the referenced target line.
    │
    ├──▶ LineMarkerProvider(s)      (tibasic.ide.editor.markers)
    │       Provide gutter icons for CALL CHAR, CALL COLOR, CALL SCREEN, CALL SOUND, and
    │       inbound line references.
    │       The SOUND provider resolves `CALL SOUND` arguments from literals,
    │       file-local constant numeric variables, or other simple statically
    │       resolvable numeric expressions and maps them to up to three tone
    │       channels plus an optional SN76489-style noise channel before
    │       triggering playback through the shared JVM audio adapter. Positive
    │       durations always play to completion; negative durations are
    │       interruptible and a new click replaces the currently playing
    │       negative-duration sound without discarding already queued sounds.
    │       The fourth CALL SOUND selector follows the TI-Basic negative noise
    │       values -1..-8 rather than the SN76489 register bits directly.
    │
    │       TiBasicLineReferenceLineMarkerProvider is triggered on the
    │       LINE_NUMBER leaf token of a TiBasicLine. It uses
    │       `tibasic.language.analysis.references.TiBasicInboundLineReferenceCollector`, which scans the file's lines
    │       and reuses PsiElement.lineNumberReferenceNodes() to collect all
    │       inbound jumps per target line number. If at least one other line
    │       refers to the current line, the provider adds a gutter icon with a
    │       compact tooltip summary and standard click navigation to the
    │       referring lines.
    │
    └──▶ Actions                    (`tibasic.ide.actions.*`)
            FormatAction  — reformats the document text via `tibasic.language.format`.
            ResequenceAction — renumbers lines via `tibasic.language.format`.
            Both actions modify the PSI document inside a WriteAction.
```

## TI-Basic debugger architecture (V1)

The upcoming TI-Basic debugger is intentionally a **line-flow simulator**, not a general
interpreter. It starts via the normal IntelliJ **Debug** action, but renders into a
dedicated **TI-Basic Debug** tool window so later stories can add keyboard, joystick, and
other runtime interaction without being constrained by the standard debugger UI.

### Story review

Architecturally, the story fits the existing plugin well:

- The source model is already line-oriented through `TiBasicFile.lines()` and `TiBasicLine.lineNumber()`.
- Existing line-reference helpers already resolve numeric jump targets in the current file.
- The plugin already uses conservative PSI-based traversals for static features, so a small
  dedicated runtime layer is consistent with the current design style.

The main architectural constraint is that the debugger must **not** reuse annotator output as
its execution contract. The current annotator deliberately reports some situations as warnings
that are terminal in debugger V1, for example undefined `GOTO`/`GOSUB` targets and trailing
content after `RETURN`, `END`, or `STOP`. The debugger therefore needs its own runtime
validation rules for the supported statements.

### Entry flow and ownership

```text
IDE Debug action
    -> TI-Basic run/debug configuration producer
    -> TI-Basic run configuration / profile state
    -> ReadAction snapshot of the current file content + PSI
    -> project-level debug session service
    -> TI-Basic Debug tool window
    -> Step / Stop commands against the in-memory session
```

V1 should keep the ownership boundaries small and explicit:

| Component group                | Responsibility                                                                                                         |
|--------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `tibasic.ide.debug.run`        | Hooks the standard IntelliJ Debug entry points to the active TI-Basic file and creates the debug session start request |
| `tibasic.ide.debug`            | Project-level session coordination, frozen snapshot/value objects, inspect evaluation, and stepping runtime            |
| `tibasic.ide.toolwindow.debug` | Dedicated debugger tool-window content that renders the frozen listing and exposes Step/Stop controls                  |

The standard IntelliJ Debug action is therefore the **entry mechanism**, not the long-term UI
host. V1 does not need XDebugger-specific concepts such as stack frames, variable views, or
breakpoint handling.

### Frozen snapshot model

The debugger must execute against the **program state at start time**, not against the live
editor after startup. The start operation should therefore build one immutable snapshot with:

- the exact listing text shown in the debugger window
- all program lines sorted by line number
- a `lineNumber -> index` lookup for fast target resolution
- the initial PC pointing to the smallest existing line number
- the PSI-derived classification for the V1-supported statements

The tool window renders only this snapshot. Later edits in the editor must not mutate the
running session. A restart creates a fresh snapshot.

### Session lifecycle

V1 needs a two-phase session model because terminal states do not close immediately:

| Session state | Meaning                                                                                                                                           |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `Paused`      | Session is active and currently stopped on exactly one program line                                                                               |
| `PendingStop` | The current line has just produced a terminal outcome (`END`, `STOP`, natural end, or runtime error message) and the next Step closes the session |
| `Stopped`     | Session has ended and the tool window shows the finished state until a new session starts                                                         |

The initial session state is `Paused` on the smallest line number. Every **Step** executes the
currently marked line and either:

1. moves the PC to the next paused line,
2. enters `PendingStop`, or
3. closes a previously pending stop.

### Step semantics

V1 step behavior is intentionally narrow and must not infer unsupported TI-Basic control flow.

| Current line kind                                                                                               | Step result                                                                                                                                                                                                                                                                                                                                                                                                  |
|-----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GOTO` with one valid existing target line                                                                      | Jump to target line and pause there                                                                                                                                                                                                                                                                                                                                                                          |
| `GOSUB` with one valid existing target line                                                                     | Push the current line number as return origin, jump to target line, and pause there                                                                                                                                                                                                                                                                                                                          |
| `IF expr THEN thenLine [ELSE elseLine]` with debugger-supported numeric or string comparison semantics          | Evaluate the condition against the current debugger state; if the resulting expression is non-zero, jump to `thenLine`, otherwise jump to `elseLine` or continue at the next higher line when `ELSE` is omitted                                                                                                                                                                                              |
| `CALL COLOR(set,fg,bg)` with debugger-supported numeric expressions                                             | Evaluate and round all three arguments against the current debugger state, update one of the 16 debugger character sets (`32..159`) with the resolved TI colors, keep transparent colors transparent so they render against the active `CALL SCREEN` background, and raise `Bad Value: character set=<n>`, `foreground color=<n>`, or `background color=<n>` when a rounded value falls outside `1..16`      |
| string `LET` with debugger-supported string expressions (`"TEXT"`, `A$`, concatenation, `CHR$`, `SEG$`, `STR$`) | Update the known string-variable map and continue to the next higher line; unknown RHS string references are initialized as `""`, and every intermediate string result is truncated to 255 characters before further reuse, with a debugger warning                                                                                                                                                          |
| `CALL KEY(mode,key,status)` with valid debugger-supported effective mode `1..5`                                 | Evaluate the numeric mode expression, resolve mode `0` to the last used keyboard mode or `5` if none exists yet, show a keyboard-input pane while paused on the line, round the entered scan result, validate it against the effective mode's allowed code set, write `key` to the rounded scan code or `-1`, write `status` to `1` for a key press or `0` for no key, then continue to the next higher line |
| `CALL KEY(mode,key,status)` with rounded mode outside `0..5`                                                    | Show `Bad Value: <value>` and enter `PendingStop`                                                                                                                                                                                                                                                                                                                                                            |
| `CALL SOUND(dur,pitch,vol...)` with debugger-supported numeric expressions                                      | Evaluate all numeric arguments against the current debugger state, round them to TI-Basic sound parameters, reuse the last explicit tone-3 pitch for `-4`/`-8` noise selectors when needed, continue to the next higher line, and hand the resolved playback to the shared sound playback service                                                                                                            |
| bare `RETURN` with non-empty GOSUB stack                                                                        | Pop the most recent GOSUB origin and continue at the smallest line number greater than that origin                                                                                                                                                                                                                                                                                                           |
| bare `RETURN` with empty GOSUB stack                                                                            | Show `Can't do that` and enter `PendingStop`                                                                                                                                                                                                                                                                                                                                                                 |
| bare `END` or `STOP`                                                                                            | Enter `PendingStop` without changing the PC                                                                                                                                                                                                                                                                                                                                                                  |
| `GOTO`/`GOSUB` with missing or non-existing target line                                                         | Show `Bad Line Number` and enter `PendingStop`                                                                                                                                                                                                                                                                                                                                                               |
| malformed or unknown statement                                                                                  | Show `Incorrect Statement` and enter `PendingStop`                                                                                                                                                                                                                                                                                                                                                           |
| any other valid but unsupported statement or invalid line                                                       | Ignore the content and continue to the next higher line number                                                                                                                                                                                                                                                                                                                                               |
| no higher line exists after sequential continuation or return continuation                                      | Enter `PendingStop` as natural program end                                                                                                                                                                                                                                                                                                                                                                   |

For `RETURN`, the continuation line is defined by the story as the smallest line number greater
than the line number of the most recently executed `GOSUB`. V1 therefore stores **origin line
numbers**, not arbitrary resume offsets or statement pointers.

### Runtime validation boundary

The debugger should validate only the statements that V1 actively interprets:

- `GOTO`
- `GOSUB`
- `IF`
- `CALL COLOR`
- simple scalar string `LET`
- `CALL KEY`
- `CALL SOUND`
- `RETURN`
- `END`
- `STOP`

All other statements are execution-transparent in V1 and must be ignored regardless of whether
they parse cleanly, produce annotator warnings, or are completely unknown.

This implies a dedicated runtime validator that works from the snapshot/PSI shape rather than
from editor highlighting. In particular:

- undefined `GOTO`/`GOSUB` targets are **fatal in the debugger** even though the annotator only warns
- `IF` conditions are evaluated against the current debug state; non-zero means `THEN`, zero means `ELSE`,
  and a missing `ELSE` falls through to the next higher line
- debugger-supported `CALL COLOR` manages all 16 character sets in debugger memory, starts them as
  black-on-transparent, and keeps those assignments across later `CALL CLEAR` steps
- numeric contexts and string contexts in debugger-supported statements raise **`String-number mismatch`**
  instead of being treated like malformed statements
- malformed `CALL KEY` statements become **`Incorrect Statement`**
- debugger-supported `CALL KEY` effective modes are `1..5`; rounded mode `0` reuses the last
  successful keyboard mode and falls back to `5` initially, mode `3` accepts only `1..15` and
  `32..95`, mode `4` accepts `1..143`, mode `5` accepts `1..15`, `32..159`, and `187`, and
  values outside `0..5` raise **`Bad Value: <value>`**
- debugger-supported `CALL SOUND` statements evaluate their numeric arguments from the current
  debug state, round them to integer playback parameters, and reuse the most recent explicit
  tone-3 pitch for noise selectors `-4` and `-8`
- trailing content after `RETURN`, `END`, or `STOP` becomes **`Incorrect Statement`** for debugger V1
- malformed `LET` statements also become **`Incorrect Statement`** and enter the same pending-stop flow
- parser permissiveness remains useful, because the runtime can still inspect malformed supported
  statements and map them to the correct TI-Basic error message instead of silently skipping them

### Tool-window architecture

The debugger UI should reuse the existing tool-window style but not the live-file refresh
mechanics of `TiBasicFileToolWindowContent` because debugger content is snapshot-based, not
editor-driven.

Recommended structure:

- `TiBasicDebugToolWindowFactory` creates one debugger content component per project
- `TiBasicDebugToolWindowContent` subscribes to the session service and renders the current session
- a listing component (`JBList`, `JBTable`, or equivalent) shows the frozen source lines, emphasizes actual program line numbers, wraps the code portion after 28 characters to match the TI-99/4A listing width, keeps the current line vertically centered when enough rows exist above and below, and otherwise scrolls to maximize visible source lines without introducing empty rows above the listing
- a full-width footer arguments pane summarizes the current paused statement only when the current
  line has debugger-supported arguments; it grows vertically for multiple argument lines, currently
  resolves `CALL SCREEN` to `color-code = XX (NAME)` against the current debugger state, traces `IF`
  conditions through all evaluated subexpressions with variables substituted by their current values,
  shows `FOR` control values as evaluated `initial-value`, `limit`, and `increment` lines, assigns
  the evaluated initial value to the control variable when stepping the `FOR` line, adds a fourth
  parenthesized iteration-count line, defaults omitted `STEP` clauses to `1`, shows `NEXT` as the
  reused increment plus the adjusted control-variable value, and shows malformed expressions as
  `<incorrect expression>` or `<incorrect expression> (string-number-mismatch)`
- a keyboard-input pane appears for supported `CALL KEY` modes and feeds rounded scan-result input
  back into the paused debug session before the next step; the pane also shows the effective mode's
  allowed code ranges directly next to the mode label
- a TI screen pane sits to the right of the listing and starts as a 24x32 grid of ASCII space characters with screen background color `4` and default character colors `2` on `1`; screen `PRINT` output is written into the 28-column text window from columns `3..30`, wraps after 28 characters, treats `:` as a line-feed separator, and applies an implicit trailing `:` when the `PRINT` statement ends without a separator; cells render through the shared TI character-pattern registry, so built-in glyphs and `CALL CHAR` overrides use the same 8x8 pixel patterns as other screen previews
- a same-height character-set preview sits directly beside the TI screen pane and renders all debugger-managed characters `32..159` as a `16 x 8` grid using the current character patterns, the current `CALL COLOR` foreground/background assignments, and the active `CALL SCREEN` background whenever a character color is transparent; the preview scales with both the available height and width of its pane
- the session service also owns debugger-only side effects that should happen exactly once per step, currently the shared `CALL SOUND` playback trigger for resolved sound semantics
- dedicated numeric- and string-variable panes show all known scalar debugger variables together
  with their TI-Basic internal encodings and normal display values
- a dedicated string-variable pane shows all known scalar string variables in TI-Basic internal storage format (
  `length byte + character bytes`), rendering printable ASCII bytes `32..126` directly as characters
- the current PC is expressed as exactly one highlighted row
- toolbar actions expose **Step** and **Stop**
- an inline status area shows pending runtime messages such as `Can't do that`, `Bad Line Number`,
  or `Incorrect Statement`

Because future stories will add interactive runtime devices, the tool window should reserve a
clear separation between the **listing pane** and future **runtime input/output panes** instead of
hardwiring the entire UI into a single flat table component.

## Variables tool window (`tibasic.ide.toolwindow.variables`)

The Variables tool window lists all scalar and array variables plus user-defined functions
in the active TI-Basic file. It refreshes automatically after every committed document change.

### Data model

| Class                              | Responsibility                                                                                                                                                                                                             |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TiBasicVariableOccurrence`        | Single PSI occurrence: `lineNumber`, `offset`, `accessType` (READ/WRITE/NONE), optional `writtenValue` (`Constant` or `VariableReference` for simple `LET` writes, else `null`)                                            |
| `TiBasicVariableEntry`             | Aggregated entry: `name`, `type`, `occurrences`, optional `arrayDetails`, optional `dimOccurrences`; derived properties: `reads`, `writes`, `lineNumbers`, `dimensionsDisplay`, `valueRange`, `rangeDisplay`, `constValue` |
| `TiBasicArrayDetails`              | Effective array metadata: declared or implicit dimension list plus the active `OPTION BASE` value                                                                                                                          |
| `TiBasicVariableDimensionsDisplay` | Combined array declaration display with clickable DIM-line occurrences plus a DIM-like text such as `DIM A(1-10)`                                                                                                          |
| `TiBasicVariableType`              | Enum: NUMERIC, STRING, NUMERIC_ARRAY, STRING_ARRAY, USER_FUNCTION                                                                                                                                                          |

### Array metadata

`tibasic.language.analysis.variables.TiBasicVariableCollector` resolves array metadata once per file and attaches it to
the single row for each array:

- **Explicit DIM**: the displayed dimension list comes from the DIM subscript expressions.
- **DIM line**: array rows also expose the declaring line number when a DIM statement exists.
- **Implicit arrays**: each used dimension defaults to `10`, so `A(1,2,3)` displays
  `10,10,10`.
- **OPTION BASE**: uses the first valid `OPTION BASE` statement value (`0` or `1`);
  if none is present, the default is `0`.

### Range-value detection

`TiBasicVariableCollector` resolves `TiBasicVariableEntry.valueRange` once per collected entry:

- Only `NUMERIC` and `STRING` scalar types can have a finite `valueRange` (arrays and user functions keep
  `valueRange = null` on the row itself).
- **Never written** (`writes == 0`): `valueRange = ["0"]` (NUMERIC) or `["\"\""]` (STRING).
- **Direct literal writes** contribute that literal to the range.
- **Simple alias writes** such as `G$=E$` inherit the referenced scalar variable's full finite range, with cycle
  protection.
- **Resolvable `FOR` loop writes** contribute the loop variable's iteration values, including explicit `STEP` values and
  singleton numeric aliases in the start/end/step expressions.
- **Statically traceable scalar writes** can also contribute a finite range when the lightweight statement traversal can
  determine the post-write value exactly (for example `READ`/`DATA`-driven writes, simple numeric expressions such as
  `A=5*2`, or accumulator-style updates like `S=S+1` inside statically bounded nested `FOR` loops).
- **Multiple writes** union their finite values, remove duplicates, and sort them in ascending natural order.
- **Any other write is non-resolvable** (`INPUT`, `CALL`, or a write whose post-state cannot be traced statically, for
  example because it depends on a variable with unknown range): `valueRange = null`.

`constValue` remains available as the singleton case of `valueRange`, and `rangeDisplay`
renders the finite list for the tool window column in ascending order, abbreviating
consecutive integer runs of length 3 or more only as ascending interval segments such as
`[-3; -1]` or `[1; 5]`. The
rendered list is capped at 20 numeric items/ranges and 10 string items; when the display
would exceed that limit, the Range cell stays empty.

For arrays, the collector also resolves statically known element values when both the
subscript tuple and the assigned value are reducible to constants. These per-element
ranges stay on the array row as `resolvedArrayElementRanges` and are rendered in the
**Range** column only when the Variables tool window view option enables array constants.
One-dimensional runs of 3 or more consecutive subscripts with the same resolved value are
compacted to a single segment such as `(0-127)="0"`; all other elements stay as individual
entries such as `(32)="FFFFFFFFFFFFFFFF"`. Array-element displays use the same type-based
limits: 20 entries for numeric arrays and 10 entries for string arrays, counted after
compaction.
Assignments with unresolved target subscripts are treated conservatively and suppress
array-element constant display for that array row.

`TiBasicVariableCollector.extractWrittenValue` checks that the RHS `EXPRESSION` node of a
`TiBasicLetStatement` has exactly one non-whitespace child and records either:

- a `Constant` when that child is a `NUMERIC_LITERAL` or `STRING_LITERAL`
- a `VariableReference` when that child is a `VARIABLE_ACCESS` (scalar or array element)

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

| Index               | Name       | Content                                                                                                                                                                |
|---------------------|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0                   | Name       | Variable name                                                                                                                                                          |
| 1                   | Type       | `TiBasicVariableType.displayName`                                                                                                                                      |
| `NAME_COLUMN`       | Name       | Variable name                                                                                                                                                          |
| `TYPE_COLUMN`       | Type       | `TiBasicVariableType.displayName`                                                                                                                                      |
| `DIMENSIONS_COLUMN` | Dimensions | `TiBasicVariableDimensionsDisplay?` — clickable DIM-line prefix plus DIM-like declaration text such as `DIM A(1-10)`; visible only for files with arrays               |
| `WRITES_COLUMN`     | Writes     | `List<TiBasicVariableOccurrence>` — rendered as clickable line-number badges                                                                                           |
| `READS_COLUMN`      | Reads      | `List<TiBasicVariableOccurrence>` — rendered as clickable line-number badges                                                                                           |
| `RANGE_COLUMN`      | Range      | `String?` — scalar value list with interval segments such as `[-1; 1]` or, when enabled, wrapped/compacted array-element values such as `(1)="HELLO"` or `(0-127)="0"` |

`TiBasicVariableTableModel` keeps the array-specific Dimensions column out of the table whenever the
current file has no arrays, then restores it as soon as at least one array entry exists. The
persisted `TiBasicVariableToolWindowSettings` toggle in the tool-window toolbar controls whether
statically known array-element values are shown in the Range column for array rows.

All columns use wrapping renderers, and `TiBasicVariableToolWindowContent` recomputes row
heights automatically after data refreshes and column-width changes so long values remain fully
visible in the current viewport width.

## Character definitions tool window (`tibasic.ide.toolwindow.characters`)

The Character Definitions tool window lists all statically resolvable `CALL CHAR`
definitions in the active TI-Basic file. Definitions remain separate per character
code, even when multiple codes share the same normalized pattern and the same derived
foreground/background color variants. Definitions that differ only in source line are
collapsed into one row with a sorted list of line occurrences.

### Shared static call traversal and collectors

`language/analysis/calls/TiBasicStaticCallStatementTraversal.kt` provides the conservative file traversal
used by both `CALL CHAR` and `CALL COLOR` collection. It performs the shared linear trace
through `READ`/`DATA`, `RESTORE`, simple statically resolvable `FOR`/`NEXT` loops, and
simple statically decidable `IF ... THEN [ELSE]` jumps, and records each encountered
`TiBasicCallStatement` together with the current snapshot of statically known scalar and
array-element values.
For `RESTORE`, the traversal mirrors TI BASIC semantics closely enough for static analysis:
`RESTORE` without a line resets to the first `DATA` line, numbered `RESTORE` targets are
resolved to the next higher `DATA` line, and a target above the highest program line aborts
the trace immediately while a target with only non-`DATA` lines above it aborts at the next
`READ`.

On top of that traversal, `language/analysis/calls/TiBasicCallCharDefinitions.kt` and
`language/analysis/calls/TiBasicCallColorAssignments.kt` provide the cached file-level collectors used by
the tool window:

| Type / function                 | Responsibility                                                                                             |
|---------------------------------|------------------------------------------------------------------------------------------------------------|
| `TiBasicStaticCallStatement`    | One traversed `CALL` statement plus the active `StaticValueSnapshot` at that execution point               |
| `collectCallCharDefinitions()`  | Cached file-level collection of all statically resolvable `CALL CHAR` definitions, sorted by code/line     |
| `resolveCallCharDefinition()`   | Resolves one `TiBasicCallStatement` into a char definition when both code and pattern are statically known |
| `collectCallColorAssignments()` | Cached file-level collection of all statically resolvable `CALL COLOR` assignments                         |
| `resolveCallColorAssignment()`  | Resolves one `TiBasicCallStatement` into a color assignment when set, foreground, and background are known |

Resolution reuses the existing helpers `resolveConstantNumericValue`,
`resolveConstantStringValue`, `normalizeHexPattern`, `asciiCharacterName`,
`callColorCharacterSetRange`, `tiColorAt`, and the rounded `CALL SCREEN` color resolver. This lets the tool window collect both
direct literal/constant-variable definitions, simple statically resolvable numeric code
expressions, array-element aliases such as `F$(I)` after a statically known `LET F$(I)=...`,
and color assignments that can be traced conservatively through the same static control flow.
Each collected `CALL COLOR` assignment also carries the active screen background at that point so
transparent foreground/background colors can render against the same background the TI screen would
use, with `SCREEN(1)` normalized to black.
Definitions or color assignments whose required values are still not statically determinable
are omitted.

### Tool-window table

| Index                          | Name    | Content                                                                                                                                                                             |
|--------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 0 (`CHARACTER_CODE_COLUMN`)    | Code    | Integer character code                                                                                                                                                              |
| 1 (`CHARACTER_ASCII_COLUMN`)   | ASCII   | ASCII name/character or empty                                                                                                                                                       |
| 2 (`CHARACTER_PATTERN_COLUMN`) | Pattern | Normalized 16-digit hex pattern                                                                                                                                                     |
| 3 (`CHARACTER_ICON_COLUMN`)    | Icon    | Base `TiBasicCharPatternIcon` plus distinct `TiBasicColoredCharPatternIcon` variants from `CALL COLOR`, resolved against the active `CALL SCREEN` background for transparent colors |
| 4 (`CHARACTER_LINE_COLUMN`)    | Lines   | Sorted `List<TiBasicCharacterDefinitionOccurrence>`; rendered as clickable line-number list                                                                                         |

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
| Debugger startup                                    | Build the immutable program snapshot inside a `ReadAction`, then hand only snapshot/model objects to the running debug session and tool window.                             |
| Debugger Step/Stop UI                               | Tool-window interactions run on the EDT; the actual step calculation should remain pure in-memory and fast enough to execute without PSI or document writes.                |
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
- **Split token/node types.** `TiBasicTokenTypes` (leaf tokens) lives in `tibasic.language.syntax.lexer`; `TiBasicNodeTypes`
  (composite nodes) lives in `tibasic.language.syntax.parser`. This reflects IntelliJ's own conventions and avoids circular imports
  between lexer and parser.
- **Kotlin extensions on framework types.** Verbose framework calls (e.g., `node.getChildren(null)`) are wrapped in
  extensions (`node.allChildren`) collected in `tibasic.common.ext`. See [`extension-points.md`](extension-points.md) and
  the coding conventions in `.github/copilot-instructions.md`.
- **Debugger runs on a frozen snapshot.** Debugger V1 must not observe live file edits after startup; restart is
  the synchronization point.
- **Debugger validation is separate from the annotator.** The annotator optimizes for editing feedback, whereas
  the debugger must reproduce V1 execution outcomes and runtime messages for the supported statements.

## Built-in expression functions

TI-Basic provides a fixed set of built-in functions usable inside expressions (e.g., `ABS(X)`, `SIN(A)`, `CHR$(65)`).
These are distinct from `CALL` subprograms: they appear inside an expression and return a value, whereas `CALL`
subprograms are stand-alone statements.

### Function registry (`tibasic.language.model` — `TiBasicBuiltInFunctions.kt`)

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
