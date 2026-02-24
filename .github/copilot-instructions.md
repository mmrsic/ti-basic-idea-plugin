# Copilot instructions for this IntelliJ IDEA plugin repository

Purpose

- This document defines repository-wide best practices, conventions and constraints that Copilot should follow when
  suggesting or generating code for this IntelliJ IDEA plugin.
- The goal is clean, maintainable, and secure implementation of plugin features for a retro BASIC language.

General principles

- Write clear, small, well-tested code. Prefer readability over clever or tersely optimized code.
- Add new functionality in isolated, easily testable steps.
- Avoid unnecessary external dependencies. Introduce new dependencies only after discussion or when they provide clear,
  measurable value.
- Follow the existing project structure, coding style, and naming conventions; align with the existing packages (
  `com.github.mmrsic.idea.plugins.tibasic`).
- Do not use code comments to explain implementation details. Instead, make the code self-explanatory: use descriptive
  function and variable names, small well-named functions/methods, and well-structured tests. Comments are allowed only
  in exceptional cases (e.g., unavoidable context for a complex algorithm) and must be explicitly justified.

Code reuse (DRY — Don't Repeat Yourself)

- **No duplication, even in small parts**: as long as extracting shared code requires no disproportionate effort,
  every repeated pattern — even a single expression, range literal, or two-line block — must be extracted into a named
  constant, extension function, or shared helper. The bar for extraction is deliberately low.
- **Magic values must be named constants**: numeric or string literals that carry domain meaning (e.g., valid line
  number bounds, default step values) must be defined as named constants at the appropriate scope, never written inline
  more than once.
- **Identical boilerplate in sibling classes must be lifted**: when two or more classes (e.g., actions, test cases)
  contain identical method bodies, extract a common abstract base class, interface default, or top-level helper.
- **Test helper duplication is code duplication**: shared test setup (e.g., `configureFile`) must live in a common
  test base class, not be copied into each test class.
- **Shared algorithmic skeletons must be parameterized**: when two functions differ only in a single step of an
  otherwise identical loop or traversal, introduce a higher-order function (lambda parameter) rather than duplicating
  the surrounding structure.
- Before introducing new code, search the codebase for existing utilities that already solve the same problem.

Framework class improvements via Kotlin extensions

- **Prefer readable extension properties/functions over raw framework API calls**: whenever a framework method call is
  verbose, requires a dummy argument, or obscures intent, wrap it in a Kotlin extension. Example:
  `node.getChildren(null)` → `node.allChildren` as `val ASTNode.allChildren: Array<ASTNode> get() = getChildren(null)`.
- **Group extensions by the framework type they extend**: place all extensions on `ASTNode` in
  `ASTNodeExtensions.kt`, extensions on `PsiElement` in `PsiElementExtensions.kt`, and so on. Never scatter
  extensions on the same type across multiple files.
- **Extensions go in the main source set** (`src/main/kotlin/.../tibasic/`) unless they are exclusively needed in
  tests, in which case they belong in the test source set.
- **Check the extensions file before calling a raw framework method**: if `ASTNodeExtensions.kt` (or the appropriate
  file) already wraps the method, use the extension instead.

Project structure & build

- Build using Gradle Kotlin DSL with the `org.jetbrains.intellij` plugin. Changes to `build.gradle.kts` must remain
  consistent with `gradle.properties`.
- Do not hard-code versions (Kotlin, IntelliJ Platform) in source code — use `gradle.properties` or version variables
  instead.
- Do not introduce breaking changes to public APIs without a version bump and updates to `plugin.xml`.

IntelliJ-specific rules

- Register extension points declaratively in `src/main/resources/META-INF/plugin.xml`. Avoid duplicate registrations.
- Use `com.intellij.openapi.util.IconLoader.getIcon(...)` or `IconProvider` only for resources under the `resources`
  path; place icons under `src/main/resources/icons/`.
- FileType/Language: implement the `Language` object as a singleton and `LanguageFileType` as an object (Kotlin) or a
  final class (Java).

Syntax / language / PSI

- For simple syntax checks an Annotator or LineMarkerProvider may be sufficient.
- For structured analysis prefer Lexer + Parser (BNF/ANTLR) and a dedicated PSI. Place BNF files in `src/main/grammar`
  and generate PSI consistently.
- PSI/parser changes must be accompanied by tests (happy-path parsing and error cases).

Threading & performance

- Respect the EDT rules: all UI operations (popups, dialogs, editor modifications) must run on the EDT.
- Move long-running work (computations, IO) to background tasks using `ProgressManager.runBackgroundableTask`.
- Use ReadAction/WriteAction correctly (read-only work inside ReadAction, modifications inside WriteAction).
- Avoid expensive operations in Annotators or LineMarkerProviders; these run frequently over many files. Prefer caching
  and incremental strategies.

Resources & internationalization

- Store resources (icons, templates) under `src/main/resources/`; reference them via `getResource`/`IconLoader`.
- Externalize UI strings to `messages` properties when they are reused or need localization.

Tests, quality assurance & debugging

- Use the IntelliJ Plugin Test Framework for unit and integration tests (light and heavy test setups).
- At minimum provide: a parsing happy-path test, an annotator test (error detection), and a line marker/action test.
- Run local tests before committing: `./gradlew test` and `./gradlew runIde` for manual sandbox testing.
- Use the Plugin Verifier (`./gradlew verifyPlugin`) only in CI for the target IDE versions.

Documentation maintenance

- **Keep `README.md` current for users**: whenever a user-visible feature is added, changed, or removed, update the
  relevant section of `README.md` (Features, Supported statements, Error and warning annotations, Code actions, or
  Project structure). The README must always reflect what the plugin actually does.
- **Keep `docs/` current for contributors**: whenever a change affects architecture, grammar, extension points, or
  testing conventions, update the corresponding file in `docs/`:
  - `docs/architecture.md` — package map, data-flow, annotator checks, threading, design decisions
  - `docs/grammar.md` — EBNF grammar, token reference, valid/invalid examples
  - `docs/extension-points.md` — all registered extension points and `tibasic.ext` extensions
  - `docs/testing.md` — test setup, base class, conventions
- **Docs are part of the definition of done**: a PR that adds or changes a feature is not complete until the
  documentation is updated. Add doc updates to the same commit or PR as the code change.
- **Do not duplicate content between `README.md` and `docs/`**: `README.md` is user-facing and summarizes features;
  `docs/` contains implementation detail for contributors. Link from `README.md` to `docs/` rather than copying.

Continuous integration / release

- CI should at minimum run: Gradle clean, build, tests, and plugin verifier. Optionally include code-style checks and
  linters.
- Releases: sign and upload the ZIP produced by the Gradle distribution task (`build/distributions`). Keep the version
  in `plugin.xml` and `build.gradle.kts` synchronized.

Security & licensing

- Do not store secrets or keys in source code or resources.
- Check dependencies for CVEs. Introduce new libraries only when they are well-maintained and have minimal footprint.
- Ensure license compatibility for third-party assets (icons, fonts etc.).

Code style / commit conventions

- Use comma after last argument on multi-line function calls and declarations (trailing commas) for cleaner diffs.
- Prefer newlines before dot in chained calls for better readability, but exclude one-liners.
- Keep the existing Java/Kotlin formatting style. Use `ktlint` or the IDEA inspector if configured project-wide.
- Commit messages: short prefix (e.g., `feat:`, `fix:`, `chore:`), followed by a concise description and an optional
  body.
- Prefer small, focused commits (one meaningful purpose per commit: feature, refactor, test).

Copilot-specific guidelines (how Copilot should operate in this repo)

- Generate only small, reviewed code blocks; avoid large untested implementations.
- For every non-trivial generated change, add at least one test (unit or integration).
- If modifying `plugin.xml`, `build.gradle.kts`, or resources, verify consistency (paths, IDs, versions).
- Do not introduce new external dependencies without discussion (explain in PR and run a CVE check).
- Avoid automatically generating binary blobs (e.g., large images); provide placeholders and maintainer guidance
  instead.
- Respect existing package names and public API surfaces. Changes to public APIs must include a version bump and a
  changelog entry.
- Use US English exclusively for code generation: identifiers, inline strings in code, and any autogenerated comments (
  if allowed) must be written in US English. Documentation, README, or commit messages may remain in German or English
  but should be consistent.

Do / Don't (quick reference)

- Do: write small, tested functions; use background tasks; register extensions in `plugin.xml`.
- Don't: perform UI work off the EDT; put expensive operations in annotators without caching; commit secrets to the
  repository.

Templates & PR checklist

- Local tests: `./gradlew test` (green)
- Build: `./gradlew build` (green)
- Run plugin: `./gradlew runIde` (manual smoke test)
- Plugin verifier: `./gradlew verifyPlugin` (CI check)
- Docs: `README.md` updated for user-facing changes; `docs/` updated for contributor-facing changes
- PR: description, tests, release-notes entry (if relevant), justification for new dependencies

Contacts / further notes

- For architecture or public API changes, mention a maintainer (repo owner) in the PR and discuss before merging.

---

This document is the authoritative source for Copilot behavior in this repository. Keep it up to date if build or
release processes change.
