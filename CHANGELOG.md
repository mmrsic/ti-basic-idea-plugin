# Changelog

All notable changes to the TI-Basic IDEA Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0]

### Added

- **Character and color quick documentation**: `Ctrl+Q` now explains character-code positions in `CALL CHAR`, `CALL HCHAR`, `CALL VCHAR`, `CHR$`, and all arguments of `CALL COLOR`, including resolved values, ASCII mappings, TI color names, and character-set ranges
- **Hex-pattern quick documentation**: `Ctrl+Q` now shows an 8x8 preview for `CALL CHAR` patterns, suitable hexadecimal `DATA` items, and matching string literals
- **Editor auto-pairing**: typing `(` or `"` now inserts matching delimiters, and Backspace removes both quotes of an empty `""` pair
- **Auto line-number workflows**: automatic TI-Basic line-number generation now also supports duplicating lines at the file end and pasting TI-Basic code at the file end, with configurable step size and optional rounding to multiples of 10
- **Display column guides**: optional TI-99/4A 28-column wrap guides can now be shown in the editor without shifting the text layout
- **Line-number navigation and inbound markers**: `Ctrl+B` / `Ctrl+Click` now jump from line-number references to their target lines, and gutter markers show which other lines refer to the current line
- **CALL SOUND gutter playback**: resolvable `CALL SOUND(...)` statements now show a gutter action that plays a preview of the sound
- **Character Definitions tool window**: a new bottom tool window lists statically resolvable `CALL CHAR` definitions with code, ASCII, pattern, icon, and source line
- **Editor settings pages**: new TI-Basic settings pages were added for display column guides, automatic line numbers, and parenthesis auto-close behavior

### Changed

- **Code completion**: completion now inserts the only remaining match immediately, suggests generated line numbers on eligible new trailing lines, offers `DATA` target lines after `RESTORE `, and inserts parentheses automatically for built-in functions, `TAB`, and `CALL` subprograms that take arguments
- **Variables tool window**: array rows now expose dimensions, effective `OPTION BASE`, DIM declaration lines, and constant scalar values in dedicated columns
- **Reformat Code integration**: the standard IDEA **Reformat Code** action is now replaced at runtime for TI-Basic files so the usual shortcut routes to the TI-Basic formatter while other file types keep the platform behavior
- **Display guides rendering**: guides are now painted as an overlay across the file based on the longest line instead of reserving horizontal layout space
- **`DATA` parsing**: bare `DATA` is now treated as valid and behaves like `DATA ""`

### Fixed

- `CALL GCHAR` now requires its third argument to be a numeric variable target; other expressions are highlighted as `INCORRECT STATEMENT`
- `FOR` / `NEXT` imbalance detection now tracks the control variable more precisely and warns on the affected statements
- `CALL COLOR`, `CALL SCREEN`, and `CALL SOUND` previews now resolve not only literals and constant variables, but also simple statically resolvable numeric expressions

## [1.2.0] - 2026-04-04

### Added

- **Find Usages for statements and functions**: `Find Usages` now also works for statement keywords, `CALL` subprogram names, built-in functions, and user-defined functions
- **CALL SCREEN gutter preview**: shows the selected screen color as a gutter swatch for `CALL SCREEN(colorCode)`

### Changed

- **TI Basic Variables tool window**: now shows effective constant values for scalar numeric and string variables in the `Const` column
- **CALL CHAR and CALL COLOR gutter previews**: constant numeric variables are now resolved like literals when preview icons are rendered
- **Variables tool window updates**: line-number links stay in sync after resequencing, and `NEXT` statements are counted as variable reads

### Fixed

- Keywords at the start of variable names are no longer misclassified by the lexer or formatter
- Invalid comparisons such as `IF C><120 THEN 30` are now highlighted as errors
- The Variables tool window no longer throws exceptions for external files or project-disposal edge cases and avoids a slow-operation issue
- A plugin exception in `Find Usages` was fixed
- The overridden IDEA **Reformat Code** action now shows its text correctly

## [1.1.0] - 2026-03-29

### Added

- **CALL CHAR gutter preview**: shows a preview of the custom character bitmap directly in the gutter
- **CALL COLOR gutter preview**: shows a color swatch for the defined foreground/background colors in the gutter
- **Variables Tool Window**: lists all variables defined in the current TI-Basic file
- **Find Variable Usages**: supports finding all usages of a variable across the file

### Changed

- IDEA standard formatter is now used for TI-Basic files (replaces the custom format action shortcut path)

### Fixed

- Auto-completion of variable names was not offered inside `CALL` statements
- Incorrect statements such as `XDIR=!`, `XDIR=+`, and `FOR S=2 TO 16)` were not highlighted as errors
- Missing or superfluous right parenthesis was not highlighted as an error

## [1.0.0] - 2026-03-28

### Added

- Recognizes `.tibasic`, `.ti-basic`, and `.ti.bas` source files with a custom file icon
- Syntax highlighting for keywords, operators, string literals, and line numbers
- Error and warning annotations:
    - Line numbers out of range (1–32767), duplicate line numbers, non-ascending order
    - Type mismatches (string vs. numeric), string–number variable name conflicts
    - Undefined GOTO/GOSUB/ON GOTO/ON GOSUB/RESTORE target line numbers
    - Bad file I/O specifications for PRINT, INPUT, RESTORE, OPEN, CLOSE
    - Invalid CALL subprogram names and argument count/type errors
    - Bad variable names, undefined array dimensions, OPTION BASE violations
    - And many more statement-specific checks
- Code completion (Ctrl+Space) for variables and array subscripts
- Shift+Enter smart handler: auto-inserts the next available line number
- **Format TI-Basic Code** action: uppercases keywords, normalizes whitespace, preserves
  string literals (available in editor popup menu and Code menu)
- **Resequence Line Numbers** action: renumbers all lines with configurable start and step
  (available in editor popup menu and Code menu)
- Full statement set:
    - `LET` (keyword optional), `DEF`, `DIM`, `OPTION BASE`
    - `PRINT` (screen and file output), `DISPLAY`, `INPUT` (screen and file), `READ`, `DATA`,
      `RESTORE` (screen and file)
    - `CALL` with all 10 subprograms: CLEAR, SCREEN, COLOR, HCHAR, VCHAR, GCHAR, CHAR, KEY,
      JOYST, SOUND
    - `GOTO` / `GO TO`, `GOSUB` / `GO SUB`, `RETURN`
    - `ON … GOTO`, `ON … GOSUB`
    - `IF … THEN … [ELSE …]`
    - `FOR … TO … [STEP …]`, `NEXT`
    - `OPEN`, `CLOSE`
    - `RANDOMIZE`, `REM`, `END`, `STOP`
    - `BREAK`, `UNBREAK`, `TRACE`, `UNTRACE`, `DELETE`
- All built-in numeric functions: ABS, ATN, COS, EOF, EXP, INT, LOG, RND, SGN, SIN, SQR, TAN
- All built-in string functions: ASC, CHR$, LEN, POS, SEG$, STR$, VAL
- User-defined functions via `DEF` with type-checked parameters and return values
