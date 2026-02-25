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
                  | letStatement
                  | remStatement
                  | endStatement
                  | stopStatement
                  | gotoStatement
                  | deleteStatement
                  | lineNumberListStatement
                  | unknownStatement ;

printStatement          = PRINT     [ whitespace ] [ expression ] ;
letStatement            = [ LET whitespace ] variableAccess EQ expression ;
                          (* LET keyword is optional; annotator checks type compatibility *)
remStatement            = REM       [ whitespace ] [ remarkText ] ;
endStatement            = END ;
                          (* halts program; by convention placed as the last line, but may appear anywhere *)
stopStatement           = STOP ;
                          (* halts program; by convention used within the program body, not at the end *)
gotoStatement           = ( GOTO | GO whitespace TO ) lineNumber ;
                          (* transfers control to the given line; lineNumber must be in 1..32767 *)
deleteStatement         = DELETE    [ whitespace ] [ stringExpression ] ;
lineNumberListStatement = listKeyword whitespace lineNumberList ;
unknownStatement        = unknownText ;         (* annotated as error *)

listKeyword       = BREAK | UNBREAK | TRACE | UNTRACE ;

lineNumberList    = numericLiteral { COMMA numericLiteral } ;
                  (* values must be in 1..32767; annotator warns on undefined references *)

expression        = numericComparison ;

(* String expressions *)
stringExpression  = stringOperand { CONCAT_OP stringOperand } ;
stringOperand     = STRING_LITERAL | stringVariableAccess ;

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
                  | LPAREN numericComparison RPAREN ;

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

### Keywords

Recognised as statement-starting keywords (case-insensitive):

| Token                                  | Statement kind                              |
|----------------------------------------|---------------------------------------------|
| `LET`                                  | Variable assignment                         |
| `PRINT`                                | Print expression                            |
| `REM`                                  | Remark/comment                              |
| `END`                                  | Halt program (end of program by convention) |
| `STOP`                                 | Halt program (mid-program by convention)    |
| `GOTO` / `GO TO`                       | Unconditional branch to a line number       |
| `DELETE`                               | Delete string                               |
| `BREAK`, `UNBREAK`, `TRACE`, `UNTRACE` | Line-number-list statements                 |

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
220 PRINT A(1,2,3,4)           ✗ error — more than 3 subscript dimensions
230 RUN                        ✗ error — command used as statement
240 PRINT A$ + 1               ✗ error — String-Number-Mismatch
250 LET A = "hello"            ✗ error — String-number mismatch (numeric variable, string expression)
260 LET A$ = 5                 ✗ error — String-number mismatch (string variable, numeric expression)
99999 PRINT "X"                ✗ error — line number out of range (> 32767)
PRINT "no number"              ✗ error — line number expected
270 FOOBAR                     ✗ error — unknown statement
```

## Scope and dialect notes

- The plugin currently supports statements that are meaningful within a single source file.
  Multi-statement lines (`:` separator) are **not** supported.
- `FOR`/`NEXT`, `IF`/`THEN`, `GOSUB`/`RETURN`, `INPUT`, `DATA`/`READ`, `CALL` and other TI Extended Basic statements
  are **not yet** implemented; lines starting with these keywords are treated as unknown statements.
- String literals use `""` to embed a literal double-quote character.
- Scientific notation exponents may use `E` or `e` with an optional sign: `1.5E-3`, `2e+10`.
