enum class Precedence(val level: Int) {
    LOWEST(1),
    OR(2),
    AND(3),
    EQUALS(4),
    COMPARE(5),
    SUM(6),
    PRODUCT(7),
    PREFIX(8),
    POWER(9),
    CALL(10)
}

val TOKEN_PRECEDENCES = mapOf(
    TokenType.OR       to Precedence.OR,
    TokenType.AND      to Precedence.AND,
    TokenType.EQ       to Precedence.EQUALS,
    TokenType.DIF      to Precedence.EQUALS,
    TokenType.LT       to Precedence.COMPARE,
    TokenType.LTE      to Precedence.COMPARE,
    TokenType.GT       to Precedence.COMPARE,
    TokenType.GTE      to Precedence.COMPARE,
    TokenType.PLUS     to Precedence.SUM,
    TokenType.MINUS    to Precedence.SUM,
    TokenType.MULTIPLY to Precedence.PRODUCT,
    TokenType.DIVISION to Precedence.PRODUCT,
    TokenType.MOD      to Precedence.PRODUCT,
    TokenType.POW      to Precedence.POWER,
    TokenType.LPAREN   to Precedence.CALL
)

typealias PrefixParseFn = () -> Expression?
typealias InfixParseFn = (Expression) -> Expression?

class Parser(private val lexer: Lexer) {

    private var currentToken: Token = Token(TokenType.EOF, "")
    private var peekToken: Token = Token(TokenType.EOF, "")
    val errors = mutableListOf<String>()
    private val prefixParseFns = mutableMapOf<TokenType, PrefixParseFn>()
    private val infixParseFns  = mutableMapOf<TokenType, InfixParseFn>()

    init {
        advanceTokens()
        advanceTokens()

        registerPrefix(TokenType.IDENTIFIER) { parseIdentifier() }
        registerPrefix(TokenType.INTEGER)    { parseIntegerLiteral() }
        registerPrefix(TokenType.FLOAT)      { parseFloatLiteral() }
        registerPrefix(TokenType.STRING)     { parseStringLiteral() }
        registerPrefix(TokenType.TRUE)       { parseBooleanLiteral() }
        registerPrefix(TokenType.FALSE)      { parseBooleanLiteral() }
        registerPrefix(TokenType.MINUS)      { parsePrefixExpression() }
        registerPrefix(TokenType.NEGATION)   { parsePrefixExpression() }
        registerPrefix(TokenType.LPAREN)     { parseGroupedExpression() }
        registerPrefix(TokenType.IF)         { parseIfExpression() }
        registerPrefix(TokenType.FUN)        { parseFunctionLiteral() }

        registerInfix(TokenType.PLUS)     { left -> parseInfixExpression(left) }
        registerInfix(TokenType.MINUS)    { left -> parseInfixExpression(left) }
        registerInfix(TokenType.MULTIPLY) { left -> parseInfixExpression(left) }
        registerInfix(TokenType.DIVISION) { left -> parseInfixExpression(left) }
        registerInfix(TokenType.MOD)      { left -> parseInfixExpression(left) }
        registerInfix(TokenType.POW)      { left -> parseInfixExpression(left) }
        registerInfix(TokenType.EQ)       { left -> parseInfixExpression(left) }
        registerInfix(TokenType.DIF)      { left -> parseInfixExpression(left) }
        registerInfix(TokenType.LT)       { left -> parseInfixExpression(left) }
        registerInfix(TokenType.LTE)      { left -> parseInfixExpression(left) }
        registerInfix(TokenType.GT)       { left -> parseInfixExpression(left) }
        registerInfix(TokenType.GTE)      { left -> parseInfixExpression(left) }
        registerInfix(TokenType.AND)      { left -> parseInfixExpression(left) }
        registerInfix(TokenType.OR)       { left -> parseInfixExpression(left) }
        registerInfix(TokenType.LPAREN)   { left -> parseCallExpression(left) }
    }

    fun parseProgram(): Program {
        val program = Program()
        while (currentToken.tokenType != TokenType.EOF) {
            val statement = parseStatement()
            if (statement != null) program.statements.add(statement)
            advanceTokens()
        }
        return program
    }

    private fun parseStatement(): Statement? {
        return when (currentToken.tokenType) {
            TokenType.VAL      -> parseValStatement()
            TokenType.RETURN   -> parseReturnStatement()
            TokenType.WHILE    -> parseWhileStatement()
            TokenType.FOR      -> parseForStatement()
            TokenType.BREAK    -> BreakStatement(currentToken)
            TokenType.CONTINUE -> ContinueStatement(currentToken)
            TokenType.PRINT    -> parsePrintStatement()
            else               -> parseExpressionStatement()
        }
    }

    private fun parseValStatement(): ValStatement? {
        val token = currentToken
        if (!expectedToken(TokenType.IDENTIFIER)) return null
        val name = Identifier(currentToken, currentToken.literal)
        if (!expectedToken(TokenType.ASSIGN)) return null
        advanceTokens()
        val value = parseExpression(Precedence.LOWEST)
        if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
        return ValStatement(token, name, value)
    }

    private fun parseReturnStatement(): ReturnStatement {
        val token = currentToken
        advanceTokens()
        val returnValue = parseExpression(Precedence.LOWEST)
        if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
        return ReturnStatement(token, returnValue)
    }

    private fun parseWhileStatement(): WhileStatement? {
        val token = currentToken
        if (!expectedToken(TokenType.LPAREN)) return null
        advanceTokens()
        val condition = parseExpression(Precedence.LOWEST) ?: return null
        if (!expectedToken(TokenType.RPAREN)) return null
        if (!expectedToken(TokenType.LBRACE)) return null
        val body = parseBlockStatement()
        return WhileStatement(token, condition, body)
    }

    private fun parseForStatement(): ForStatement? {
        val token = currentToken
        if (!expectedToken(TokenType.LPAREN)) return null
        advanceTokens()
        val init = if (currentToken.tokenType != TokenType.SEMICOLON) parseStatement() else null
        if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
        advanceTokens()
        val condition = if (currentToken.tokenType != TokenType.SEMICOLON) parseExpression(Precedence.LOWEST) else null
        if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
        advanceTokens()
        // ✅ fix: update es Statement? igual que init
        val update = if (currentToken.tokenType != TokenType.RPAREN) parseStatement() else null
        if (!expectedToken(TokenType.RPAREN)) return null
        if (!expectedToken(TokenType.LBRACE)) return null
        val body = parseBlockStatement()
        return ForStatement(token, init, condition, update, body)
    }

    private fun parsePrintStatement(): PrintStatement? {
        val token = currentToken
        if (!expectedToken(TokenType.LPAREN)) return null
        advanceTokens()
        val value = parseExpression(Precedence.LOWEST) ?: return null
        if (!expectedToken(TokenType.RPAREN)) return null
        if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
        return PrintStatement(token, value)
    }

    private fun parseExpressionStatement(): ExpressionStatement {
        val token = currentToken
        val expression = parseExpression(Precedence.LOWEST)
        if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
        return ExpressionStatement(token, expression)
    }

    private fun parseBlockStatement(): BlockStatement {
        val token = currentToken
        val block = BlockStatement(token)
        advanceTokens()
        while (currentToken.tokenType != TokenType.RBRACE &&
               currentToken.tokenType != TokenType.EOF) {
            val stmt = parseStatement()
            if (stmt != null) block.statements.add(stmt)
            advanceTokens()
        }
        return block
    }

    private fun parseExpression(precedence: Precedence): Expression? {
        val prefixFn = prefixParseFns[currentToken.tokenType]
        if (prefixFn == null) {
            errors.add("No hay función prefix para ${currentToken.tokenType}")
            return null
        }
        var leftExpression = prefixFn()
        while (peekToken.tokenType != TokenType.SEMICOLON &&
               precedence.level < peekPrecedence().level) {
            val infixFn = infixParseFns[peekToken.tokenType] ?: return leftExpression
            advanceTokens()
            leftExpression = infixFn(leftExpression!!)
        }
        return leftExpression
    }

    private fun parseIdentifier(): Expression =
        Identifier(currentToken, currentToken.literal)

    private fun parseIntegerLiteral(): Expression? {
        return try {
            IntegerLiteral(currentToken, currentToken.literal.toInt())
        } catch (e: NumberFormatException) {
            errors.add("No se pudo convertir '${currentToken.literal}' a entero")
            null
        }
    }

    private fun parseFloatLiteral(): Expression? {
        return try {
            FloatLiteral(currentToken, currentToken.literal.toDouble())
        } catch (e: NumberFormatException) {
            errors.add("No se pudo convertir '${currentToken.literal}' a flotante")
            null
        }
    }

    private fun parseStringLiteral(): Expression =
        StringLiteral(currentToken, currentToken.literal)

    private fun parseBooleanLiteral(): Expression =
        BooleanLiteral(currentToken, currentToken.tokenType == TokenType.TRUE)

    // ✅ fix: PrefixExpression acepta Expression? ahora
    private fun parsePrefixExpression(): Expression {
        val token = currentToken
        val operator = currentToken.literal
        advanceTokens()
        val right = parseExpression(Precedence.PREFIX)
        return PrefixExpression(token, operator, right)
    }

    private fun parseGroupedExpression(): Expression? {
        advanceTokens()
        val expression = parseExpression(Precedence.LOWEST)
        if (!expectedToken(TokenType.RPAREN)) return null
        return expression
    }

    private fun parseIfExpression(): Expression? {
        val token = currentToken
        if (!expectedToken(TokenType.LPAREN)) return null
        advanceTokens()
        val condition = parseExpression(Precedence.LOWEST) ?: return null
        if (!expectedToken(TokenType.RPAREN)) return null
        if (!expectedToken(TokenType.LBRACE)) return null
        val consequence = parseBlockStatement()
        val alternatives = mutableListOf<Pair<Expression, BlockStatement>>()
        var elseBlock: BlockStatement? = null

        // ✅ fix: ELSEIF no ELIF
        while (peekToken.tokenType == TokenType.ELSEIF) {
            advanceTokens()
            if (!expectedToken(TokenType.LPAREN)) return null
            advanceTokens()
            val altCondition = parseExpression(Precedence.LOWEST) ?: return null
            if (!expectedToken(TokenType.RPAREN)) return null
            if (!expectedToken(TokenType.LBRACE)) return null
            alternatives.add(Pair(altCondition, parseBlockStatement()))
        }

        if (peekToken.tokenType == TokenType.ELSE) {
            advanceTokens()
            if (!expectedToken(TokenType.LBRACE)) return null
            elseBlock = parseBlockStatement()
        }

        return IfExpression(token, condition, consequence, alternatives, elseBlock)
    }

    // ✅ fix: FunctionLiteral ahora recibe (token, parameters, body) en ese orden
  private fun parseFunctionLiteral(): Expression? {
    val token = currentToken

    // ✅ Si el siguiente token es IDENTIFIER, es el nombre de la función
    var name = ""
    if (peekToken.tokenType == TokenType.IDENTIFIER) {
        advanceTokens()
        name = currentToken.literal
    }

    if (!expectedToken(TokenType.LPAREN)) return null
    val parameters = parseFunctionParameters()
    if (!expectedToken(TokenType.LBRACE)) return null
    val body = parseBlockStatement()

    val fn = FunctionLiteral(token, parameters, body)
    fn.name = name  // guarda el nombre si tiene
    return fn
}

    private fun parseFunctionParameters(): MutableList<Identifier> {
        val params = mutableListOf<Identifier>()
        if (peekToken.tokenType == TokenType.RPAREN) {
            advanceTokens()
            return params
        }
        advanceTokens()
        params.add(Identifier(currentToken, currentToken.literal))
        while (peekToken.tokenType == TokenType.COMMA) {
            advanceTokens()
            advanceTokens()
            params.add(Identifier(currentToken, currentToken.literal))
        }
        if (!expectedToken(TokenType.RPAREN)) return mutableListOf()
        return params
    }

    // ✅ fix: InfixExpression acepta Expression? ahora
    private fun parseInfixExpression(left: Expression): Expression {
        val token = currentToken
        val operator = currentToken.literal
        val precedence = currentPrecedence()
        advanceTokens()
        val right = parseExpression(precedence)
        return InfixExpression(token, left, operator, right)
    }

    private fun parseCallExpression(function: Expression): Expression {
        val token = currentToken
        val arguments = parseCallArguments()
        return CallExpression(token, function, arguments)
    }

    private fun parseCallArguments(): MutableList<Expression> {
        val args = mutableListOf<Expression>()
        if (peekToken.tokenType == TokenType.RPAREN) {
            advanceTokens()
            return args
        }
        advanceTokens()
        args.add(parseExpression(Precedence.LOWEST) ?: return mutableListOf())
        while (peekToken.tokenType == TokenType.COMMA) {
            advanceTokens()
            advanceTokens()
            val arg = parseExpression(Precedence.LOWEST) ?: continue
            args.add(arg)
        }
        if (!expectedToken(TokenType.RPAREN)) return mutableListOf()
        return args
    }

    private fun advanceTokens() {
        currentToken = peekToken
        peekToken = lexer.nextToken()
    }

    private fun expectedToken(type: TokenType): Boolean {
        return if (peekToken.tokenType == type) {
            advanceTokens()
            true
        } else {
            errors.add("Se esperaba $type pero se encontró ${peekToken.tokenType}")
            false
        }
    }

    private fun registerPrefix(type: TokenType, fn: PrefixParseFn) { prefixParseFns[type] = fn }
    private fun registerInfix(type: TokenType, fn: InfixParseFn) { infixParseFns[type] = fn }
    private fun peekPrecedence(): Precedence = TOKEN_PRECEDENCES[peekToken.tokenType] ?: Precedence.LOWEST
    private fun currentPrecedence(): Precedence = TOKEN_PRECEDENCES[currentToken.tokenType] ?: Precedence.LOWEST
}
