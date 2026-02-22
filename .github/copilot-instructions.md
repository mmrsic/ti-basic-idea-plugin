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
- Never run local tests automatically, always let user run them: `./gradlew test` and `./gradlew runIde` for manual sandbox testing.
- Use the Plugin Verifier (`./gradlew verifyPlugin`) only in CI for the target IDE versions.

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
- PR: description, tests, release-notes entry (if relevant), justification for new dependencies

Contacts / further notes

- For architecture or public API changes, mention a maintainer (repo owner) in the PR and discuss before merging.

---

This document is the authoritative source for Copilot behavior in this repository. Keep it up to date if build or
release processes change.
