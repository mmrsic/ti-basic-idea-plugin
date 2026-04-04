# Grammar specification

This document describes the grammar of the TI-Basic dialect supported by this plugin.
The dialect is based on the BASIC interpreter of the **Texas Instruments TI-99/4A** home computer.

## Source file structure

A TI-Basic source file is a sequence of lines separated by newline characters (`\n`, or `\r\n`).
Every non-empty line must begin with a **line number**.

## Line classification

The lexer classifies each line into one of five kinds before tokenizing it:

| Kind                     | Pattern                                                          | PSI node                                           |
|--------------------------|------------------------------------------------------------------|----------------------------------------------------|
| `VALID_STATEMENT`        | `[ws] lineNumber [ws] keyword [ws] argument?`                    | `TiBasicLine` containing a statement node          |
| `LINE_NUMBER_ONLY`       | `[ws] lineNumber [ws]`                                           | `TiBasicLine` with no child statement              |
| `LET_IMPLICIT_STATEMENT` | `[ws] lineNumber [ws] variable [ws] = ...`  (no leading keyword) | `TiBasicLine` containing `TiBasicLetStatement`     |
| `UNKNOWN_STATEMENT`      | `[ws] lineNumber [ws] unrecognisedText`                          | `TiBasicLine` containing `TiBasicUnknownStatement` |
| `NO_LINE_NUMBER`         | anything else (non-blank)                                        | `TiBasicInvalidLine` (annotated as error)          |

Blank lines are silently ignored.

## EBNF grammar

```ebnf
file              = { line } ;

line              = numberedLine | invalidLine ;
numberedLine      = lineNumber [ whitespace ] [ statement ] ;
invalidLine       = noLineNumberText ;          (* annotated as error *)

lineNumber        = digit { digit } ;           (* value must be in 1..32767 *)

statement         = printStatement
                  | displayStatement
                  | inputStatement
                  | readStatement
                  | dataStatement
                  | restoreStatement
                  | letStatement
                  | defStatement
                  | dimStatement
                  | optionBaseStatement
                  | remStatement
                  | endStatement
                  | stopStatement
                  | gotoStatement
                  | gosubStatement
                  | returnStatement
                  | onGotoStatement
                  | onGosubStatement
                  | ifStatement
                  | forStatement
                  | nextStatement
                  | deleteStatement
                  | openStatement
                  | closeStatement
                  | callStatement
                  | randomizeStatement
                  | lineNumberListStatement
                  | unknownStatement ;

printStatement          = PRINT     [ whitespace ] [ printArgList ]
                        | PRINT     whitespace? HASH whitespace? numericExpression
                          [ DOT REC whitespace? numericExpression ]
                          whitespace? COLON [ printArgList ] ;
                          (* screen form: no # prefix; outputs to screen *)
                          (* file form: # is mandatory; file number is a numeric expression (1–255);
                             optional .REC specifies a record number (no whitespace between . and REC);
                             COLON is mandatory before the argument list; DISPLAY does not support file output *)
displayStatement        = DISPLAY   [ whitespace ] [ printArgList ] ;
                          (* identical syntax to screen form of printStatement; screen output only *)
printArgList            = { printItem } ;
printItem               = printSep | tabFunction | expression ;
                          (* expressions must be separated by at least one separator;
                             separators may appear any number of times before, between, and after expressions;
                             tabFunction may appear in any position where an expression is allowed *)
printSep                = COLON | SEMICOLON | COMMA ;
tabFunction             = TAB LPAREN numericExpression RPAREN ;
inputStatement          = INPUT     [ whitespace ] [ stringExpression whitespace? COLON whitespace? ] variablesList
                        | INPUT     whitespace? HASH whitespace? numericExpression
                          [ DOT REC whitespace? numericExpression ]
                          whitespace? COLON whitespace? fileInputVariablesList ;
                          (* screen form: prompt is optional; if present it must be a string expression *)
                          (* file form: # is mandatory; file number is a numeric expression (1–255);
                             .REC recordNumber is optional; variable list may end with a trailing comma *)
readStatement           = READ      [ whitespace ] variablesList ;
                          (* variable list is mandatory *)
dataStatement           = DATA      [ whitespace ] dataList ;
                          (* data list is mandatory; empty DATA is an error *)
dataList                = dataItem { COMMA dataItem } ;
dataItem                = STRING_LITERAL               (* quoted string; may contain comma, quote, leading/trailing spaces *)
                        | NUMERIC_LITERAL              (* number *)
                        | unquotedText                 (* unquoted string; leading/trailing spaces are stripped *)
                        | ε ;                          (* empty item from consecutive commas *)
                          (* whitespace around commas is ignored *)
restoreStatement        = RESTORE   [ whitespace lineNumber ]
                        | RESTORE whitespace HASH numericExpression
                          [ COMMA REC numericExpression ] ;
                          (* file variant: reposition within an open file *)
variablesList           = variableAccess { COMMA variableAccess } ;
letStatement            = [ LET whitespace ] variableAccess EQ expression ;
                          (* LET keyword is optional; annotator checks type compatibility *)
defStatement            = DEF whitespace funcName [ LPAREN paramName RPAREN ] EQ expression ;
                          (* funcName and paramName are variable names (NUMERIC_VARIABLE or STRING_VARIABLE);
                             if the body expression is a string expression, funcName must be a STRING_VARIABLE;
                             paramName is local to this DEF — it does not alias program variables of the same name;
                             DEF calls in expressions look syntactically like a VARIABLE_ACCESS with one subscript *)
dimStatement            = DIM whitespace dimEntry { COMMA dimEntry } ;
                          (* each entry declares the maximum indices of one array;
                             the same array name must appear at most once across all DIM statements;
                             a DIM statement must precede the first use of every array it declares;
                             dimensions must be plain integer literals — variables, floats, and expressions are forbidden *)
dimEntry                = ( NUMERIC_VARIABLE | STRING_VARIABLE ) LPAREN integerLiteral { COMMA integerLiteral } RPAREN ;
                          (* 1–3 dimensions; integerLiteral must be a NUMERIC_LITERAL without '.' or exponent *)
optionBaseStatement     = OPTION_BASE integerLiteral ;
                          (* integerLiteral must be exactly 0 or 1;
                             sets the minimum array index for the program;
                             variables, floats, and other integers are forbidden *)
remStatement            = REM       [ whitespace ] [ remarkText ] ;
endStatement            = END ;
                          (* halts program; by convention placed as the last line, but may appear anywhere *)
stopStatement           = STOP ;
                          (* halts program; by convention used within the program body, not at the end *)
gotoStatement           = ( GOTO | GO whitespace TO ) [ whitespace ] lineNumber ;
                          (* transfers control to the given line; lineNumber must be in 1..32767 *)
gosubStatement          = ( GOSUB | GO whitespace SUB ) [ whitespace ] lineNumber ;
                          (* calls subroutine at given line; lineNumber must be in 1..32767;
                             returns to the statement after the call when RETURN is executed *)
returnStatement         = RETURN ;
                          (* returns to the statement after the most recent GOSUB *)
onGotoStatement         = ON whitespace numericExpression whitespace ( GOTO | GO whitespace TO ) whitespace lineNumberList ;
                          (* computed branch; expression must be numeric; at least one lineNumber required *)
onGosubStatement        = ON whitespace numericExpression whitespace ( GOSUB | GO whitespace SUB ) whitespace lineNumberList ;
                          (* computed subroutine call; expression must be numeric; at least one lineNumber required *)
ifStatement             = IF whitespace numericExpression whitespace THEN whitespace lineNumber
                          [ whitespace ELSE whitespace lineNumber ] ;
                          (* conditional branch; expression must be numeric; non-zero = true → THEN target, zero → ELSE target *)
forStatement            = FOR whitespace numericVariable whitespace EQ whitespace numericExpression
                          whitespace TO whitespace numericExpression
                          [ whitespace STEP whitespace numericExpression ] ;
                          (* numericVariable must be a scalar numeric variable; expressions must be numeric *)
nextStatement           = NEXT whitespace numericVariable ;
                          (* control variable must match a preceding FOR variable *)
deleteStatement         = DELETE    [ whitespace ] [ stringExpression ] ;
openStatement           = OPEN [ whitespace ] HASH fileNumber COLON fileNameExpr
                          { COMMA openOption } ;
                          (* HASH is mandatory; fileNumber is a numeric expression rounded to int, must be 1..255;
                             file number 0 is reserved for the screen; fileNameExpr is a string expression;
                             options may appear in any order *)
openOption              = ( SEQUENTIAL | RELATIVE ) [ whitespace numericExpression ]
                        | DISPLAY | INTERNAL
                        | INPUT | OUTPUT | APPEND | UPDATE
                        | FIXED | VARIABLE
                        | PERMANENT ;
                          (* SEQUENTIAL / RELATIVE: optional numeric expression is the initial record count;
                             RELATIVE requires fixed-length records — combining RELATIVE with VARIABLE is an error;
                             duplicate options within the same category are errors *)
closeStatement          = CLOSE [ whitespace ] HASH fileNumber [ COLON DELETE ] ;
                          (* HASH is mandatory; same fileNumber rules as openStatement;
                             :DELETE is optional and causes the file to be deleted on close *)
fileNumber              = numericExpression ;   (* literal values are validated to 1..255 *)
fileNameExpr            = stringExpression ;
lineNumberListStatement = listKeyword whitespace lineNumberList ;
callStatement           = CALL [ whitespace ] CALL_SUBPROGRAM_NAME
                          [ LPAREN [ callArgList ] RPAREN ]
                          { token } ;   (* trailing tokens annotated as error for CLEAR *)
callArgList             = expression { COMMA expression } ;
randomizeStatement      = RANDOMIZE [ whitespace numericExpression ] ;
                          (* without argument: system clock is used as seed;
                             with a numeric expression: INT of the result is used as seed *)
unknownStatement        = unknownText ;         (* annotated as error *)

listKeyword       = BREAK | UNBREAK | TRACE | UNTRACE ;

lineNumberList    = numericLiteral { COMMA numericLiteral } ;
                  (* values must be in 1..32767; annotator warns on undefined references *)

expression        = numericComparison ;

(* String expressions *)
stringExpression  = stringOperand { CONCAT_OP stringOperand } ;
stringOperand     = STRING_LITERAL | stringVariableAccess | stringFunctionCall ;

(* Numeric expressions — operator precedence, low to high *)
numericComparison = comparable { compOp comparable } ;   (* left-to-right *)
comparable        = stringExpression | numericAddSub ;   (* strings allowed in comparisons *)
compOp            = EQ | LT | GT | NEQ | LE | GE ;
numericAddSub     = numericMulDiv { ( PLUS | MINUS ) numericMulDiv } ;
numericMulDiv     = numericPow    { ( MUL   | DIV   ) numericPow    } ;
numericPow        = numericUnary  { POW numericUnary } ;   (* right-associative *)
numericUnary      = ( PLUS | MINUS ) numericUnary | numericPrimary ;
numericPrimary    = NUMERIC_LITERAL
                  | variableAccess
                  | STRING_LITERAL          (* type mismatch — parsed for error reporting *)
                  | stringVariableAccess    (* type mismatch — parsed for error reporting *)
                  | LPAREN numericComparison RPAREN
                  | numericFunctionCall ;

numericFunctionCall = NUMERIC_FUNCTION_KEYWORD LPAREN expressionList RPAREN ;
                    (* functions with argCount=0, e.g. RND, omit the parentheses entirely *)
stringFunctionCall  = STRING_FUNCTION_KEYWORD LPAREN expressionList RPAREN ;
                    (* string-returning functions; valid in any stringOperand position *)
expressionList      = expression { COMMA expression } ;

variableAccess        = ( NUMERIC_VARIABLE | STRING_VARIABLE ) [ subscripts ] ;
stringVariableAccess  = STRING_VARIABLE [ subscripts ] ;
subscripts            = LPAREN subscriptList RPAREN ;
subscriptList         = subscriptExpr { COMMA subscriptExpr } ;   (* 1–3 elements *)
subscriptExpr         = numericComparison ;
```

> **Note on semi-permissive parsing:** String literals and string variables are accepted inside numeric expressions by
> the parser. This allows the annotator to produce precise error messages (e.g., "String-Number-Mismatch") rather than
> a generic parse failure.

## Tokens

### Built-in expression functions

Built-in functions appear inside expressions and return a value. They are distinct from `CALL` subprograms
(which are stand-alone statements). Function names are recognised as `NUMERIC_FUNCTION_KEYWORD` or
`STRING_FUNCTION_KEYWORD` tokens in the lexer.

#### Numeric and mixed-return functions (`NUMERIC_FUNCTION_KEYWORD`)

All take one or more parenthesised arguments unless noted; all return a numeric value.

| Function       | Arguments             | Description                                                            |
|----------------|-----------------------|------------------------------------------------------------------------|
| `ABS(x)`       | 1 numeric             | Absolute value                                                         |
| `ATN(x)`       | 1 numeric             | Arctangent (result in radians)                                         |
| `COS(x)`       | 1 numeric             | Cosine (argument in radians)                                           |
| `EOF(n)`       | 1 numeric             | End-of-file status: 0 = not at end, 1 = logical end, −1 = physical end |
| `EXP(x)`       | 1 numeric             | *e* raised to the power *x*                                            |
| `INT(x)`       | 1 numeric             | Greatest integer ≤ *x*                                                 |
| `LOG(x)`       | 1 numeric             | Natural logarithm                                                      |
| `RND`          | none (no parentheses) | Random number in [0, 1)                                                |
| `SGN(x)`       | 1 numeric             | Sign of *x* (−1, 0, or 1)                                              |
| `SIN(x)`       | 1 numeric             | Sine (argument in radians)                                             |
| `SQR(x)`       | 1 numeric             | Square root                                                            |
| `TAN(x)`       | 1 numeric             | Tangent (argument in radians)                                          |
| `ASC(s$)`      | 1 string              | ASCII code of first character                                          |
| `LEN(s$)`      | 1 string              | Length of string                                                       |
| `POS(s$,t$,n)` | 2 strings, 1 numeric  | Starting position of *t$* in *s$* ≥ *n*                                |
| `VAL(s$)`      | 1 string              | Numeric value of string                                                |

#### String-returning functions (`STRING_FUNCTION_KEYWORD`)

These functions return a string value and are valid in string expression positions.

| Function             | Arguments            | Description                   |
|----------------------|----------------------|-------------------------------|
| `CHR$(n)`            | 1 numeric            | Character with ASCII code *n* |
| `SEG$(s$,start,len)` | 1 string, 2 numerics | Substring of *s$*             |
| `STR$(x)`            | 1 numeric            | String representation of *x*  |

> **Implementation note:** All functions listed above are fully implemented. Adding a further function
> requires only one new entry in `TiBasicBuiltInFunctions.signatures`.

### Keywords

Recognised as statement-starting keywords (case-insensitive):

| Token                                  | Statement kind                                                                     |
|----------------------------------------|------------------------------------------------------------------------------------|
| `LET`                                  | Variable assignment                                                                |
| `DEF`                                  | User-defined function                                                              |
| `DIM`                                  | Array dimension declaration                                                        |
| `OPTION BASE`                          | Minimum array index (0 or 1)                                                       |
| `PRINT`                                | Print to screen, printer, or file                                                  |
| `DISPLAY`                              | Print to screen only                                                               |
| `TAB`                                  | Column-positioning function (PRINT/DISPLAY)                                        |
| `INPUT`                                | Keyboard input (with optional string prompt); or file I/O: `INPUT #n[.REC r]:vars` |
| `READ`                                 | Read values from DATA into variables                                               |
| `DATA`                                 | Supply data values for READ statements                                             |
| `RESTORE`                              | Reset DATA pointer (optionally to given line)                                      |
| `REM`                                  | Remark/comment                                                                     |
| `END`                                  | Halt program (end of program by convention)                                        |
| `STOP`                                 | Halt program (mid-program by convention)                                           |
| `GOTO` / `GO TO`                       | Unconditional branch to a line number                                              |
| `ON … GOTO` / `ON … GO TO`             | Computed branch to one of several lines                                            |
| `IF … THEN` / `IF … THEN … ELSE`       | Conditional branch to a line number                                                |
| `FOR … TO` / `FOR … TO … STEP`         | Counted loop — start, limit, optional step                                         |
| `NEXT`                                 | End of counted loop body                                                           |
| `DELETE`                               | Delete string                                                                      |
| `OPEN`                                 | Open a file for I/O                                                                |
| `CLOSE`                                | Close an open file                                                                 |
| `BREAK`, `UNBREAK`, `TRACE`, `UNTRACE` | Line-number-list statements                                                        |

### Commands (not valid as statements)

These identifiers are recognized by the annotator and produce a specific error:

`BYE`, `CON`, `CONTINUE`, `EDIT`, `LIST`, `NEW`, `NUM`, `NUMBER`, `OLD`, `RES`, `RESEQUENCE`, `RUN`, `SAVE`

### Operators

| Token       | Symbol |
|-------------|--------|
| `PLUS_OP`   | `+`    |
| `MINUS_OP`  | `-`    |
| `MUL_OP`    | `*`    |
| `DIV_OP`    | `/`    |
| `POW_OP`    | `^`    |
| `CONCAT_OP` | `&`    |
| `EQ_OP`     | `=`    |
| `NEQ_OP`    | `<>`   |
| `LT_OP`     | `<`    |
| `GT_OP`     | `>`    |
| `LE_OP`     | `<=`   |
| `GE_OP`     | `>=`   |
| `COLON`     | `:`    |
| `HASH`      | `#`    |
| `DOT`       | `.`    |
| `SEMICOLON` | `;`    |
| `COMMA`     | `,`    |

### Variables

| Token                   | Pattern                               | Notes                        |
|-------------------------|---------------------------------------|------------------------------|
| `NUMERIC_VARIABLE`      | `[A-Za-z@\[\]\\_][A-Za-z0-9@_]{0,14}` | Max 14 chars excluding first |
| `STRING_VARIABLE`       | same as numeric + trailing `$`        | Max 13 chars before `$`      |
| `INVALID_VARIABLE_NAME` | identifier that matches neither rule  | Annotated as error           |

### Literals

| Token             | Example                                           |
|-------------------|---------------------------------------------------|
| `NUMERIC_LITERAL` | `42`, `3.14`, `1.5E-3`                            |
| `STRING_LITERAL`  | `"HELLO"`, `"say ""hi"""` (doubled quotes inside) |

## Valid and invalid example lines

```tibasic
100 PRINT "HELLO"              ✓ valid
110 PRINT A + B * 2            ✓ valid — numeric expression
120 PRINT A$ & " WORLD"        ✓ valid — string concatenation
125 PRINT "X=";X," Y=";Y       ✓ valid — mixed separators: semicolon and comma
126 PRINT TAB(5);"TEXT"         ✓ valid — position cursor at column 5, then print
127 PRINT TAB(N);"TEXT"         ✓ valid — TAB argument may be any numeric expression
128 PRINT TAB(5)                ✓ valid — TAB without following output
129 PRINT TAB(5);"A";TAB(10);"B" ✓ valid — multiple TAB calls in one PRINT
130 DISPLAY "HELLO"             ✓ valid — screen output identical to PRINT syntax
131 DISPLAY TAB(5);"TEXT"       ✓ valid — TAB is valid in DISPLAY too
132 DISPLAY                     ✓ valid — DISPLAY with no argument
127 PRINT :;,                  ✓ valid — separators without expressions
128 PRINT ,"RIGHT ZONE"        ✓ valid — leading comma jumps to right screen zone
129 PRINT "CONT";              ✓ valid — trailing semicolon, cursor stays on same line
130 PRINT A(1,2)               ✓ valid — 2-D array subscript
140 REM this is a remark       ✓ valid
150 DELETE A$                  ✓ valid
160 BREAK 100,120,140          ✓ valid — line-number list
170                            ✓ valid — line number only (no statement)
180 LET A = 5                  ✓ valid — explicit LET assignment
190 A = 5                      ✓ valid — implicit LET (no keyword)
200 A$ = "HELLO"               ✓ valid — implicit LET, string variable
210 LET A(2) = 3.14            ✓ valid — explicit LET with subscripted variable
220 END                        ✓ valid — halts program (conventional last line)
230 STOP                       ✓ valid — halts program (conventional mid-program)
240 GOTO 100                   ✓ valid — unconditional branch
250 GO TO 100                  ✓ valid — unconditional branch (two-word form)
260 ON X GOTO 100,200,300      ✓ valid — computed branch (numeric expression X)
270 ON X GO TO 100,200         ✓ valid — computed branch (two-word GO TO form)
280 IF X>0 THEN 100            ✓ valid — conditional branch
290 IF X>0 THEN 100 ELSE 200   ✓ valid — conditional branch with else target
300 FOR I = 1 TO 10             ✓valid — counted loop, step defaults to 1
310 FOR I = 1 TO 10 STEP 2     ✓ valid — counted loop, explicit step
320 NEXT I                      ✓valid — end of loop body
400 INPUT A,B$                 ✓ valid — keyboard input, two variables
410 INPUT "Enter name: ":A$    ✓ valid — keyboard input with string prompt (no space before variable)
415 INPUT #1:A                 ✓ valid — file input, file number 1, one variable
416 INPUT #X+5:A,B$            ✓ valid — file input, file number expression, two variables
417 INPUT #1.REC 5:A           ✓ valid — file input with record number
418 INPUT #1:A,B,              ✓ valid — file input with trailing comma
420 READ A,B$                  ✓ valid — read into two variables
430 DATA 42,"hello",world      ✓ valid — data list with number, quoted string, unquoted string
435 DATA 1,,3                  ✓ valid — empty item from consecutive commas (second item is empty string)
436 DATA "has,comma","has ""quote"""  ✓ valid — quotes required when item contains comma or quote
440 INPUT                       ✗ error — no variable list
450 READ                        ✗ error — no variable list
460 DATA                        ✗ error — no data list
470 RESTORE                     ✓ valid — reset DATA pointer to beginning
480 RESTORE 200                 ✓ valid — reset DATA pointer to line 200
490 RESTORE A                   ✗ error — argument must be a numeric literal
500 RESTORE 100 200             ✗ error — only one line number allowed
510 RUN                         ✗ error — command used as statement
520 PRINT A$ + 1                ✗ error — String-Number-Mismatch
525 PRINT TAB                   ✗ error — TAB requires a numeric argument in parentheses
526 PRINT TAB()                 ✗ error — TAB requires a numeric argument
527 LET X = TAB(5)              ✗ error — TAB is only valid in a PRINT or DISPLAY statement
530 LET A = "hello"             ✗ error — String-number mismatch (numeric variable, string expression)
540 LET A$ = 5                  ✗ error — String-number mismatch (string variable, numeric expression)
550 LET A = 5 EXTRA             ✗ error — Incorrect statement (trailing tokens after LET expression)
99999 PRINT "X"                 ✗ error — line number out of range (> 32767)
PRINT "no number"               ✗ error — line number expected
560 FOOBAR                      ✗ error — unknown statement
570 FOR A$ = 1 TO 10            ✗ error — string variable as FOR control variable
580 NEXT A$                     ✗ error — string variable as NEXT control variable
590 FOR I = "X" TO 10           ✗ error — string expression as initial value
600 FOR I = 1 TO 10\n610 FOR J = 1 TO 5\n620 NEXT I  ✗ warning on line 610 — FOR-NEXT-ERROR: 2 FOR statements and 1 NEXT statements
630 GOTO 100                    ✗ warning — undefined line number (100 not in file)
640 GOTO A                      ✗ error — argument must be a numeric literal (Incorrect statement)
650 LET Y = ABS(X)              ✓ valid — built-in numeric function
660 LET Y = ABS(-5)             ✓ valid — literal as function argument
670 LET Y = ABS(X,2)            ✗ error — ABS takes exactly 1 argument (INCORRECT STATEMENT)
680 LET Y = ABS("A")            ✗ error — ABS requires a numeric argument (INCORRECT STATEMENT)
690 LET Y = ABS()               ✗ error — ABS requires 1 argument (INCORRECT STATEMENT)
700 DIM A(10)                   ✓ valid — 1-D array; indices 0–10
710 DIM A(5,3)                  ✓ valid — 2-D array
720 DIM A(10),B$(5)             ✓ valid — two arrays in one DIM statement
730 OPTION BASE 0               ✓ valid — minimum index is 0 (default)
740 OPTION BASE 1               ✓ valid — minimum index is 1
750 DIM                         ✗ error — at least one array entry required
760 DIM A                       ✗ error — dimension parentheses required (Incorrect statement)
770 DIM A(X)                    ✗ error — variable not allowed as DIM dimension
780 DIM A(2.5)                  ✗ error — float not allowed as DIM dimension
790 DIM A(1+2)                  ✗ error — expression not allowed as DIM dimension
800 DIM A(10)\n810 DIM A(5)     ✗ error — duplicate DIM for array name A
820 LET A(1)=5\n830 DIM A(10)   ✗ warning — DIM for A must appear before first use at line 820
840 OPTION BASE                 ✗ error — value required (Incorrect statement)
850 OPTION BASE 2               ✗ error — OPTION BASE value must be 0 or 1
860 OPTION BASE X               ✗ error — variable not allowed as OPTION BASE value
870 OPTION BASE 1.0             ✗ error — float not allowed as OPTION BASE value
880 OPEN #1:"DSK1.FILE"                    ✓ valid — open file with literal file number and literal file name
890 OPEN #N:"DSK1.FILE"                    ✓ valid — file number as variable (no static range check)
900 OPEN #(A+1):"DSK1.FILE"               ✓ valid — file number as expression
910 CLOSE #1                               ✓ valid — close file with literal file number
911 CLOSE #1:DELETE                         ✓ valid — close and delete the file
920 CLOSE #N                               ✓ valid — close file with variable file number
921 CLOSE #N:DELETE                         ✓ valid — close and delete file by variable number
921 OPEN #1:"FILE",SEQUENTIAL              ✓ valid — default file organization
922 OPEN #1:"FILE",RELATIVE 100            ✓ valid — relative organization with initial record count
923 OPEN #1:"FILE",INTERNAL,OUTPUT,FIXED 128 ✓ valid — FIXED with explicit record length
924 OPEN #1:"FILE",UPDATE,RELATIVE,PERMANENT ✓ valid — five categories may appear in any order
930 OPEN 1:"DSK1.FILE"                     ✗ error — # is mandatory (Incorrect statement)
940 CLOSE 1                                ✗ error — # is mandatory (Incorrect statement)
941 CLOSE #1:                              ✗ error — colon without DELETE (Incorrect statement)
942 CLOSE #1 DELETE                        ✗ error — DELETE without colon (Incorrect statement)
950 OPEN #0:"DSK1.FILE"                    ✗ error — file number 0 is reserved for screen
960 OPEN #300:"DSK1.FILE"                  ✗ error — file number must be between 1 and 255
970 CLOSE #300                             ✗ error — file number must be between 1 and 255
980 OPEN #"X":"DSK1.FILE"                  ✗ error — file number must be a numeric expression
990 OPEN #1:42                             ✗ error — file name must be a string expression
995 OPEN #1                                ✗ error — colon and file name required (Incorrect statement)
996 OPEN #1:"FILE",SEQUENTIAL,RELATIVE     ✗ error — duplicate file organization option
997 OPEN #1:"FILE",RELATIVE,VARIABLE       ✗ error — RELATIVE requires fixed-length records
```

## Scope and dialect notes

- The plugin currently supports statements that are meaningful within a single source file.
- `CALL` is implemented for the 10 built-in TI-Basic subprograms: `CLEAR`, `SCREEN`, `COLOR`, `HCHAR`, `VCHAR`, `GCHAR`,
  `CHAR`, `KEY`, `JOYST`, `SOUND`. Extended Basic subprograms are out of scope.
- **Built-in expression functions** are supported for all standard TI-Basic functions:
  numeric-returning: `ABS`, `ASC`, `ATN`, `COS`, `EOF`, `EXP`, `INT`, `LEN`, `LOG`, `POS`, `RND`, `SGN`, `SIN`, `SQR`,
  `TAN`, `VAL`; string-returning: `CHR$`, `SEG$`, `STR$`. Adding a new function requires only a single registry entry
  in `TiBasicBuiltInFunctions`.
- `FOR`/`NEXT` are implemented. The annotator checks FOR-NEXT balance by count (total in file);
  it does **not** check that the control variable in `NEXT` matches the preceding `FOR` variable.
- `TAB(n)` is a column-positioning function valid **only** inside `PRINT` statements.
  The argument is a numeric expression in the range −32767..32767; float values are rounded to an integer.
  The effective column is computed as `n mod width` (1-based), where `width` = 28 for screen output and
  32 for printer/file output. If the cursor is already at or past the target column, it advances to the
  next line before moving to the column.
- String literals use `""` to embed a literal double-quote character.
- Scientific notation exponents may use `E` or `e` with an optional sign: `1.5E-3`, `2e+10`.
