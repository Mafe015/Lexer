// =============================================================================
// Evaluator.kt — Evaluador Tree-Walking
//
// Recorre el AST nodo por nodo y ejecuta el programa.
// Python:  def evaluate(node, env)
// Kotlin:  fun evaluate(node: Node?, env: Environment): Object?
// =============================================================================

fun evaluate(node: Node?, env: Environment): Object? {
    return when (node) {

        // ── Nodo raíz ────────────────────────────────────────────────────────
        is Program -> evaluateProgram(node, env)

        // ── Sentencias ───────────────────────────────────────────────────────
        is ExpressionStatement -> evaluate(node.expression, env)

        is BlockStatement -> evaluateBlockStatement(node, env)

        is ValStatement -> {
            // evalúa el valor del lado derecho
            val value = evaluate(node.value, env) ?: return NULL_OBJ
            if (isError(value)) return value
            // guarda la variable en el entorno
            env.set(node.name.value, value)
        }

        is ReturnStatement -> {
            val value = evaluate(node.returnValue, env) ?: return NULL_OBJ
            if (isError(value)) return value
            // envuelve el valor en ReturnValue para burbujear hacia arriba
            ReturnValue(value)
        }

        is PrintStatement -> {
            val value = evaluate(node.value, env) ?: return NULL_OBJ
            if (isError(value)) return value
            // imprime el valor y retorna null
            println(value.inspect())
            NULL_OBJ
        }

        is WhileStatement -> evaluateWhileStatement(node, env)

        is ForStatement -> evaluateForStatement(node, env)

        is BreakStatement -> ErrorObj("break")    // señal interna de break
        is ContinueStatement -> ErrorObj("continue") // señal interna de continue

        // ── Expresiones ──────────────────────────────────────────────────────
        is IntegerLiteral -> IntegerObj(node.value)
        is FloatLiteral   -> FloatObj(node.value)
        is StringLiteral -> {
            // ✅ detectar el error de string sin cerrar
             if (node.value == "ERROR string sin cerrar") {
            newError("String sin cerrar, le falta '\"'")
            } else {
            StringObj(node.value)
            }
        }
        is BooleanLiteral -> nativeBoolToBooleanObj(node.value)

        is Identifier -> evaluateIdentifier(node, env)

        is PrefixExpression -> {
            val right = evaluate(node.right, env) ?: return NULL_OBJ
            if (isError(right)) return right
            evaluatePrefixExpression(node.operator, right)
        }

        is InfixExpression -> {
            val left = evaluate(node.left, env) ?: return NULL_OBJ
            if (isError(left)) return left
            val right = evaluate(node.right, env) ?: return NULL_OBJ
            if (isError(right)) return right
            evaluateInfixExpression(node.operator, left, right)
        }

        is IfExpression -> evaluateIfExpression(node, env)

        is FunctionLiteral -> {
             val fn = FunctionObj(node.parameters, node.body, env)
            // si tiene nombre, guárdala automáticamente en el entorno
            if (node.name.isNotEmpty()) {
                env.set(node.name, fn)
            }
             fn
        }

        is CallExpression -> {
            val function = evaluate(node.function, env) ?: return NULL_OBJ
            if (isError(function)) return function
            val args = evaluateExpressions(node.arguments, env)
            if (args.size == 1 && isError(args[0])) return args[0]
            applyFunction(function, args)
        }

        else -> null
    }
}

// =============================================================================
// EVALUACIÓN DEL PROGRAMA
// =============================================================================

// Evalúa todas las sentencias del programa
// Si encuentra un ReturnValue lo desenvuelve y retorna
fun evaluateProgram(program: Program, env: Environment): Object? {
    var result: Object? = null
    for (statement in program.statements) {
        result = evaluate(statement, env)
        when (result) {
            is ReturnValue -> return result.value  // desenvuelve el return
            is ErrorObj    -> return result        // propaga el error
        }
    }
    return result
}

// Evalúa un bloque { ... }
// Similar a evaluateProgram pero NO desenvuelve ReturnValue
// (lo deja burbujear hacia arriba)
fun evaluateBlockStatement(block: BlockStatement, env: Environment): Object? {
    var result: Object? = null
    for (statement in block.statements) {
        result = evaluate(statement, env)
        if (result != null) {
            val type = result.type()
            // si es return o error, para y deja burbujear
            if (type == ObjectType.RETURN_VALUE || type == ObjectType.ERROR)
                return result
        }
    }
    return result
}

// =============================================================================
// OPERADORES PREFIX:  -x  !x
// =============================================================================

fun evaluatePrefixExpression(operator: String, right: Object): Object {
    return when (operator) {
        "!" -> evaluateBangOperator(right)
        "-" -> evaluateMinusPrefix(right)
        else -> newError("Operador prefix desconocido: $operator${right.type()}")
    }
}

// !true → false,  !false → true,  !null → true
fun evaluateBangOperator(right: Object): Object {
    return when (right) {
        TRUE_OBJ  -> FALSE_OBJ
        FALSE_OBJ -> TRUE_OBJ
        NULL_OBJ  -> TRUE_OBJ
        // ✅ agregar esto — cualquier otro tipo es error
        else -> newError("Operador ! no soportado para ${right.type()}")
    }
}

// -5 → -5,  -3.14 → -3.14
fun evaluateMinusPrefix(right: Object): Object {
    return when (right) {
        is IntegerObj -> IntegerObj(-right.value)
        is FloatObj   -> FloatObj(-right.value)
        else -> newError("Operador - no soportado para ${right.type()}")
    }
}

// =============================================================================
// OPERADORES INFIX:  5 + 3,  x == y,  "hola" + " mundo"
// =============================================================================

fun evaluateInfixExpression(operator: String, left: Object, right: Object): Object {
    return when {
        // ambos son enteros
        left is IntegerObj && right is IntegerObj ->
            evaluateIntegerInfix(operator, left, right)

        // alguno es flotante
        left is FloatObj || right is FloatObj ->
            evaluateFloatInfix(operator, left, right)

        // strings
        left is StringObj && right is StringObj ->
            evaluateStringInfix(operator, left, right)

        // booleanos y comparaciones
        operator == "==" -> nativeBoolToBooleanObj(left == right)
        operator == "!=" -> nativeBoolToBooleanObj(left != right)
        operator == "&&" -> nativeBoolToBooleanObj(isTruthy(left) && isTruthy(right))
        operator == "||" -> nativeBoolToBooleanObj(isTruthy(left) || isTruthy(right))

        // tipos incompatibles
        left.type() != right.type() ->
            newError("Tipos incompatibles: ${left.type()} $operator ${right.type()}")

        else -> newError("Operador desconocido: ${left.type()} $operator ${right.type()}")
    }
}

fun evaluateIntegerInfix(operator: String, left: IntegerObj, right: IntegerObj): Object {
    return when (operator) {
        "+"  -> IntegerObj(left.value + right.value)
        "-"  -> IntegerObj(left.value - right.value)
        "*"  -> IntegerObj(left.value * right.value)
        "/"  -> if (right.value == 0) newError("División entre cero")
                else IntegerObj(left.value / right.value)
        "%"  -> IntegerObj(left.value % right.value)
        "**" -> IntegerObj(Math.pow(left.value.toDouble(), right.value.toDouble()).toInt())
        "==" -> nativeBoolToBooleanObj(left.value == right.value)
        "!=" -> nativeBoolToBooleanObj(left.value != right.value)
        "<"  -> nativeBoolToBooleanObj(left.value < right.value)
        "<=" -> nativeBoolToBooleanObj(left.value <= right.value)
        ">"  -> nativeBoolToBooleanObj(left.value > right.value)
        ">=" -> nativeBoolToBooleanObj(left.value >= right.value)
        else -> newError("Operador desconocido para enteros: $operator")
    }
}

fun evaluateFloatInfix(operator: String, left: Object, right: Object): Object {
    val l = when (left)  { is IntegerObj -> left.value.toDouble()
                           is FloatObj   -> left.value
                           else -> return newError("Tipo inválido: ${left.type()}") }
    val r = when (right) { is IntegerObj -> right.value.toDouble()
                           is FloatObj   -> right.value
                           else -> return newError("Tipo inválido: ${right.type()}") }
    return when (operator) {
        "+"  -> FloatObj(l + r)
        "-"  -> FloatObj(l - r)
        "*"  -> FloatObj(l * r)
        "/"  -> if (r == 0.0) newError("División entre cero") else FloatObj(l / r)
        "%"  -> FloatObj(l % r)
        "==" -> nativeBoolToBooleanObj(l == r)
        "!=" -> nativeBoolToBooleanObj(l != r)
        "<"  -> nativeBoolToBooleanObj(l < r)
        "<=" -> nativeBoolToBooleanObj(l <= r)
        ">"  -> nativeBoolToBooleanObj(l > r)
        ">=" -> nativeBoolToBooleanObj(l >= r)
        else -> newError("Operador desconocido para flotantes: $operator")
    }
}

fun evaluateStringInfix(operator: String, left: StringObj, right: StringObj): Object {
    return when (operator) {
        "+"  -> StringObj(left.value + right.value)  // concatenación
        "==" -> nativeBoolToBooleanObj(left.value == right.value)
        "!=" -> nativeBoolToBooleanObj(left.value != right.value)
        else -> newError("Operador $operator no soportado para strings")
    }
}

// =============================================================================
// IF / ELIF / ELSE
// =============================================================================

fun evaluateIfExpression(node: IfExpression, env: Environment): Object? {
    val condition = evaluate(node.condition, env) ?: return NULL_OBJ
    if (isError(condition)) return condition

    return when {
        // condición principal es verdadera
        isTruthy(condition) -> evaluate(node.consequence, env)

        // revisa ramas elif
        node.alternative.isNotEmpty() -> {
            var result: Object? = NULL_OBJ
            for ((altCondition, altBlock) in node.alternative) {
                val altEval = evaluate(altCondition, env) ?: break
                if (isTruthy(altEval)) {
                    result = evaluate(altBlock, env)
                    break
                }
            }
            // si ningún elif fue verdadero y hay else
            if (result == NULL_OBJ && node.elseBlock != null)
                evaluate(node.elseBlock, env)
            else result
        }

        // else
        node.elseBlock != null -> evaluate(node.elseBlock, env)

        else -> NULL_OBJ
    }
}

// =============================================================================
// WHILE
// =============================================================================

fun evaluateWhileStatement(node: WhileStatement, env: Environment): Object? {
    var result: Object? = NULL_OBJ
    while (true) {
        val condition = evaluate(node.condition, env) ?: break
        if (isError(condition)) return condition
        if (!isTruthy(condition)) break

        result = evaluate(node.body, env)

        // manejo de break y continue
        if (result is ErrorObj) {
            when (result.message) {
                "break"    -> return NULL_OBJ
                "continue" -> continue
            }
            return result
        }
        if (result is ReturnValue) return result
    }
    return result
}

// =============================================================================
// FOR
// =============================================================================

fun evaluateForStatement(node: ForStatement, env: Environment): Object? {
    val loopEnv = Environment(outer = env)  // entorno propio del for

    // init: val i = 0
    if (node.init != null) evaluate(node.init, loopEnv)

    var result: Object? = NULL_OBJ
    while (true) {
        // condición
        if (node.condition != null) {
            val condition = evaluate(node.condition, loopEnv) ?: break
            if (isError(condition)) return condition
            if (!isTruthy(condition)) break
        }

        // cuerpo
        result = evaluate(node.body, loopEnv)
        if (result is ErrorObj) {
            when (result.message) {
                "break"    -> return NULL_OBJ
                "continue" -> { }
                else       -> return result
            }
        }
        if (result is ReturnValue) return result

        // update: val i = i + 1
        if (node.update != null) evaluate(node.update, loopEnv)
    }
    return result
}

// =============================================================================
// IDENTIFICADORES — buscar variables en el entorno
// =============================================================================

fun evaluateIdentifier(node: Identifier, env: Environment): Object {
    return env.get(node.value)
        ?: newError("Identificador no encontrado: ${node.value}")
}

// =============================================================================
// FUNCIONES — llamadas y aplicación
// =============================================================================

// Evalúa la lista de argumentos de una llamada
fun evaluateExpressions(expressions: List<Expression>, env: Environment): List<Object> {
    val result = mutableListOf<Object>()
    for (expr in expressions) {
        val evaluated = evaluate(expr, env) ?: return listOf(NULL_OBJ)
        if (isError(evaluated)) return listOf(evaluated)
        result.add(evaluated)
    }
    return result
}

// Aplica la función con sus argumentos
fun applyFunction(function: Object, args: List<Object>): Object? {
    if (function !is FunctionObj)
        return newError("No es una función: ${function.type()}")

    // crea un entorno hijo del closure
    val extendedEnv = extendFunctionEnv(function, args)
    // evalúa el cuerpo
    val evaluated = evaluate(function.body, extendedEnv)
    // desenvuelve el ReturnValue si existe
    return unwrapReturnValue(evaluated)
}

// Crea el entorno de la función con los parámetros enlazados
fun extendFunctionEnv(function: FunctionObj, args: List<Object>): Environment {
    val env = Environment(outer = function.env)
    for ((index, param) in function.parameters.withIndex()) {
        env.set(param.value, args.getOrElse(index) { NULL_OBJ })
    }
    return env
}

// Si el resultado es un ReturnValue, extrae el valor interno
fun unwrapReturnValue(obj: Object?): Object? {
    return if (obj is ReturnValue) obj.value else obj
}

// =============================================================================
// UTILIDADES
// =============================================================================

// ¿Es un valor "verdadero"?
// false y null son falsy, todo lo demás es truthy
fun isTruthy(obj: Object): Boolean {
    return when (obj) {
        NULL_OBJ  -> false
        TRUE_OBJ  -> true
        FALSE_OBJ -> false
        else      -> true
    }
}