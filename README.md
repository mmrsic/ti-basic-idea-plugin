# TI-Basic IDEA Plugin

An IntelliJ IDEA plugin that adds language support for **TI-Basic** and **TI Extended Basic** —
the BASIC dialects of the Texas Instruments TI-99/4 and TI-99/4A home computers.

## Features

### Language support

- Recognizes TI-Basic source files with extensions `.ti-basic`, `.tibasic`, `.ti.bas`
- Custom file icon for TI-Basic source files
- Each source line must start with a **line number** in the range **1–32767**; lines without a number are flagged as
  errors
- Typing a letter directly after a bare line number automatically inserts the required separating space so the result
  matches the formatter (for example `100P` becomes `100 P`, while `100+` stays `100+`)
- Typing a letter directly after a numeric literal automatically inserts a separating space when the formatter would
  also keep one there (for example `IF X=10T` becomes `IF X=10 T`, while separators and operators such as `,`, `;`,
  `:`, and `+` stay attached as in `PRINT 1,`, `PRINT 1;`, or `LET X=10+`); this is skipped inside string literals,
  `REM` text, and `DATA` lines, and still does not trigger while the typed character can continue the number (for
  example `.`, `E`/`e`, or an exponent sign)
- Automatic line-number insertion for **Shift+Enter**, **Duplicate at end**, and **Paste at end** supports a configurable delta and optional rounding to multiples of 10
- Typing `(` or `"` inserts the matching closing delimiter; Backspace between an empty `""` pair removes both quotes
- Inside string literals, typing a **3-digit** trigger like `\065` or `\255` inserts the corresponding raw character code (`000..255` supported); in addition, CTRL aliases such as `\C@`, `\CX`, `\C.`, `\C;`, `\C=`, `\C9`, and `\C/` as well as FCTN aliases such as `\F7` and `\FCTN-S` are supported
- **Preview TI-Basic Screen** action renders a **32x24** screen preview for the current selection when it contains `CALL HCHAR` or `CALL VCHAR`; selected `CALL CHAR`, `CALL COLOR`, `CALL SCREEN`, and `CALL CLEAR` statements are applied as preview context, `CALL HCHAR`/`CALL VCHAR` wrap across screen rows/columns like the TI-99/4A does, and unresolved statements are reported as a partial preview

### Supported statements

| Statement                    | Description                                                                                                                                                                                                                                             |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `LET`                        | Assign a value to a variable (`LET` keyword is optional)                                                                                                                                                                                                |
| `DEF`                        | Define a user function: `DEF name[(param)] = expression`; name is a variable name (string if body is string)                                                                                                                                            |
| `DIM`                        | Declare array dimensions: `DIM name(size[,size…])`, comma-separated; must appear before first array use                                                                                                                                                 |
| `OPTION BASE`                | Set the minimum array index to `0` (default) or `1`; only integer literals 0 or 1 are allowed                                                                                                                                                           |
| `PRINT`                      | Output values or text to screen or file; screen form: `PRINT [args]`; file form: `PRINT #fileNumber[.REC recordNumber]:args`; multiple expressions separated by `;`, `,`, or `:`                                                                        |
| `DISPLAY`                    | Output values or text to screen only; identical syntax to `PRINT`                                                                                                                                                                                       |
| `INPUT`                      | Read keyboard input into one or more variables (optional string prompt); file I/O variant: `INPUT #fileNumber[.REC recordNumber]:variableList` (trailing comma allowed)                                                                                 |
| `READ`                       | Read values from DATA statements into one or more variables                                                                                                                                                                                             |
| `DATA`                       | Supply a comma-separated list of values for `READ` statements                                                                                                                                                                                           |
| `RESTORE`                    | Reset the DATA pointer (optionally to a specific line number); or reposition within an open file (`RESTORE #fileNumber [,REC recordNumber]`)                                                                                                            |
| `CALL`                       | Invoke a built-in TI-Basic subprogram (see table below)                                                                                                                                                                                                 |
| `REM`                        | Remark / comment                                                                                                                                                                                                                                        |
| `END`                        | Halt program execution (by convention the last line)                                                                                                                                                                                                    |
| `STOP`                       | Halt program execution (by convention used mid-program)                                                                                                                                                                                                 |
| `RANDOMIZE`                  | Seed the random-number generator; optional numeric expression (integer part used as seed)                                                                                                                                                               |
| `GOTO` / `GO TO`             | Unconditional branch to the given line number                                                                                                                                                                                                           |
| `GOSUB` / `GO SUB`           | Call a subroutine at the given line number; execution returns after the matching `RETURN`                                                                                                                                                               |
| `RETURN`                     | Return from a subroutine to the statement after the most recent `GOSUB`                                                                                                                                                                                 |
| `ON … GOTO` / `ON … GO TO`   | Computed branch to one of several line numbers                                                                                                                                                                                                          |
| `ON … GOSUB` / `ON … GO SUB` | Computed subroutine call to one of several line numbers                                                                                                                                                                                                 |
| `IF … THEN … [ELSE …]`       | Conditional branch; numeric expression selects target                                                                                                                                                                                                   |
| `FOR … TO … [STEP …]`        | Counted loop with numeric control variable, start, limit, and optional step                                                                                                                                                                             |
| `NEXT`                       | Marks the end of the counted loop body                                                                                                                                                                                                                  |
| `DELETE`                     | Delete a string expression                                                                                                                                                                                                                              |
| `OPEN`                       | Open a file: `OPEN #fileNumber:fileName[,org][,type][,mode][,format][,PERMANENT]`; `#` is mandatory; file number 1–255; options in any order: `SEQUENTIAL`/`RELATIVE [n]`, `DISPLAY`/`INTERNAL`, `INPUT`/`OUTPUT`/`APPEND`/`UPDATE`, `FIXED`/`VARIABLE` |
| `CLOSE`                      | Close a file: `CLOSE #fileNumber[:DELETE]`; `#` is mandatory; file number 1–255; optional `:DELETE` deletes the file on close                                                                                                                           |
| `BREAK` / `UNBREAK`          | Set or clear breakpoints at given line numbers                                                                                                                                                                                                          |
| `TRACE` / `UNTRACE`          | Enable or disable execution tracing at given line numbers                                                                                                                                                                                               |

Lines whose keyword is not one of the above are flagged as unknown statements.

### CALL subprograms

| Subprogram                   | Signature                                             | Description                                                                    |
|------------------------------|-------------------------------------------------------|--------------------------------------------------------------------------------|
| `CALL CLEAR`                 | No arguments                                          | Clear the screen                                                               |
| `CALL SCREEN(color)`         | 1 numeric                                             | Set the screen background color                                                |
| `CALL COLOR(set,fg,bg)`      | 3 numerics                                            | Set foreground/background colors for a character set                           |
| `CALL HCHAR(row,col,ch[,n])` | 3 numerics; 4th optional (default 1)                  | Print character horizontally (optionally n times)                              |
| `CALL VCHAR(row,col,ch[,n])` | 3 numerics; 4th optional (default 1)                  | Print character vertically (optionally n times)                                |
| `CALL GCHAR(row,col,var)`    | 2 numerics, 1 numeric variable or array entry         | Read character at position into a variable                                     |
| `CALL CHAR(code,pattern$)`   | 1 numeric, 1 string                                   | Define a custom character pattern                                              |
| `CALL KEY(unit,key,status)`  | 3 numerics                                            | Read keyboard input                                                            |
| `CALL JOYST(unit,x,y)`       | 3 numerics                                            | Read joystick input                                                            |
| `CALL SOUND(dur,freq,vol…)`  | 3, 5, 7, or 9 numerics (`dur`, then `freq/vol` pairs) | Play up to three tones plus one noise channel; negative `dur` is interruptible |

### Expressions

- **Numeric literals** — integers, decimals, scientific notation (e.g. `1.5E-3`)
- **String literals** — double-quoted (e.g. `"HELLO"`)
- **Numeric variables** — 1–15 characters; starts with a letter (or `@`, `[`, `]`, `\`, `_`); remaining characters are
  letters, digits, `@`, or `_` (e.g. `A`, `X1`, `COUNTER`)
- **String variables** — same naming rules as numeric, ending with `$` (e.g. `A$`, `STR$`)
- **Array subscripts** — up to 3 dimensions (e.g. `A(1)`, `B(1,2)`, `C(1,2,3)`)
- **Operators** — `+`, `-`, `*`, `/`, `^` (power), `&` (string concatenation)
- **Comparisons** — `=`, `<>`, `<`, `>`, `<=`, `>=`
- Parentheses for grouping; `^` is right-associative, all others left-associative

### Built-in functions

Built-in functions appear directly inside expressions and return a value (unlike `CALL` subprograms, which are
stand-alone statements).

| Function             | Arguments            | Returns | Description                                                                         |
|----------------------|----------------------|---------|-------------------------------------------------------------------------------------|
| `ABS(x)`             | 1 numeric            | numeric | Absolute value                                                                      |
| `ATN(x)`             | 1 numeric            | numeric | Arctangent in radians                                                               |
| `COS(x)`             | 1 numeric            | numeric | Cosine in radians                                                                   |
| `EOF(n)`             | 1 numeric            | numeric | End-of-file status for file *n*: 0 = not at end, 1 = logical end, −1 = physical end |
| `EXP(x)`             | 1 numeric            | numeric | *e* to the power *x*                                                                |
| `INT(x)`             | 1 numeric            | numeric | Greatest integer ≤ *x*                                                              |
| `LOG(x)`             | 1 numeric            | numeric | Natural logarithm                                                                   |
| `RND`                | none                 | numeric | Random number in [0, 1)                                                             |
| `SGN(x)`             | 1 numeric            | numeric | Sign of *x*                                                                         |
| `SIN(x)`             | 1 numeric            | numeric | Sine in radians                                                                     |
| `SQR(x)`             | 1 numeric            | numeric | Square root                                                                         |
| `TAN(x)`             | 1 numeric            | numeric | Tangent in radians                                                                  |
| `ASC(s$)`            | 1 string             | numeric | ASCII code of first character                                                       |
| `LEN(s$)`            | 1 string             | numeric | Length of string                                                                    |
| `POS(s$,t$,n)`       | 2 strings, 1 numeric | numeric | Position of *t$* in *s$* starting at *n*                                            |
| `VAL(s$)`            | 1 string             | numeric | Numeric value of string                                                             |
| `CHR$(n)`            | 1 numeric            | string  | Character with ASCII code *n*                                                       |
| `SEG$(s$,start,len)` | 1 string, 2 numerics | string  | Substring of *s$*                                                                   |
| `STR$(x)`            | 1 numeric            | string  | String representation of *x*                                                        |

### Error and warning annotations

The annotator inspects every file and highlights:

| Severity | Check                                                                                                                                 |
|----------|---------------------------------------------------------------------------------------------------------------------------------------|
| Error    | Line number out of range (< 1 or > 32767)                                                                                             |
| Error    | Duplicate line numbers                                                                                                                |
| Warning  | Line numbers not in ascending order                                                                                                   |
| Error    | Line without a line number                                                                                                            |
| Error    | Unknown statement keyword                                                                                                             |
| Error    | Variable name that is a reserved keyword or command                                                                                   |
| Error    | Conflicting variable usage (scalar vs. array)                                                                                         |
| Error    | Empty subscript or more than 3 subscript dimensions                                                                                   |
| Error    | Type mismatch (numeric value where string is required, or vice versa)                                                                 |
| Error    | String-number mismatch in LET assignment (variable type differs from expression)                                                      |
| Error    | LET with an invalid variable name (Bad variable name)                                                                                 |
| Error    | LET with trailing tokens after the expression (Incorrect statement)                                                                   |
| Error    | `END` or `STOP` with trailing content (Incorrect statement)                                                                           |
| Warning  | Reference to an undefined line number in BREAK/UNBREAK/TRACE/UNTRACE                                                                  |
| Error    | `GOTO` / `GO TO` without a numeric line number, or with extra content (Incorrect statement)                                           |
| Warning  | `GOTO` / `GO TO` reference to an undefined line number                                                                                |
| Error    | `ON … GOTO` with string expression (String-number mismatch)                                                                           |
| Error    | `ON … GOTO` missing expression, GOTO keyword, or line numbers (Incorrect statement)                                                   |
| Error    | `ON … GOTO` line number out of range 1–32767 (Bad line number)                                                                        |
| Warning  | `ON … GOTO` reference to an undefined line number                                                                                     |
| Error    | `IF … THEN` with string expression (String-number mismatch)                                                                           |
| Error    | `IF … THEN` missing expression, THEN keyword, or THEN line number (Incorrect statement)                                               |
| Error    | `IF … THEN` / `ELSE` line number out of range 1–32767 (Bad line number)                                                               |
| Warning  | `IF … THEN` / `ELSE` reference to an undefined line number                                                                            |
| Error    | `FOR` missing `=`, `TO`, control variable, or a required expression (Incorrect statement)                                             |
| Error    | `FOR` or `NEXT` control variable is a string variable (Numeric variable expected)                                                     |
| Error    | `FOR` initial value, limit, or step is a string expression (String-number mismatch)                                                   |
| Warning  | Unequal number of `FOR` and `NEXT` statements — surplus occurrences flagged (FOR-NEXT-ERROR)                                          |
| Error    | `NEXT` without a control variable (Incorrect statement)                                                                               |
| Error    | `INPUT` without a variable list (Incorrect statement)                                                                                 |
| Error    | `INPUT` with a bad variable name (Bad variable name)                                                                                  |
| Error    | `INPUT #…` missing `:` separator before variable list (Incorrect statement)                                                           |
| Error    | `INPUT #…` missing variable list after `:` (Incorrect statement)                                                                      |
| Error    | `INPUT #…` with file number 0 — reserved for screen (File number 0 is reserved for screen)                                            |
| Error    | `INPUT #…` with literal file number outside 1–255 (File number must be between 1 and 255)                                             |
| Error    | `INPUT #…` with a string expression as file number (Numeric expression expected)                                                      |
| Error    | `INPUT #….REC` with a string expression as record number (Numeric expression expected)                                                |
| Error    | `INPUT #….` with unrecognised modifier instead of `REC` (Incorrect statement)                                                         |
| Error    | `READ` without a variable list (Incorrect statement)                                                                                  |
| Error    | `READ` with a bad variable name (Bad variable name)                                                                                   |
| Error    | `PRINT` or `DISPLAY` with two adjacent expressions missing a separator (Separator expected between expressions)                       |
| Error    | `PRINT` or `DISPLAY` with an invalid token that is not an expression or separator (PRINT argument must be an expression)              |
| Error    | `PRINT #…` missing file number expression after `#` (Incorrect statement)                                                             |
| Error    | `PRINT #…` missing `:` separator before argument list (Incorrect statement)                                                           |
| Error    | `PRINT #…` with file number 0 — reserved for screen (File number 0 is reserved for screen)                                            |
| Error    | `PRINT #…` with literal file number outside 1–255 (File number must be between 1 and 255)                                             |
| Error    | `PRINT #…` with a string expression as file number (Numeric expression expected)                                                      |
| Error    | `PRINT #….REC` with a string expression as record number (Numeric expression expected)                                                |
| Error    | `PRINT #….` with unrecognised modifier instead of `REC` (Incorrect statement)                                                         |
| Error    | `RESTORE` with invalid argument — not a single numeric literal (Incorrect statement)                                                  |
| Warning  | `RESTORE` references a line number that does not exist in the program                                                                 |
| Error    | `RESTORE #0` — file number 0 is reserved for screen (File number 0 is reserved for screen)                                            |
| Error    | `RESTORE #…` with literal file number outside 1–255 (File number must be between 1 and 255)                                           |
| Error    | `RESTORE #…` with a string expression as file number (Numeric expression expected)                                                    |
| Error    | `RESTORE #…,REC` with a string expression as record number (Numeric expression expected)                                              |
| Error    | `RESTORE #…` with malformed file spec (Incorrect statement)                                                                           |
| Error    | `CALL` with unknown subprogram name                                                                                                   |
| Error    | `CALL SCREEN`, `HCHAR`, `VCHAR`, `GCHAR`, `COLOR` with wrong argument count or type — will cause run-time error `INCORRECT STATEMENT` |
| Error    | `CALL` with wrong number of arguments for other subprograms                                                                           |
| Warning  | `CALL` with a type mismatch in any argument for `CHAR`, `KEY`, `JOYST`, `SOUND`                                                       |
| Error    | `CALL CLEAR` with any trailing tokens on the same line — will cause run-time error `BAD NAME`                                         |
| Error    | Built-in function with wrong argument count — will cause run-time error `INCORRECT STATEMENT`                                         |
| Error    | Built-in function with a type-mismatched argument — will cause run-time error `INCORRECT STATEMENT`                                   |
| Error    | `DEF` without a function name (Incorrect statement)                                                                                   |
| Error    | `DEF` with an invalid function name (Bad variable name)                                                                               |
| Error    | `DEF` missing `=` (Incorrect statement)                                                                                               |
| Error    | `DEF` missing body expression after `=` (Incorrect statement)                                                                         |
| Error    | `DEF` type mismatch — string function name with numeric body, or vice versa (String-number mismatch)                                  |
| Error    | `DEF` with an invalid parameter name (Bad variable name)                                                                              |
| Error    | `DEF` parameter used with subscripts inside the body expression (Incorrect statement)                                                 |
| Warning  | Duplicate `DEF` for the same function name                                                                                            |
| Warning  | `DEF` body expression directly references the function itself (self-reference not allowed)                                            |
| Error    | `DIM` without any array entry (Incorrect statement)                                                                                   |
| Error    | `DIM` with an invalid variable name (Bad variable name)                                                                               |
| Error    | `DIM` entry without parenthesised dimension (Incorrect statement)                                                                     |
| Error    | `DIM` dimension contains a variable (Variable not allowed as DIM dimension)                                                           |
| Error    | `DIM` dimension is a floating-point literal (Float not allowed as DIM dimension)                                                      |
| Error    | `DIM` dimension is an expression rather than a plain integer literal (Integer expected as DIM dimension)                              |
| Error    | Same array name declared more than once with `DIM` across the whole file (Duplicate DIM for array name …)                             |
| Warning  | `DIM` statement appears after the first use of the array it declares (DIM for … must appear before first use at line …)               |
| Error    | `OPTION BASE` without a value (Incorrect statement)                                                                                   |
| Error    | `OPTION BASE` with a variable as value (Variable not allowed as OPTION BASE value)                                                    |
| Error    | `OPTION BASE` with a floating-point value (Float not allowed as OPTION BASE value)                                                    |
| Error    | `OPTION BASE` value is not 0 or 1 (OPTION BASE value must be 0 or 1)                                                                  |
| Error    | `OPEN` or `CLOSE` without `#` before the file number (Incorrect statement)                                                            |
| Error    | `OPEN` or `CLOSE` with file number 0 — reserved for screen (File number 0 is reserved for screen)                                     |
| Error    | `OPEN` or `CLOSE` with literal file number outside 1–255 (File number must be between 1 and 255)                                      |
| Error    | `OPEN` or `CLOSE` with a string expression as file number (Numeric expression expected)                                               |
| Error    | `OPEN` with a numeric expression as file name (String expression expected)                                                            |
| Error    | `OPEN` without `:` separator or file name (Incorrect statement)                                                                       |
| Error    | `OPEN` with duplicate option in the same category, e.g. two organization keywords (Duplicate … option)                                |
| Error    | `OPEN` combining `RELATIVE` with `VARIABLE` (RELATIVE files require fixed-length records)                                             |
| Error    | `OPEN` with a string expression as the optional record count for `SEQUENTIAL`/`RELATIVE` (Numeric expression expected)                |
| Error    | `CLOSE` with `:` but no `DELETE`, or `DELETE` without `:` (Incorrect statement)                                                       |

### Code actions

**Format TI-Basic Code** (Ctrl+Alt+L, editor context menu, or `Code` menu)

- Converts all keywords outside string literals to uppercase
- Removes extraneous whitespace outside string literals
- Normalizes exactly one space between the line number and the first keyword
- Can be applied to the whole file or to the current selection
- Ctrl+Alt+L (standard IDEA Reformat Code) is mapped to this action for TI-Basic files via `TiBasicReformatCodeAction`

**Resequence Line Numbers** (editor context menu or `Code` menu)

- Renumbers all lines with a configurable start number and step
- A dialog lets you choose both values before applying
- Also available as a quick-fix on duplicate-line-number and out-of-order warnings

### Editor assistance

- **Keyword, variable, and line-number completion** — on-demand autocomplete (Ctrl+Space) for all TI-Basic keywords,
  all variables defined in the current file, and the next automatically generated line number at the beginning of the
  current eligible unnumbered last line; completion is case-insensitive where applicable, and if exactly one matching
  suggestion remains, it is inserted immediately without opening the popup; keywords and variables appear in separate
  groups, a blank eligible last line or a matching typed line-number prefix shows the generated line number as the
  first suggestion, array variables are shown as `NAME()` and inserted with the cursor placed between the parentheses,
  the generated line number inserts with the required trailing space, and `NEXT` completion also offers all still-open
  FOR loop variables as distinct `NEXT <var>` suggestions without duplicates; after `RESTORE `, completion offers all
  line numbers that contain `DATA` statements
- **CALL subprogram completion** — when the cursor is at the subprogram name position (directly after `CALL` or on an
  existing subprogram name token), autocomplete (Ctrl+Space) lists all 10 built-in subprogram names in a dedicated
  group; subprograms with arguments insert `()` and place the cursor between them, while `CLEAR` remains without
  parentheses;
  inside CALL argument lists, the general completion (variables, functions, keywords) is offered instead
- **Built-in function completion** — autocomplete (Ctrl+Space) suggests all built-in function names in a dedicated
  group; if exactly one matching suggestion remains, it is inserted immediately without opening the popup; functions
  that require parentheses insert `()` and place the cursor between them, while `RND` remains without parentheses
- **TAB completion** — the `TAB` screen-positioning function is completed with `()` as well, with the cursor placed
  between the parentheses
- **Character-code quick documentation** — pressing `Ctrl+Q` on the character-code argument of `CALL CHAR`, `CALL HCHAR`,
  `CALL VCHAR`, or on the argument of `CHR$` shows the resolved code, its ASCII character (if any), its TI-Basic
  character group (`32..159` in groups of 8), and any matching `CALL CHAR` overrides in the current file;
  simple statically resolvable numeric expressions such as `32+I` are also supported
- **CALL COLOR quick documentation** — pressing `Ctrl+Q` on an argument of `CALL COLOR(set,fg,bg)` first shows the
  resolved constant value (or a hint when the value is not statically determinable); on `set` it also shows the
  selected character-code range and all ASCII characters within that range, while on `fg` and `bg` it shows the
  resolved TI color name
- **Hex-pattern quick documentation** — pressing `Ctrl+Q` on the pattern argument of `CALL CHAR`, on a hexadecimal
  item in a `DATA` line, or on a string literal elsewhere in the program shows the normalized 16-digit pattern
  together with the same 8x8 black-and-white pictogram used for `CALL CHAR`; general string and `DATA` detection uses
  the stricter digit-only rule (`0...` up to 16 digits, or any 9 to 16 digits) to avoid matching short decimal text
- **CALL CHAR gutter preview** — for lines containing `CALL CHAR(code,pattern$)` with a valid hex pattern of up to
  16 characters, a 16×16 px black-and-white pictogram appears in the gutter showing the defined 8×8 character
  (1-bit → black, 0-bit → white, with a dark-gray border); the pattern may be a string literal or a constant string
  variable, and shorter patterns are padded with trailing zeroes
- **CALL COLOR gutter preview** — for lines containing `CALL COLOR(set,fg,bg)` with resolvable numeric color
  arguments, a split color square appears in the gutter (left half = foreground, right half = background TI color;
  literals, constant numeric variables, and simple statically resolvable numeric expressions are resolved;
  transparent checkerboard shown for non-constant
  arguments)
- **CALL SCREEN gutter preview** — for every `CALL SCREEN(colorCode)` line a solid 16×16 color square appears in the
  gutter showing the chosen background color (`colorCode` may be a literal, a constant numeric variable, or a simple
  statically resolvable numeric expression; a
  transparent checkerboard is shown when the color cannot be resolved)
- **CALL SOUND gutter playback** — for every resolvable `CALL SOUND(dur,pitch1,vol1[,pitch2,vol2...])`, a play icon
  appears in the gutter; clicking it plays a square-wave approximation of up to three tone channels plus the optional
  SN76489-style noise channel via the JVM audio stack on Linux, macOS, and Windows. Positive durations always play to
  completion and additional clicks are queued FIFO; negative durations are interruptible and are replaced immediately by
  a newly clicked sound while already queued follow-up sounds remain queued. Negative TI-Basic noise selectors `-1..-4`
  (periodic) and `-5..-8` (white) are accepted wherever a CALL SOUND frequency argument resolves statically, with `-4`
  and `-8` following tone 3
- **Line-reference gutter indicator** — if other program lines refer to a line number (`GOTO`, `GOSUB`,
  `ON ... GOTO/GOSUB`, `IF ... THEN/ELSE`, `RESTORE`, `BREAK`, `UNBREAK`, `TRACE`, `UNTRACE`), a gutter icon appears
  next to that target line; the tooltip summarizes the referring line numbers, and clicking the icon opens a
  navigation list of the referring lines
- **Ctrl+B / Ctrl+Click for line-number references** — the referenced numeric literals in `GOTO`, `GOSUB`,
  `ON ... GOTO/GOSUB`, `IF ... THEN/ELSE`, `RESTORE`, `BREAK`, `UNBREAK`, `TRACE`, and `UNTRACE` navigate directly to
  the matching target line in the same file; unresolved or out-of-range targets stay non-navigable
- **TI-99/4A display column guides** — thin overlay guidelines are drawn across the whole visible file at every 28th
  character position needed by the longest line in the file, showing exactly where the TI-99/4A's 28-column text
  display would wrap to the next screen row without shifting the surrounding code layout; the guides can be enabled or
  disabled in *Settings → Editor → TI-Basic Display Column Guides* and can start appearing a configurable number of
  characters before the next wrap boundary (default: 2)
- **Reformat Code** (Ctrl+Alt+L) — the standard IDEA "Reformat Code" action is mapped to **Format TI-BASIC** for
  TI-Basic files; for all other file types the default behavior is preserved
- **TI Basic Variables tool window** — a dockable bottom panel listing all variables in the active TI-Basic file
  in a sortable table with columns Name, Type, Writes, Reads, and Range, plus the additional array-only columns
  Dimensions, Base, and DIM whenever the file defines at least one array; the Dimensions and Base columns show the
  effective array size and `OPTION BASE` value directly on each numeric or string array row, including explicit `DIM`
  declarations and implicit arrays with the TI-Basic default dimension size `10`; the **DIM** column shows the
  clickable `DIM` statement line number on that same array row when the array was declared explicitly; the Writes and Reads
  columns show clickable line numbers that navigate to the selected occurrence in the editor; all table columns wrap
  automatically as the tool window is resized; the table refreshes
  automatically on every document change; the **Range**
  column shows the effective finite value range for scalar numeric and string variables — singleton ranges behave like
  the previous constant display (`0`, `""`, `42`, `"HELLO"`), while multi-value ranges are shown as comma-separated
  literal lists (e.g. `"307C6EF8FE7C7C30", "0C3E761F7F3E3E0C"` for `G$` when it can receive either value through simple
  variable aliases)
- **TI Basic Character Definitions tool window** — a dockable bottom panel listing all statically resolvable
  `CALL CHAR` definitions in the active TI-Basic file in a sortable table with columns Code, ASCII, Pattern, Icon, and Line;
  entries stay grouped by their actual character code even when multiple codes share the same pattern and the same derived
  foreground/background colors;
  besides direct literals, simple constant variables, and simple statically resolvable numeric code expressions,
  the table also includes definitions that can be traced
  statically through `READ`/`DATA` statements (including `RESTORE`) and through simple statically resolvable
  `FOR`/`NEXT` loops that repeatedly execute such `READ` + `CALL CHAR` sequences, including simple `IF ... THEN`
  line jumps inside those loops when the branch condition is statically decidable;
  the Icon column renders the defined character as a base black/white pictogram plus all distinct colorized variants
  that can be derived statically from matching `CALL COLOR(set,fg,bg)` statements for the character's set;
  clicking the Line cell navigates to that program line;
  the table refreshes automatically on every document change
- **Find Usages** (Alt+F7) — finds usages of TI-Basic variables, statement keywords, `CALL` subprogram names,
  built-in functions, and user-defined functions; for variables, the Usages panel distinguishes read accesses (blue)
  from write accesses (orange/red)
- **Shift+Enter** — inserts a new line and automatically prepends the next logical line
  number; when **auto-close on Shift+Enter** is enabled (default: on), also appends `)` characters
  for each unclosed `(` on the current line before creating the new line
- **Ctrl+D / Paste** — duplicating the current line or pasting content at the end of the
  file automatically renumbers the inserted TI-Basic lines with the next multiple of 10
  after the highest line number in the file; line number references within statements
  (GOTO, IF-THEN, ON GOTO, RESTORE, …) are shifted by the same delta as their containing
  line; in the middle of the file the standard IntelliJ duplicate/paste behavior is preserved
- **Enter** — when **auto-close on Enter** is enabled (default: off) and the cursor is at
  the end of a line, appends `)` characters for each unclosed `(` before creating the new line
- **Bracket matching** — when the cursor is adjacent to a `(` or `)`, the matching bracket
  is highlighted automatically
- **Parenthesis Auto-Close settings** — configurable via *Settings › Editor › TI-Basic
  Parenthesis Auto-Close*; independently enable/disable auto-close for Shift+Enter and Enter

## Project structure

```
src/
├── main/
│   ├── kotlin/com/github/mmrsic/idea/plugins/tibasic/
│   │   ├── action/          File actions, formatter, and resequencing
│   │   ├── editor/          Completion, Shift+Enter, line-number navigation, gutter previews/reference markers, and display column hints
│   │   ├── ext/             Kotlin wrappers around IntelliJ framework APIs
│   │   ├── findusages/      Find Usages provider, handler, target evaluator, and read/write detection
│   │   ├── highlight/       Annotator and syntax highlighting
│   │   ├── lang/            Language object, file type, keywords, built-in functions, CALL registries, colors
│   │   ├── lexer/           Line-based lexer and token types
│   │   ├── parser/          PSI parser, node types, and parser definition
│   │   ├── psi/             PSI elements and PSI-related extensions
│   │   ├── toolwindow/      Variables and character-definition tool windows, collectors, table models, and navigation
│   │   └── util/            Shared PSI/document helpers
│   └── resources/META-INF/plugin.xml           Plugin descriptor
└── test/
    └── kotlin/com/github/mmrsic/idea/plugins/tibasic/
        ├── TiBasicTestBase.kt                  Shared test base class
        ├── action/                             Formatter and resequencing tests
        ├── editor/                             Completion, Shift+Enter, line-number navigation, gutter preview/reference, and display column hint tests
        ├── findusages/                         Variable, statement, subprogram, and function usage tests
        ├── highlight/                          General and statement-specific annotator tests
        ├── lang/                               Icon and language-related tests
        ├── parser/                             General and statement-specific parser tests
        └── toolwindow/                         Variables and character-definition tool window / collector tests
```

## Prerequisites

| Tool          | Minimum version          |
|---------------|--------------------------|
| JDK           | 21                       |
| Gradle        | 8.x (via wrapper)        |
| IntelliJ IDEA | 2025.2 (target platform) |

## Building

```bash
# Compile and run all tests
./gradlew build

# Run tests only
./gradlew test

# Launch a sandbox IDE instance with the plugin installed
./gradlew runIde

# Verify plugin compatibility against recommended IDE versions
./gradlew verifyPlugin
```

Test reports are written to `build/reports/tests/test/index.html`.

## Developer documentation

| Document                                               | Contents                                                                                |
|--------------------------------------------------------|-----------------------------------------------------------------------------------------|
| [`docs/architecture.md`](docs/architecture.md)         | Package map, data-flow diagram, annotator checks, threading model, key design decisions |
| [`docs/grammar.md`](docs/grammar.md)                   | Full EBNF grammar, token reference, valid/invalid example lines                         |
| [`docs/extension-points.md`](docs/extension-points.md) | All registered IntelliJ extension points and how to add new ones                        |
| [`docs/testing.md`](docs/testing.md)                   | Test setup, base class, writing parser/annotator/action tests, running the sandbox      |

## Contributing

1. Fork the repository and create a feature branch.
2. Follow the coding conventions in [`.github/copilot-instructions.md`](.github/copilot-instructions.md).
3. Add or update tests for every non-trivial change.
4. Keep the `docs/` files and this README in sync with your changes (see conventions).
5. Ensure `./gradlew build` and `./gradlew verifyPlugin` pass before opening a pull request.
6. Keep commits small and focused; prefix messages with `feat:`, `fix:`, `chore:`, etc.

## License

This project is licensed under the **GNU General Public License v3.0**.
See the [LICENSE](LICENSE) file for details.
