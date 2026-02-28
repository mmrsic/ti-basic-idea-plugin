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
Suggests all TI-Basic keywords from `TiBasicKeywords.getKeywords()` and all variables
defined in the current file. Completion is case-insensitive; keywords and variables
appear in separate groups in the popup.

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

### Actions

Both actions are added to the editor popup menu (`EditorPopupMenu`) and the `Code` menu (`CodeMenu`).

| Action ID                       | Class                                        | Description                                           |
|---------------------------------|----------------------------------------------|-------------------------------------------------------|
| `TiBasic.ResequenceLineNumbers` | `tibasic.action.resequence.ResequenceAction` | Renumbers all lines with user-selected start and step |
| `TiBasic.FormatCode`            | `tibasic.action.format.FormatAction`         | Uppercases keywords and removes extraneous whitespace |

Both actions extend `TiBasicFileAction`, which gates execution to TI-Basic files only.

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
| `PsiElementExtensions.kt`       | `PsiElement`       | `firstChildOfType<T>()` — first direct child of the given PSI type                                                                                                                                                                                                                                                         |

When you find yourself calling a raw framework method that is verbose or obscures intent,
add an extension to the appropriate file in `tibasic.ext` and use it everywhere.
