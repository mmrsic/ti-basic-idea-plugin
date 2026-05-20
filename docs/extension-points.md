# Extension points

This document lists all IntelliJ Platform extension points registered by the plugin
(`src/main/resources/META-INF/plugin.xml`) and explains how to add new ones.

## Registered extension points

### `fileType`

| Attribute             | Value                           |
|-----------------------|---------------------------------|
| `name`                | `TI-Basic File`                 |
| `language`            | `TI-Basic`                      |
| `implementationClass` | `tibasic.lang.TiBasicFileType`  |
| `extensions`          | `ti-basic`, `tibasic`, `ti.bas` |

Associates files with the plugin's language and provides the MIME type, default extension, and file description.

---

### `fileIconProvider`

| Attribute        | Value                                  |
|------------------|----------------------------------------|
| `implementation` | `tibasic.lang.TiBasicFileIconProvider` |

Supplies a custom icon for TI-Basic files in the project tree. Icons are loaded from
`src/main/resources/icons/` via `IconLoader.getIcon(...)`.

---

### `lang.syntaxHighlighterFactory`

| Attribute             | Value                                               |
|-----------------------|-----------------------------------------------------|
| `language`            | `TI-Basic`                                          |
| `implementationClass` | `tibasic.highlight.TiBasicSyntaxHighlighterFactory` |

Returns a `TiBasicSyntaxHighlighter` instance. The highlighter maps token types to
`TextAttributesKey` colours (keywords, literals, operators, strings, comments).

---

### `lang.documentationProvider`

| Attribute             | Value                                                       |
|-----------------------|-------------------------------------------------------------|
| `language`            | `TI-Basic`                                                  |
| `implementationClass` | `tibasic.editor.TiBasicCharacterCodeDocumentationProvider`  |

Provides Quick Documentation (`Ctrl+Q`) for the character-code argument positions of
`CALL CHAR`, `CALL HCHAR`, `CALL VCHAR`, and the argument of `CHR$`.
The documentation resolves numeric literals, constant numeric variables, and simple
statically resolvable numeric expressions, shows the
ASCII character if one exists, computes the TI-Basic character group for codes
`32..159`, and lists file-local `CALL CHAR` overrides for the same code.

The same provider also handles all three argument positions of `CALL COLOR(set,fg,bg)`.
For each argument it first shows the resolved constant value when one can be determined
from a literal, constant numeric variable, or other simple statically resolvable numeric
expression; otherwise it reports that the value is not
statically determinable. On `set`, it additionally explains the character-set role,
shows the derived character-code range, and lists all ASCII characters within that
range. On `fg` and `bg`, it shows the derived TI color name for the resolved value.

The same provider also recognizes hexadecimal `DATA` items and renders a popup preview
for the corresponding 8×8 character pattern. It accepts quoted and unquoted hex payloads
without a prefix; digit-only items are treated as hex patterns when they start with `0`
and are at most 16 characters long, or when they are 9 to 16 characters long. This keeps
short ordinary decimal `DATA` values such as `30` excluded.

It also provides the same preview for the pattern argument of `CALL CHAR`. There, constant
string expressions use the regular `CALL CHAR` hex normalization rules, so valid character
definitions such as `"30"` remain previewable. Outside `DATA`, plain string literals can
also show the preview; for those generic string literals, the stricter digit-only rule is
used to avoid excessive false positives.

---

### `completion.contributor`

| Attribute             | Value                                         |
|-----------------------|-----------------------------------------------|
| `language`            | `TI-Basic`                                    |
| `implementationClass` | `tibasic.editor.TiBasicCompletionContributor` |

Provides on-demand completion suggestions (Ctrl+Space only; auto-popup is disabled).
If exactly one match remains after filtering, that match is inserted immediately
without showing the lookup popup. Built-in functions and CALL subprograms with
arguments insert `()` and place the caret between them, while generated line
numbers insert a trailing space and place the caret after it.
Suggests all TI-Basic keywords from `TiBasicKeywords.getKeywords()`, all variables
defined in the current file, CALL subprogram names (from `TiBasicCallSubprograms`) when
the cursor is immediately after `CALL`, and built-in function names (from
`TiBasicBuiltInFunctions`) in expression context. At the beginning of the current
eligible unnumbered last line, it also suggests the same next auto-generated line
number that Shift+Enter would insert there. After `RESTORE `, it suggests all line
numbers in the current file that contain `DATA` statements. Keywords, variables,
subprogram names, and function names appear in separate groups in the popup.

---

### `typedHandler`

| Attribute        | Value                                               |
|------------------|-----------------------------------------------------|
| `implementation` | `tibasic.editor.TiBasicPairedCharacterTypedHandler` |

Intercepts typed `(`, `)` and `"` characters in TI-Basic files. It inserts matching
closing delimiters for opening parentheses and quotes, skips over an existing closing
`)` or `"` when that delimiter should be reused, and inserts doubled quotes inside an
existing string literal. Inside string literals it also recognizes 3-digit character-code
triggers such as `\065` or `\255`, replacing them with the corresponding raw character
code for the full range `000..255`. For every CTRL and FCTN key combination defined in
`docs/img/TI-BASIC_key_unit_5.png` it additionally recognizes mnemonic aliases:
CTRL combinations accept forms such as `\C@`, `\CX`, `\C.`, `\C;`, `\C=`, `\C9`,
`\C/`, `\^X`, `\C-X`, and `\CTRL-X`, while
FCTN combinations accept forms such as `\F7`, `\F-7`, and `\FCTN-7`. When the resulting
character is a double quote, the handler inserts the doubled source representation `""`
so the string literal remains valid. For all other non-space characters, when the caret
is after a valid line number or numeric literal and the interpreter would not treat the
typed character as a continuation of that number, it inserts the required separating
space before the typed character.

---

### `backspaceHandlerDelegate`

| Attribute        | Value                                                   |
|------------------|---------------------------------------------------------|
| `implementation` | `tibasic.editor.TiBasicPairedCharacterBackspaceHandler` |

Intercepts Backspace in TI-Basic files for auto-paired string delimiters. When the
caret is between an empty pair of double quotes (`"<caret>"`), deleting the opening
quote also deletes the matching closing quote so empty string literals behave like
parentheses during editing. In all other cases, it delegates to the standard
IntelliJ backspace behavior unchanged.

---

### `lang.parserDefinition`

| Attribute             | Value                                    |
|-----------------------|------------------------------------------|
| `language`            | `TI-Basic`                               |
| `implementationClass` | `tibasic.parser.TiBasicParserDefinition` |

Wires together the lexer (`TiBasicLexer`), the parser (`TiBasicParser`), the file node type
(`TiBasicNodeTypes.FILE`), and the PSI element factory (`createElement()`).

---

### `annotator`

| Attribute             | Value                                |
|-----------------------|--------------------------------------|
| `language`            | `TI-Basic`                           |
| `implementationClass` | `tibasic.highlight.TiBasicAnnotator` |

Performs semantic analysis on the PSI tree and attaches error/warning annotations.
See [`architecture.md`](architecture.md) for the full list of checks.

---

## Registered actions

### `action` — `TiBasic.ShowScreenPreview`

| Attribute | Value                                               |
|-----------|-----------------------------------------------------|
| `id`      | `TiBasic.ShowScreenPreview`                         |
| `class`   | `tibasic.action.preview.TiBasicScreenPreviewAction` |

Adds the **Preview TI-Basic Screen...** action to the editor popup menu and the
Code menu. The action is available in TI-Basic files and is enabled only when the
current selection intersects at least one `CALL HCHAR` or `CALL VCHAR` line. It opens
an explicit dialog with a 32x24 screen preview built from the selected lines, taking
selected `CALL CHAR`, `CALL COLOR`, `CALL SCREEN`, and `CALL CLEAR` statements into
account. `CALL HCHAR` wraps at the right edge into the next row, `CALL VCHAR` wraps at
the bottom edge into the next column, and both continue at the top-left corner after the
last screen cell. When some selected statements cannot be resolved statically, the dialog marks
the result as a partial preview instead of failing silently.

---

### `copyPastePreProcessor`

| Attribute        | Value                                             |
|------------------|---------------------------------------------------|
| `implementation` | `tibasic.editor.TiBasicPastePreProcessor`         |

Intercepts paste operations in TI-Basic files. When the caret (or selection end) is at the
end of the file, replaces the line numbers of any pasted TI-Basic lines with auto-generated
values based on the configured automatic line-number delta and optional 10-rounding mode.
Line number references within statements (GOTO, GOSUB, ON GOTO, ON GOSUB, IF-THEN/ELSE,
RESTORE, BREAK/UNBREAK/TRACE/UNTRACE) are shifted by the same delta as their containing line.
Lines in the pasted text without a valid TI-Basic line number are left unchanged.
Has no effect when pasting into the middle of the file or into non-TI-Basic files.

---

### `editorActionHandler` — Ctrl+D (Duplicate Line)

| Attribute             | Value                                           |
|-----------------------|-------------------------------------------------|
| `action`              | `EditorDuplicate`                               |
| `implementationClass` | `tibasic.editor.TiBasicDuplicateLineHandler`    |

When the caret (or selection end) is at or after the last TI-Basic line, duplicates
the line(s) and replaces the line numbers of the newly inserted copies with
auto-generated values based on the configured automatic line-number delta and optional
10-rounding mode. Line number references within statements (GOTO, GOSUB, ON GOTO,
ON GOSUB, IF-THEN/ELSE, RESTORE, BREAK/UNBREAK/TRACE/UNTRACE) are shifted by the same
delta as their containing line. When the caret is not at the end of the file, delegates to
the standard IntelliJ duplicate handler unchanged. Multiple selected lines at the end
are each renumbered consecutively according to the same configured sequence.

---

### `editorActionHandler` — Shift+Enter

| Attribute             | Value                                     |
|-----------------------|-------------------------------------------|
| `action`              | `EditorStartNewLine`                      |
| `implementationClass` | `tibasic.editor.TiBasicShiftEnterHandler` |

Intercepts the Shift+Enter keystroke to insert a new line and automatically prepend the
next logical line number using the configured automatic line-number delta and optional
10-rounding mode. When `TiBasicParenAutoCloseSettings.autoCloseOnShiftEnter` is enabled
(default: on), it also appends `)` characters for each unclosed `(` on the current line
before inserting the new line.

---

### `editorActionHandler` — Enter

| Attribute             | Value                                |
|-----------------------|--------------------------------------|
| `action`              | `EditorEnter`                        |
| `implementationClass` | `tibasic.editor.TiBasicEnterHandler` |

When `TiBasicParenAutoCloseSettings.autoCloseOnEnter` is enabled (default: off) and the
cursor is at (or near) the end of a TI-Basic line, appends `)` characters for each
unclosed `(` on the current line before delegating to the original Enter handler. Has no
effect on mid-line cursor positions or non-TI-Basic files.

---

### `lang.braceMatcher`

| Attribute             | Value                                |
|-----------------------|--------------------------------------|
| `language`            | `TI-Basic`                           |
| `implementationClass` | `tibasic.editor.TiBasicBraceMatcher` |

Provides bracket-pair definitions so IntelliJ automatically highlights the matching `(`
or `)` when the cursor is adjacent to one. Returns a single non-structural
`BracePair(LPAREN, RPAREN)`.

---

### `applicationService` — `TiBasicParenAutoCloseSettings`

| Attribute               | Value                                          |
|-------------------------|------------------------------------------------|
| `serviceImplementation` | `tibasic.editor.TiBasicParenAutoCloseSettings` |

Persists two boolean settings (`autoCloseOnShiftEnter`, `autoCloseOnEnter`) to
`editor.xml`. Follows the same `PersistentStateComponent` pattern as
`TiBasicColumnHintSettings`.

---

### `applicationService` — `TiBasicAutoLineNumberSettings`

| Attribute               | Value                                          |
|-------------------------|------------------------------------------------|
| `serviceImplementation` | `tibasic.editor.TiBasicAutoLineNumberSettings` |

Persists the automatic line-number settings to `editor.xml`: the numeric delta to add
for generated line numbers and an optional mode that rounds every generated number to a
multiple of 10. When rounding is enabled, generation still advances to the next
strictly greater 10er-Zahl so duplicate line numbers cannot occur.

---

### `applicationConfigurable` — Parenthesis Auto-Close

| Attribute  | Value                                              |
|------------|----------------------------------------------------|
| `parentId` | `editor`                                           |
| `id`       | `tibasic.paren.auto.close`                         |
| `instance` | `tibasic.editor.TiBasicParenAutoCloseConfigurable` |
| `key`      | `paren.auto.close.settings.title`                  |

Settings UI under **Settings › Editor › TI-Basic Parenthesis Auto-Close** with two
checkboxes to independently enable auto-close on Shift+Enter and on Enter.

---

### `applicationConfigurable` — Automatic Line Numbers

| Attribute  | Value                                              |
|------------|----------------------------------------------------|
| `parentId` | `editor`                                           |
| `id`       | `tibasic.auto.line.number`                         |
| `instance` | `tibasic.editor.TiBasicAutoLineNumberConfigurable` |
| `key`      | `auto.line.number.settings.title`                  |

Settings UI under **Settings › Editor › TI-Basic Automatic Line Numbers** with an
integer delta field and a checkbox to round every generated line number to a multiple
of 10.

---

### `postStartupActivity` — ReformatCode override

| Attribute        | Value                                                                |
|------------------|----------------------------------------------------------------------|
| `implementation` | `tibasic.action.format.TiBasicReformatCodeActionOverrideInitializer` |

Replaces the platform `ReformatCode` action after project startup with
`TiBasicReformatCodeAction` via the public `ActionManager.replaceAction()` API. The
replacement keeps the global action ID and shortcut, routes TI-Basic files to
`FormatAction`, and delegates all other files to the standard IntelliJ reformat action.

---

### `postStartupActivity` — TI-99/4A display column guides

| Attribute        | Value                                                             |
|------------------|-------------------------------------------------------------------|
| `implementation` | `tibasic.editor.TiBasicDisplayColumnGuideInitializer`             |

Registers editor lifecycle handling for TI-Basic display column guides. On project startup the
initializer installs a per-editor controller for already open editors and adds an `EditorFactory`
listener for future editors in the same project.

The controller (`TiBasicDisplayColumnGuideController`) adds one `RangeHighlighter` per TI-Basic
editor and assigns a `CustomHighlighterRenderer` (`TiBasicDisplayColumnGuideRenderer`) that draws
thin vertical guide lines at every 28th character position required by the longest line in the
file. Because the guides are painted as an overlay, they do not reserve horizontal space and
therefore do not shift the code layout.

The guide calculation remains centralized in `TiBasicDisplayColumnGuides.kt`: `longestLineLength(...)`
finds the maximum source-line length of the document, and `displayColumnGuideColumns(...)` derives
the global guide columns from that value. The constant `TI99_4A_DISPLAY_COLUMNS = 28` defines the
TI-99/4A screen width.

---

### `applicationConfigurable` — Display Column Guides

| Attribute  | Value                                                  |
|------------|--------------------------------------------------------|
| `parentId` | `editor`                                               |
| `id`       | `tibasic.display.column.guides`                        |
| `instance` | `tibasic.editor.TiBasicDisplayColumnGuideConfigurable` |
| `key`      | `display.columns.settings.title`                       |
| `bundle`   | `messages.TiBasicBundle`                               |

Settings UI under **Settings › Editor › TI-Basic Display Column Guides** with a checkbox to
enable or disable the guides and a numeric preview-distance field that controls how many
characters before the next 28-column wrap boundary the guides become visible (default: 2).
The configurable persists `guidesEnabled` and `guidePreviewDistance` via
`TiBasicColumnHintSettings` and triggers a refresh of all open editors after apply.

---

### `codeInsight.lineMarkerProvider` — CALL CHAR gutter preview

| Attribute             | Value                                              |
|-----------------------|----------------------------------------------------|
| `language`            | `TI-Basic`                                         |
| `implementationClass` | `tibasic.editor.TiBasicCallCharLineMarkerProvider` |

```xml

<codeInsight.lineMarkerProvider
        language="TI-Basic"
        implementationClass="com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicCallCharLineMarkerProvider"
/>
```

Displays a 16×16 px black-and-white character preview in the gutter for every
`CALL CHAR(code,pattern)` line where `pattern` resolves to a valid hex string of 0–16 characters.
`pattern` may be a string literal or a **constant string variable** (a variable that is assigned
exactly one distinct string literal throughout the file). The pattern encodes 8 rows of 8 pixels
each (1 byte per row, MSB = left pixel): `1`-bit → black, `0`-bit → white. Patterns shorter than
16 characters are zero-padded; longer patterns, non-hex content, or non-constant variables produce
no icon. The implementation class is `TiBasicCharPatternIcon`, which renders the bitmap
from a shared `TiBasicCharPatternBitmap` model that is also reused by the `Ctrl+Q` popup
preview for hexadecimal `DATA` items.

---

### `codeInsight.lineMarkerProvider` — CALL COLOR

| Attribute             | Value                                               |
|-----------------------|-----------------------------------------------------|
| `language`            | `TI-Basic`                                          |
| `implementationClass` | `tibasic.editor.TiBasicCallColorLineMarkerProvider` |

Displays a split 16×16 color square in the gutter for every `CALL COLOR(set,fg,bg)` line.
`fg` and `bg` may be integer literals, **constant numeric variables** (variables assigned exactly
one distinct numeric literal throughout the file), or other simple statically resolvable numeric
expressions. The left half shows the foreground TI color,
the right half the background TI color. If an argument cannot be resolved to a constant integer
(non-constant variable, expression, or out-of-range value), the corresponding half is rendered as
a checkerboard (transparent). Colors map via `TiColor.at(index)` (1-based,
index 1 = Transparent through 16 = White). The rendering uses `JBColor` to stay compatible
with IntelliJ's theme system while keeping the fixed TI color palette.

---

### `codeInsight.lineMarkerProvider` — CALL SCREEN

| Attribute             | Value                                                |
|-----------------------|------------------------------------------------------|
| `language`            | `TI-Basic`                                           |
| `implementationClass` | `tibasic.editor.TiBasicCallScreenLineMarkerProvider` |

Displays a solid 16×16 color square in the gutter for every `CALL SCREEN(colorCode)` line.
`colorCode` may be an integer literal, a **constant numeric variable**, or another simple
statically resolvable numeric expression. The square is filled with the single TI color the screen
background is set to. If the argument cannot be resolved to a valid color index, the square is
rendered as a checkerboard (Transparent). Colors map via `TiColor.at(index)`
(1-based, same as CALL COLOR). Rendering uses `TiBasicScreenColorIcon`.

Shared infrastructure:

- Color argument resolution is performed by the package-level `internal fun colorFromArg` in
  `tibasic.editor.TiBasicColorArgResolver`, shared with `TiBasicCallColorLineMarkerProvider`.
- `TiBasicScreenColorIcon` lives in `TiBasicColorPreviewIcon.kt` alongside `TiBasicColorPreviewIcon`;
  both use the shared private `paintColorRect` helper.

---

### `codeInsight.lineMarkerProvider` — CALL SOUND

| Attribute             | Value                                               |
|-----------------------|-----------------------------------------------------|
| `language`            | `TI-Basic`                                          |
| `implementationClass` | `tibasic.editor.TiBasicCallSoundLineMarkerProvider` |

Displays a play icon in the gutter for every resolvable `CALL SOUND(dur,pitch1,vol1[,pitch2,vol2...])`
line.

The duration and every `pitch/vol` pair must resolve to integer constants, either as literals or as
**constant numeric variables** (variables assigned exactly one distinct numeric literal throughout the
file), or as other simple statically resolvable numeric expressions. The resolved values must describe
a playable sound: `abs(dur) >= 1`, every `pitch >= 1`, and every
`vol` in `0..30`. When these conditions are met, clicking the gutter icon dispatches square-wave playback through the
shared `TiBasicSoundPlaybackService`. Positive durations always play to completion and later clicks are queued FIFO;
negative durations are interruptible and a new click replaces the currently playing negative-duration sound while
preserving already queued follow-up sounds.

Shared infrastructure:

- `callStatementForSubprogram(...)` centralizes the common `CALL_*` PSI lookup pattern shared by the
  CHAR/COLOR/SCREEN/SOUND line-marker providers.
- `resolveSoundPlayback(...)` reuses the same constant-expression resolution helpers that already
  power the character and color editor features.
- `TiBasicSoundPlaybackService` renders 16-bit mono PCM square waves, manages the playback queue, and
  delegates output to the `TiBasicAudioOutput` adapter. The default implementation uses the JVM
  `javax.sound.sampled` API so the same plugin logic works across Linux, macOS, and Windows.

---

### `codeInsight.lineMarkerProvider` — inbound line references

| Attribute             | Value                                                   |
|-----------------------|---------------------------------------------------------|
| `language`            | `TI-Basic`                                              |
| `implementationClass` | `tibasic.editor.TiBasicLineReferenceLineMarkerProvider` |

```xml
<codeInsight.lineMarkerProvider
        language="TI-Basic"
        implementationClass="com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicLineReferenceLineMarkerProvider"
/>
```

Displays a gutter icon on a line's `LINE_NUMBER` token when at least one **other** line in the same
file refers to that target line number. The provider is file-local and uses
`TiBasicInboundLineReferenceCollector.collectCached(file)` to build a cached target→referrers index.

The collector deliberately reuses `PsiElement.lineNumberReferenceNodes()` so statement coverage stays
aligned with resequencing and paste/duplicate renumbering. This includes references from `GOTO`,
`GOSUB`, `ON ... GOTO`, `ON ... GOSUB`, `IF ... THEN ... [ELSE ...]`, `RESTORE`, and line-number-list
statements (`BREAK`, `UNBREAK`, `TRACE`, `UNTRACE`).

UX behavior:

- Tooltip: compact summary such as `Referenced by lines 100, 200`
- Click: opens the standard IntelliJ navigation popup with the referring lines
- Self-reference alone does not produce the marker

---

### `gotoDeclarationHandler` — line-number declaration navigation

| Attribute        | Value                                          |
|------------------|------------------------------------------------|
| `implementation` | `tibasic.editor.TiBasicGotoDeclarationHandler` |

```xml
<gotoDeclarationHandler
        implementation="com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicGotoDeclarationHandler"
/>
```

Handles declaration navigation for referenced `NUMERIC_LITERAL` PSI elements in the same
statement contexts already covered by `PsiElement.lineNumberReferenceNodes()`: `GOTO`, `GOSUB`,
`ON ... GOTO`, `ON ... GOSUB`, `IF ... THEN ... [ELSE ...]`, `RESTORE`, and line-number-list
statements (`BREAK`, `UNBREAK`, `TRACE`, `UNTRACE`).

The handler resolves file-locally through `TiBasicFile.lineByNumber(...)`, which enables
standard IntelliJ declaration navigation (`Ctrl+B`, qualified mouse click) from the
referencing numeric literal to the referenced target line. Unresolved or out-of-range line
numbers yield no target, so they do not navigate anywhere.

---

### Actions

Both actions are added to the editor popup menu (`EditorPopupMenu`) and the `Code` menu (`CodeMenu`).

| Action ID                         | Class                                             | Description                                                                                                                                                                                                                                                                                                                              |
|-----------------------------------|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TiBasic.ResequenceLineNumbers`   | `tibasic.action.resequence.ResequenceAction`      | Renumbers all lines with user-selected start and step                                                                                                                                                                                                                                                                                    |
| `TiBasic.FormatCode`              | `tibasic.action.format.FormatAction`              | Uppercases keywords and removes extraneous whitespace                                                                                                                                                                                                                                                                                    |
| `ReformatCode` (runtime override) | `tibasic.action.format.TiBasicReformatCodeAction` | Reuses the platform action ID so Ctrl+Alt+L routes to `FormatAction` for TI-Basic files while delegating to the standard IntelliJ action for all other file types. The runtime replacement is performed by `TiBasicReformatCodeActionOverrideInitializer`; localized text and description come from `action.ReformatCode.*` bundle keys. |

Both `TiBasic.*` actions extend `TiBasicFileAction`, which gates execution to TI-Basic files only.

---

### `lang.findUsagesProvider`

| Attribute             | Value                                          |
|-----------------------|------------------------------------------------|
| `language`            | `TI-Basic`                                     |
| `implementationClass` | `tibasic.findusages.TiBasicFindUsagesProvider` |

Enables **Alt+F7 Find Usages** for TI-Basic variables. Provides a `DefaultWordsScanner`
backed by `TiBasicLexer` so IDEA can find candidate files. `getNodeText` returns the
variable name; `getWordsScanner` indexes `NUMERIC_VARIABLE` and `STRING_VARIABLE` tokens
as identifier words.

`TiBasicVariableAccess` implements `PsiNamedElement` and overrides `getReference()` to
return a `TiBasicVariableReference`. The reference's `isReferenceTo` uses a semantic
comparison (name + token type + isArray flag) so that Find Usages on any occurrence of a
variable finds all occurrences symmetrically.

---

### `findUsagesHandlerFactory`

| Attribute        | Value                                                |
|------------------|------------------------------------------------------|
| `implementation` | `tibasic.findusages.TiBasicFindUsagesHandlerFactory` |

Creates a `TiBasicFindUsagesHandler` for any `TiBasicVariableAccess` element, enabling IDEA
to resolve usages through the plugin's semantic reference model rather than the default
text-search fallback.

---

### `targetElementEvaluator`

| Attribute             | Value                                              |
|-----------------------|----------------------------------------------------|
| `language`            | `TI-Basic`                                         |
| `implementationClass` | `tibasic.findusages.TiBasicTargetElementEvaluator` |

Refines which PSI element IDEA treats as the "target" when the user invokes Find Usages
(Alt+F7) or Navigate → Declaration. Ensures that the caret position resolves to the correct
`TiBasicVariableAccess` node even when the caret is on a surrounding token.

---

### `readWriteAccessDetector`

| Attribute        | Value                                               |
|------------------|-----------------------------------------------------|
| `implementation` | `tibasic.findusages.TiBasicReadWriteAccessDetector` |

Classifies each usage in the Find Usages panel as read (blue) or write (orange/red).
Delegates to `TiBasicVariableCollector.determineAccessType` for the classification logic
(same rules as the Variables tool window).

---

### `toolWindow` — TI Basic Variables

| Attribute      | Value                                                 |
|----------------|-------------------------------------------------------|
| `id`           | `TI Basic Variables`                                  |
| `anchor`       | `bottom`                                              |
| `icon`         | `/icons/ti99_4a_icon_small.svg`                       |
| `factoryClass` | `tibasic.toolwindow.TiBasicVariableToolWindowFactory` |

Provides a dockable bottom panel that lists all variables in the currently active TI-Basic
file in a sortable table (columns: Name, Type, Dimensions, Base, DIM, Writes, Reads,
Range). The Dimensions and Base columns show effective array metadata for DIM
declarations directly on each numeric or string array row, including implicit or explicit
array usages. The DIM column shows the declaration line number on that same array row when
the array has an explicit `DIM`. Clicking any line number in the DIM, Writes,
or Reads column navigates the editor to that line. The Range column shows singleton values
like the previous constant display and otherwise renders finite comma-separated value lists
inferred from simple literal and variable-alias assignments. The table refreshes automatically on
every document edit and whenever the active file changes, and all columns wrap automatically with
row heights adjusted to the available width.

---

### `toolWindow` — TI Basic Character Definitions

| Attribute      | Value                                                             |
|----------------|-------------------------------------------------------------------|
| `id`           | `TI Basic Character Definitions`                                  |
| `anchor`       | `bottom`                                                          |
| `icon`         | `/icons/ti99_4a_icon_small.svg`                                   |
| `factoryClass` | `tibasic.toolwindow.TiBasicCharacterDefinitionsToolWindowFactory` |

Provides a dockable bottom panel that lists all statically resolvable `CALL CHAR`
definitions in the currently active TI-Basic file in a sortable table (columns: Code,
ASCII, Pattern, Icon, Line). Each `CALL CHAR` statement appears as its own row, so repeated
definitions of the same character code remain visible. The Icon column renders the
normalized pattern via `TiBasicCharPatternIcon` and, when available, adds distinct
`TiBasicColoredCharPatternIcon` variants derived from matching statically resolvable
`CALL COLOR(set,fg,bg)` assignments for the same character set. The collectors include
direct literal and simple constant-variable definitions, simple statically resolvable numeric
code expressions, as well as values that can be traced
statically through `READ`/`DATA` statements, `RESTORE`, and simple statically resolvable
`FOR`/`NEXT` loops around such reads, including simple statically decidable
`IF ... THEN [ELSE]` line jumps inside that control flow. Clicking the Line cell navigates
to the corresponding program line. The table refreshes automatically on every document edit
and whenever the active file changes.

---

## Adding a new extension point

1. **Choose the extension point** from the IntelliJ Platform SDK documentation
   (`com.intellij.*` namespace or a plugin-provided namespace).

2. **Implement the required interface or abstract class** in the appropriate sub-package:
    - Language features → `tibasic.lang`
    - Highlighting / inspection → `tibasic.highlight`
    - Editor assistance → `tibasic.editor`
    - Code actions → `tibasic.action` (or a sub-package)

3. **Register in `plugin.xml`** under `<extensions defaultExtensionNs="com.intellij">`:
   ```xml
   <myExtensionPoint
       language="TI-Basic"
       implementationClass="com.github.mmrsic.idea.plugins.tibasic.yourpackage.YourClass"
   />
   ```

4. **Update this file** to document the new extension point.

5. **Add at least one test** (see [`testing.md`](testing.md)).

6. **Run `./gradlew verifyPlugin`** to confirm compatibility with the target IDE version.

## Kotlin extensions on framework classes

The plugin follows a convention of wrapping verbose IntelliJ API calls in Kotlin extension
properties and functions. These are collected in `tibasic.ext`:

| File                            | Type extended      | Extensions                                                                                                                                                                                                                                                                                                                 |
|---------------------------------|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ASTNodeExtensions.kt`          | `ASTNode`          | `allChildren` — all child nodes; `nonWhitespaceChildren` — children excluding whitespace; `firstChildType` — element type of first child; `childrenOfType(type)` — children matching a type; `firstChildOfType(type)` — first child matching a type; `childrenAfter(type)` — children after the first node of a given type |
| `AnnotationHolderExtensions.kt` | `AnnotationHolder` | `error(message, element)`, `error(message, range)` — create error annotations; `warning(message, element)`, `warning(message, range)` — create warning annotations                                                                                                                                                         |
| `PsiElementExtensions.kt`       | `PsiElement`       | `firstChildOfType<T>()` — first direct child of the given PSI type; `lineNumberReferenceNodes()` — all NUMERIC_LITERAL AST nodes that are branch-target line number references (covers GOTO, GOSUB, ON GOTO, ON GOSUB, IF-THEN/ELSE, RESTORE, BREAK/UNBREAK/TRACE/UNTRACE) (**`tibasic.ext`**)                             |
| `PsiElementExtensions.kt`       | `PsiElement`       | `containingTiBasicFile` — casts `containingFile` to `TiBasicFile?` (**`tibasic.psi`** — lives alongside the PSI types it returns)                                                                                                                                                                                          |

When you find yourself calling a raw framework method that is verbose or obscures intent,
add an extension to the appropriate file in `tibasic.ext` and use it everywhere.
