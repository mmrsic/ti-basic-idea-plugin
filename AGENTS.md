# AGENTS.md — TI-Basic IDEA Plugin

IntelliJ IDEA plugin providing language support for TI-Basic / TI Extended Basic (TI-99/4A).
All sources live under `com.github.mmrsic.idea.plugins.tibasic` (abbreviated `tibasic` below).

## Essential commands

```bash
./gradlew test                                         # run all tests
./gradlew test --tests "*.TiBasicParserTest"           # single class
./gradlew test --tests "*.TiBasicParserTest.test valid print line"  # single method
./gradlew build                                        # compile + test
./gradlew runIde                                       # sandbox IDE with plugin loaded
./gradlew verifyPlugin                                 # CI-only compatibility check
```

Test reports: `build/reports/tests/test/index.html`

## Pipeline: source text → editor features

```
Source text → TiBasicLexer (line-based regex classifier)
           → TiBasicParser (PsiBuilder composite AST)
           → PSI tree (TiBasicFile / TiBasicLine / TiBasicXxxStatement)
           → TiBasicAnnotator   (semantic errors/warnings + quick-fixes)
           → TiBasicSyntaxHighlighter   (token colours)
           → TiBasicCompletionContributor  (Ctrl+Space only, no auto-popup)
           → Actions (FormatAction, ResequenceAction via WriteAction on EDT)
```

**Key design**: the lexer does the heavy lifting (regex classification per line into
`VALID_STATEMENT`, `LET_IMPLICIT_STATEMENT`, `UNKNOWN_STATEMENT`, `NO_LINE_NUMBER`, …).
The parser only structures pre-classified token streams — it is intentionally permissive
(type-mismatched tokens are accepted) so the annotator can report specific error messages.

## Critical domain constants and registries

| Symbol                              | Location                          | Purpose                                                                                                                        |
|-------------------------------------|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `VALID_LINE_NUMBER_RANGE`           | `psi/TiBasicPsiElements.kt`       | `1..32767` — used by annotator and resequence                                                                                  |
| `TiBasicTokenTypes`                 | `lexer/TiBasicTokenTypes.kt`      | All leaf token `IElementType` constants                                                                                        |
| `TiBasicNodeTypes`                  | `parser/TiBasicNodeTypes.kt`      | All composite AST node constants                                                                                               |
| `TiBasicCallSubprograms`            | `lang/TiBasicCallSubprograms.kt`  | Registry for all 10 `CALL` subprograms                                                                                         |
| `TiBasicBuiltInFunctions`           | `lang/TiBasicBuiltInFunctions.kt` | Registry for all expression functions (`ABS`, `SIN`, …)                                                                        |
| `BAD_NAME_RUNTIME_ERROR`            | `lang/TiBasicCallSubprograms.kt`  | Message constant: `"Will cause run-time error 'BAD NAME'"` — used in `CallSubprogramSignature.syntaxViolationError`            |
| `INCORRECT_STATEMENT_RUNTIME_ERROR` | `lang/TiBasicCallSubprograms.kt`  | Message constant: `"Will cause run-time error 'INCORRECT STATEMENT'"` — used in `CallSubprogramSignature.syntaxViolationError` |

**Adding a built-in function**: add one entry to `TiBasicBuiltInFunctions.signatures` — no other change needed (works
for both numeric-returning and string-returning functions).
**Adding a CALL subprogram**: add one entry to `TiBasicCallSubprograms.signatures` — annotator picks it up
automatically.
**Adding a statement keyword**: add token type → `TiBasicTokenTypes`, node type → `TiBasicNodeTypes`, extend
`VALID_LINE` regex and `tokenizeValidLine` in `TiBasicLexer`, add `parseXxxStatement` to `TiBasicParser`, add
`TiBasicXxxStatement` PSI class, register in `TiBasicParserDefinition.createElement`, add to `TiBasicSyntaxHighlighter`
KEYWORD list, add to `TiBasicKeywords`. See `DEF_KEYWORD` / `TiBasicDefStatement` as the reference implementation.

## Framework extension wrappers — always use these

`tibasic.ext` wraps verbose IntelliJ APIs; use them instead of raw framework calls:

```kotlin
// ASTNodeExtensions.kt
node.allChildren          // instead of node.getChildren(null)
node.nonWhitespaceChildren
node.childrenOfType(type)
node.firstChildOfType(type)
node.firstChildType       // shorthand for firstChildNode?.elementType
node.childrenAfter(type)
node.childSequence

// AnnotationHolderExtensions.kt
holder.error("Bad line number", element, quickFix)
holder.error("Bad line number", range)            // TextRange overload
holder.warning("Undefined line", element)
holder.warning("Undefined line", range)           // TextRange overload

// ext/PsiElementExtensions.kt + psi/PsiElementExtensions.kt
element.firstChildOfType<T>()
element.containingTiBasicFile  // from psi/PsiElementExtensions.kt — casts containingFile to TiBasicFile?
```

Never call `node.getChildren(null)` directly.

## Test base class

Annotator, action, and editor test classes extend `TiBasicTestBase` (`src/test/kotlin/…/tibasic/TiBasicTestBase.kt`):

```kotlin
val file = configureFile("100 PRINT \"HELLO\"")  // creates in-memory .tibasic, runs full pipeline
val annotations = myFixture.doHighlighting()      // annotator tests
myFixture.completeBasic()                         // completion tests
myFixture.performEditorAction("EditorStartNewLine") // editor handler tests
```

Parser-only tests extend `ParsingTestCase` directly (not `TiBasicTestBase`):

```kotlin
class TiBasicCallParserTest : ParsingTestCase("", "tibasic", TiBasicParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"
    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile
}
```

Annotator tests are split into focused classes by statement type:
`TiBasicAnnotatorTest` (general), `TiBasicCallAnnotatorTest`, `TiBasicDefAnnotatorTest`,
`TiBasicDimAnnotatorTest`, `TiBasicFunctionCallAnnotatorTest`, `TiBasicOpenCloseAnnotatorTest`.

Parser tests follow the same split:
`TiBasicParserTest` (general), `TiBasicCallParserTest`, `TiBasicDefParserTest`,
`TiBasicDimParserTest`, `TiBasicFunctionCallParserTest`, `TiBasicOpenCloseParserTest`.

Format tests: `FormatCodeTest`, `FormatCallCodeTest`, `FormatActionTest`.

Line marker tests: `TiBasicCallCharLineMarkerTest`, `TiBasicCallColorLineMarkerTest`.

Find Usages tests: `TiBasicFindUsagesTest`, `TiBasicFindUsagesHandlerTest`.

Tool window tests: `TiBasicVariableCollectorTest`.

Every non-trivial change needs a happy-path test **and** an error-case test.

## Registering new extension points

Declare in `src/main/resources/META-INF/plugin.xml`. Never add duplicates.
See `docs/extension-points.md` for every registered EP and the required XML snippet.

## Docs are part of done

- User-visible change → update `README.md` (Features / Supported statements / Error annotations / Code actions)
- Architecture/grammar/EP/test change → update the relevant file in `docs/`
- Detail in `docs/architecture.md`, `docs/grammar.md`, `docs/extension-points.md`, `docs/testing.md`

## Threading rules

- Annotator / SyntaxHighlighter / CompletionContributor: read-only PSI — no explicit `ReadAction` needed.
- `FormatAction` / `ResequenceAction`: all document mutations via `PsiFileUtils.replaceFileText` / `replaceRange` (wraps
  `WriteCommandAction`); called from EDT.
- No background tasks exist yet; use `ProgressManager.runBackgroundableTask` for any future slow operation.

