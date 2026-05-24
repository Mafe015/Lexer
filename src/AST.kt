abstract class Node{
    abstract fun tokenLiteral(): String
    abstract override fun toString(): String
}

abstract class Statement: Node()
abstract class Expression: Node()

class Program: Node(){
    val statements = mutableListOf<Statement>()

    override fun tokenLiteral(): String {
        // ✅ fix 1: firstOrNull con O mayúscula
        return statements.firstOrNull()?.tokenLiteral() ?: ""
    }

    override fun toString(): String =
        statements.joinToString("\n")
}

class ValStatement(
    val token: Token,
    val name: Identifier,
    val value: Expression?
) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String {
        val valueStr = value?.toString() ?: ""
        return "val $name = $valueStr;"
    }
}

class ReturnStatement(
    val token: Token,
    val returnValue: Expression?
) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String {
        val valueStr = returnValue?.toString() ?: ""
        return "return $valueStr;"
    }
}

class ExpressionStatement(
    val token: Token,
    val expression: Expression? = null
) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = expression?.toString() ?: ""
}

class BlockStatement(
    val token: Token
) : Statement() {
    val statements = mutableListOf<Statement>()
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String {
        val stmts = statements.joinToString("\n") { "  $it" }
        return "{\n$stmts\n}"
    }
}

class WhileStatement(
    val token: Token,
    val condition: Expression,
    val body: BlockStatement
) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "while ($condition) $body"
}

// ✅ fix 2: update es Statement? no Expression? para que el parser pueda pasarle parseStatement()
class ForStatement(
    val token: Token,
    val init: Statement?,
    val condition: Expression?,
    val update: Statement?,
    val body: BlockStatement
) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "for (${init ?: ""}; ${condition ?: ""}; ${update ?: ""}) $body"
}

class BreakStatement(val token: Token) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "break;"
}

class ContinueStatement(val token: Token) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "continue;"
}

class PrintStatement(
    val token: Token,
    val value: Expression
) : Statement() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "print($value);"
}

// EXPRESIONES

class Identifier(
    val token: Token,
    val value: String
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = value
}

class IntegerLiteral(
    val token: Token,
    val value: Int
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = value.toString()
}

class FloatLiteral(
    val token: Token,
    val value: Double
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = value.toString()
}

class StringLiteral(
    val token: Token,
    val value: String
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "\"$value\""
}

class BooleanLiteral(
    val token: Token,
    val value: Boolean
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = if (value) "true" else "false"
}

// ✅ fix 3: right es Expression? con ? para aceptar null
class PrefixExpression(
    val token: Token,
    val operator: String,
    val right: Expression?
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "($operator$right)"
}

// ✅ fix 4: right es Expression? con ? para aceptar null
class InfixExpression(
    val token: Token,
    val left: Expression,
    val operator: String,
    val right: Expression?
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "($left $operator $right)"
}

class IfExpression(
    val token: Token,
    val condition: Expression,
    val consequence: BlockStatement,
    val alternative: MutableList<Pair<Expression, BlockStatement>> = mutableListOf(),
    val elseBlock: BlockStatement? = null
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String {
        var result = "if ($condition) $consequence"
        for ((cond, block) in alternative) {
            result += " elif ($cond) $block"
        }
        elseBlock?.let { result += " else $it" }
        return result
    }
}

// ✅ fix 5: body no puede tener default value si va después de parámetro con default
// solución: mover name al final y body antes de name
class FunctionLiteral(
    val token: Token,
    val parameters: MutableList<Identifier>,
    val body: BlockStatement,
    var name: String = ""
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String {
        val params = parameters.joinToString(", ")
        val nameStr = if (name.isNotEmpty()) " $name" else ""
        return "fun$nameStr($params) $body"
    }
}

class CallExpression(
    val token: Token,
    val function: Expression,
    val arguments: MutableList<Expression> = mutableListOf()
) : Expression() {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String {
        val args = arguments.joinToString(", ")
        return "$function($args)"
    }
}
