# Intérprete en Kotlin

Intérprete completo implementado en Kotlin, basado en el ejemplo del profesor en Python.
Desarrollado en 3 fases: Lexer, Parser y Evaluador.

## Descripción
Este intérprete convierte código fuente en resultados ejecutables.
El lenguaje soportado tiene sintaxis similar a Kotlin.

## Fases del intérprete
| Fase | Archivos | Descripción |
|---|---|---|
| Lexer | TokenType.kt, Tokens.kt, Lexer.kt | Convierte texto en tokens |
| Parser | AST.kt, Parser.kt | Convierte tokens en árbol AST |
| Evaluador | ObjectSystem.kt, Evaluator.kt | Ejecuta el programa |

## Tokens soportados
| Token | Símbolo |
|---|---|
| VAL | val |
| FUN | fun |
| IF / ELSEIF / ELSE | if / elseif / else |
| WHILE / FOR | while / for |
| RETURN / BREAK / CONTINUE | return / break / continue |
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
```
LEXER/
├── src/
│   ├── TokenType.kt
│   ├── Tokens.kt
│   ├── Lexer.kt
│   ├── AST.kt
│   ├── Parser.kt
│   ├── ObjectSystem.kt
│   ├── Evaluator.kt
│   └── Main.kt
├── test/
│   └── test_lexer.kt
├── ejemplo.lf
├── final.lf
└── README.md
```

## Cómo compilar y ejecutar

### Intérprete completo (REPL)
```powershell
kotlinc src/TokenType.kt src/Tokens.kt src/Lexer.kt src/AST.kt src/Parser.kt src/ObjectSystem.kt src/Evaluator.kt src/Main.kt -include-runtime -d lexer.jar
java -jar lexer.jar
```

### Ejecutar un archivo .lf
```powershell
java -jar lexer.jar ejemplo.lf
java -jar lexer.jar final.lf
```

### Tests del Lexer
```powershell
kotlinc src/TokenType.kt src/Tokens.kt src/Lexer.kt test/test_lexer.kt -include-runtime -d test.jar
java -jar test.jar
```

## Ejemplos de uso

### Variables y aritmética
```
~> val x = 10 + 5;
15
~> val nombre = "Laura";
~> print("Hola, " + nombre);
"Hola, Laura"
```

### Condicionales
```
~> if (x == 15) { print("correcto"); } else { print("incorrecto"); }
"correcto"
```

### Funciones y recursión
```
~> fun factorial(n) { if (n == 0) { return 1; } return n * factorial(n - 1); }
~> print(factorial(5));
120
```

### Bucles
```
~> val i = 1;
~> while (i < 6) { print(i); val i = i + 1; }
1
2
3
4
5
```

### For
```
~> for (val j = 1; j <= 5; val j = j + 1) { print(j); }
1
2
3
4
5
```

### Operadores lógicos
```
~> print(true && false);
false
~> print(true || false);
true
~> print(!true);
false
```

## Manejo de errores
| Error | Ejemplo | Mensaje |
|---|---|---|
| Variable no encontrada | `print(z)` | `ERROR: Identificador no encontrado: z` |
| Tipos incompatibles | `10 + "hola"` | `ERROR: Tipos incompatibles: INTEGER + STRING` |
| División entre cero | `10 / 0` | `ERROR: División entre cero` |
| Operador inválido | `!10` | `ERROR: Operador ! no soportado para INTEGER` |
| String sin cerrar | `"hola` | `ERROR: String sin cerrar, le falta '"'` |
| Carácter ilegal | `@` | `ERROR: Carácter no reconocido '@'` |
| Sintaxis incorrecta | `val = 10` | `Error del parser: Se esperaba IDENTIFIER` |

## Errores conocidos y limitaciones

### 1. `while (true)` con `break` — error de Kotlin con labels

**Código que falla:**
```
while (true) {
    val n = n + 1;
    if (n == 4) { break; }
    print(n);
}
```

**Causa:** En Kotlin, `continue` no está permitido directamente dentro de un bloque `when`.
El evaluador usa `when` para detectar las señales internas de `break` y `continue`:

```kotlin
// En Evaluator.kt — evaluateWhileStatement
when (result.message) {
    "break"    -> return NULL_OBJ
    "continue" -> continue   // ERROR: continue no permitido en when
}
```

**Fix conocido:** Usar labels de Kotlin para referirse al loop externo:
```kotlin
loop@ while (true) {
    ...
    when (result.message) {
        "break"    -> return NULL_OBJ
        "continue" -> continue@loop   // correcto con label
    }
}
```

**Impacto:** El `while` con condición normal funciona correctamente.
Solo falla cuando se usa `while (true)` combinado con `break`/`continue`.

---

### 2. Caracteres Unicode especiales en archivos `.lf`

**Problema:** El lexer no reconoce caracteres Unicode fuera del rango ASCII
como `─`, `—`, `á`, `é` cuando aparecen **fuera** de strings o comentarios.
Dentro de strings funcionan bien (`"Aritmética"` es válido).
Dentro de comentarios `//` con caracteres decorativos como `// ─── TITULO ───`
el lexer produce tokens `ILEGAL` que desincroniza el parser.

**Fix conocido:** Evitar caracteres decorativos Unicode en comentarios.
Usar `// === TITULO ===` en vez de `// ─── TITULO ───`.

---

### 3. Archivos `.lf` deben usar codificación UTF-8

Al correr en Windows, usar:
```powershell
java -Dfile.encoding=UTF-8 -jar lexer.jar archivo.lf
```

## Tecnologías
- Kotlin
- JDK 25