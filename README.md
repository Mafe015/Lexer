# Lexer en Kotlin

Analizador léxico (Lexer) implementado en Kotlin, basado en el ejemplo de Python.

## Descripción
Este lexer convierte código fuente en tokens. Es la primera fase de un intérprete.
El lenguaje soportado tiene sintaxis similar a Kotlin.

## Tokens soportados
| Token | Símbolo |
|---|---|
| VAL | val |
| FUN | fun |
| IF / ELSE / ELIF | if / else / elif |
| WHILE / FOR | while / for |
| RETURN | return |
| PRINT | print |
| PLUS / MINUS / MULTIPLY / DIVISION | + - * / |
| MOD / POW | % ** |
| EQ / DIF | == != |
| LT / LTE / GT / GTE | < <= > >= |
| AND / OR / NEGATION | && \|\| ! |
| ASSIGN | = |
| TRUE / FALSE | true / false |
| INTEGER / FLOAT / STRING | 42 / 3.14 / "hola" |
| QUESTION | ? |

## Estructura del proyecto
LEXER/
├── src/
│   ├── TokenType.kt
│   ├── Tokens.kt
│   ├── Lexer.kt
│   └── Main.kt
└── test/
└── test_lexer.kt

## Cómo compilar y ejecutar

### REPL
``` En la powershell
kotlinc src/TokenType.kt src/Tokens.kt src/Lexer.kt src/Main.kt -include-runtime -d lexer.jar
java -jar lexer.jar
```

### Tests
```En la powershell
kotlinc src/TokenType.kt src/Tokens.kt src/Lexer.kt test/test_lexer.kt -include-runtime -d test.jar
java -jar test.jar
```

## Ejemplos de uso
val x = 10;
Type : VAL          Literal: 'val'
Type : IDENTIFIER   Literal: 'x'
Type : ASSIGN       Literal: '='
Type : INTEGER      Literal: '10'
Type : SEMICOLON    Literal: ';'
fun suma(a, b) { return a + b; }
Type : FUN          Literal: 'fun'
Type : IDENTIFIER   Literal: 'suma'
...
val msg = "hola
ERROR: String sin cerrar, le falta '"'
@
ERROR: Caracter no reconocido '@'

## Manejo de errores
- Carácter no reconocido → muestra ERROR con el carácter
- String sin cerrar → muestra ERROR indicando la comilla faltante

## Tecnologías
- Kotlin
- JDK 25