# Changelog

All notable changes to the TI-Basic IDEA Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
