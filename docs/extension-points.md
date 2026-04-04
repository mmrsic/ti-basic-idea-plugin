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

### `completion.contributor`

| Attribute             | Value                                         |
|-----------------------|-----------------------------------------------|
| `language`            | `TI-Basic`                                    |
| `implementationClass` | `tibasic.editor.TiBasicCompletionContributor` |

Provides on-demand completion suggestions (Ctrl+Space only; auto-popup is disabled).
Suggests all TI-Basic keywords from `TiBasicKeywords.getKeywords()`, all variables
defined in the current file, CALL subprogram names (from `TiBasicCallSubprograms`) when
the cursor is immediately after `CALL`, and built-in function names (from
`TiBasicBuiltInFunctions`) in expression context. Keywords, variables, subprogram names,
and function names appear in separate groups in the popup.

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

### `editorActionHandler` — Shift+Enter

| Attribute             | Value                                     |
|-----------------------|-------------------------------------------|
| `action`              | `EditorStartNewLine`                      |
| `implementationClass` | `tibasic.editor.TiBasicShiftEnterHandler` |

Intercepts the Shift+Enter keystroke to insert a new line and automatically prepend the
next logical line number.

---

### `codeInsight.declarativeInlayProvider` — TI-99/4A display column breaks

| Attribute             | Value                                                        |
|-----------------------|--------------------------------------------------------------|
| `language`            | `TI-Basic`                                                   |
| `implementationClass` | `tibasic.editor.TiBasicDisplayColumnHintProvider`            |
| `isEnabledByDefault`  | `true`                                                       |
| `group`               | `OTHER_GROUP`                                                |
| `providerId`          | `tibasic.display.column.hints`                               |
| `bundle`              | `messages.TiBasicBundle`                                     |
| `nameKey`             | `inlay.hints.display.columns.name`                           |

```xml
<codeInsight.declarativeInlayProvider
        language="TI-Basic"
        implementationClass="com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicDisplayColumnHintProvider"
        isEnabledByDefault="true"
        group="OTHER_GROUP"
        providerId="tibasic.display.column.hints"
        bundle="messages.TiBasicBundle"
        nameKey="inlay.hints.display.columns.name"
/>
```

Inserts a `┊` inlay marker at every 28th character of each source line.
The TI-99/4A text mode shows 28 visible columns per screen row; these markers let programmers see
exactly where the real hardware would wrap to the next row without leaving the editor.
The hints can be toggled via *Settings → Editor → Inlay Hints → Other → TI-99/4A Display Column Breaks*.

Implementation uses the **declarative InlayHints API** (`InlayHintsProvider` /
`OwnBypassCollector.collectHintsForFile`). The offset calculation logic is extracted into the
package-level function `displayColumnBreakOffsets(lineStart, lineLength, columnWidth)` in
`TiBasicDisplayColumnHintProvider.kt` — also used directly in tests. The constant
`TI99_4A_DISPLAY_COLUMNS = 28` defines the column width.

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
on-the-fly using `Graphics2D` (HiDPI-safe, no static image files needed).

---

### `codeInsight.lineMarkerProvider` — CALL COLOR

| Attribute             | Value                                               |
|-----------------------|-----------------------------------------------------|
| `language`            | `TI-Basic`                                          |
| `implementationClass` | `tibasic.editor.TiBasicCallColorLineMarkerProvider` |

Displays a split 16×16 color square in the gutter for every `CALL COLOR(spriteNum,fg,bg)` line.
`fg` and `bg` may be integer literals or **constant numeric variables** (variables assigned exactly
one distinct numeric literal throughout the file). The left half shows the foreground TI color,
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
`colorCode` may be an integer literal or a **constant numeric variable**. The square is filled with
the single TI color the screen background is set to. If the argument cannot be resolved to a valid
color index, the square is rendered as a checkerboard (Transparent). Colors map via `TiColor.at(index)`
(1-based, same as CALL COLOR). Rendering uses `TiBasicScreenColorIcon`.

Shared infrastructure:
- Color argument resolution is performed by the package-level `internal fun colorFromArg` in
  `tibasic.editor.TiBasicColorArgResolver`, shared with `TiBasicCallColorLineMarkerProvider`.
- `TiBasicScreenColorIcon` lives in `TiBasicColorPreviewIcon.kt` alongside `TiBasicColorPreviewIcon`;
  both use the shared private `paintColorRect` helper.

---

### Actions

Both actions are added to the editor popup menu (`EditorPopupMenu`) and the `Code` menu (`CodeMenu`).

| Action ID                       | Class                                             | Description                                                                                                                                                                                                                            |
|---------------------------------|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TiBasic.ResequenceLineNumbers` | `tibasic.action.resequence.ResequenceAction`      | Renumbers all lines with user-selected start and step                                                                                                                                                                                  |
| `TiBasic.FormatCode`            | `tibasic.action.format.FormatAction`              | Uppercases keywords and removes extraneous whitespace                                                                                                                                                                                  |
| `ReformatCode` (override)       | `tibasic.action.format.TiBasicReformatCodeAction` | Maps Ctrl+Alt+L to FormatAction for TI-Basic files; delegates to the standard IntelliJ action for all other file types. Registered with `overrides="true"` (IDEA schema warning suppressed with `<!--suppress PluginXmlValidity -->`). |

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

| Attribute        | Value                                                       |
|------------------|-------------------------------------------------------------|
| `implementation` | `tibasic.findusages.TiBasicFindUsagesHandlerFactory`        |

Creates a `TiBasicFindUsagesHandler` for any `TiBasicVariableAccess` element, enabling IDEA
to resolve usages through the plugin's semantic reference model rather than the default
text-search fallback.

---

### `targetElementEvaluator`

| Attribute             | Value                                                  |
|-----------------------|--------------------------------------------------------|
| `language`            | `TI-Basic`                                             |
| `implementationClass` | `tibasic.findusages.TiBasicTargetElementEvaluator`     |

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
file in a sortable table (columns: Name, Type, Writes, Reads, Lines). Clicking any line
number in the Writes, Reads, or Lines column navigates the editor to that line.
The table refreshes automatically on every document edit and whenever the active file changes.

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
| `PsiElementExtensions.kt`       | `PsiElement`       | `firstChildOfType<T>()` — first direct child of the given PSI type (**`tibasic.ext`**)                                                                                                                                                                                                                                     |
| `PsiElementExtensions.kt`       | `PsiElement`       | `containingTiBasicFile` — casts `containingFile` to `TiBasicFile?` (**`tibasic.psi`** — lives alongside the PSI types it returns)                                                                                                                                                                                          |

When you find yourself calling a raw framework method that is verbose or obscures intent,
add an extension to the appropriate file in `tibasic.ext` and use it everywhere.
