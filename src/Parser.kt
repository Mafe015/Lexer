enum class Precedence(val level: Int) {
    LOWEST(1), OR(2), AND(3), EQUALS(4), COMPARE(5),
    SUM(6), PRODUCT(7), PREFIX(8), POWER(9), CALL(10)
}

val TOKEN_PRECEDENCES = mapOf(
    TokenType.OR to Precedence.OR, TokenType.AND to Precedence.AND,
    TokenType.EQ to Precedence.EQUALS, TokenType.DIF to Precedence.EQUALS,
    TokenType.LT to Precedence.COMPARE, TokenType.LTE to Precedence.COMPARE,
    TokenType.GT to Precedence.COMPARE, TokenType.GTE to Precedence.COMPARE,
    TokenType.PLUS to Precedence.SUM, TokenType.MINUS to Precedence.SUM,
    TokenType.MULTIPLY to Precedence.PRODUCT, TokenType.DIVISION to Precedence.PRODUCT,
    TokenType.MOD to Precedence.PRODUCT, TokenType.POW to Precedence.POWER,
    TokenType.LPAREN to Precedence.CALL
)

typealias PrefixParseFn = () -> Expression?
typealias InfixParseFn = (Expression) -> Expression?

class Parser(private val lexer: Lexer) {
    private var currentToken: Token = Token(TokenType.EOF, "")
    private var peekToken: Token = Token(TokenType.EOF, "")
    val errors = mutableListOf<String>()
    private val prefixParseFns = mutableMapOf<TokenType, PrefixParseFn>()
    private val infixParseFns = mutableMapOf<TokenType, InfixParseFn>()

    init {
        advanceTokens(); advanceTokens()
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
        registerInfix(TokenType.PLUS)     { l -> parseInfixExpression(l) }
        registerInfix(TokenType.MINUS)    { l -> parseInfixExpression(l) }
        registerInfix(TokenType.MULTIPLY) { l -> parseInfixExpression(l) }
        registerInfix(TokenType.DIVISION) { l -> parseInfixExpression(l) }
        registerInfix(TokenType.MOD)      { l -> parseInfixExpression(l) }
        registerInfix(TokenType.POW)      { l -> parseInfixExpression(l) }
        registerInfix(TokenType.EQ)       { l -> parseInfixExpression(l) }
        registerInfix(TokenType.DIF)      { l -> parseInfixExpression(l) }
        registerInfix(TokenType.LT)       { l -> parseInfixExpression(l) }
        registerInfix(TokenType.LTE)      { l -> parseInfixExpression(l) }
        registerInfix(TokenType.GT)       { l -> parseInfixExpression(l) }
        registerInfix(TokenType.GTE)      { l -> parseInfixExpression(l) }
        registerInfix(TokenType.AND)      { l -> parseInfixExpression(l) }
        registerInfix(TokenType.OR)       { l -> parseInfixExpression(l) }
        registerInfix(TokenType.LPAREN)   { l -> parseCallExpression(l) }
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

    private fun parseStatement(): Statement? = when (currentToken.tokenType) {
        TokenType.VAL      -> parseValStatement()
        TokenType.RETURN   -> parseReturnStatement()
        TokenType.WHILE    -> parseWhileStatement()
        TokenType.FOR      -> parseForStatement()
        TokenType.BREAK    -> BreakStatement(currentToken)
        TokenType.CONTINUE -> ContinueStatement(currentToken)
        TokenType.PRINT    -> parsePrintStatement()
        else               -> parseExpressionStatement()
    }

    private fun parseValStatement(): ValStatement? {
        val token = currentToken
        if (!expectedToken(TokenType.IDENTIFIER)) return null
        val name = Identifier(currentToken, currentToken.literal)
        if (!expectedToken(TokenType.ASSIGN)) return null
        advanceTokens()
        val value = parseExpression(Precedence.LOWEST)
        // consume ; si está en peek
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

    // ==========================================================================
    // parseForStatement — LOGICA VERIFICADA con simulacion
    //
    // for ( val j = 1 ; j <= 6 ; val j = j + 1 )
    //
    // INIT: parseValStatement consume el ;
    //   → currentToken = ; al salir
    //   → advanceTokens() → llegamos al primer token de la condicion
    //
    // CONDITION: parseExpression para cuando peek = ;
    //   → currentToken = ultimo token de la expr (ej: INT:6)
    //   → peekToken = ;
    //   → if peek==; → consume → currentToken=;, peek=primer token update
    //   → advanceTokens() → currentToken = primer token del update (VAL)
    //
    // UPDATE: parseValStatement — peek despues del expr es ) no ;
    //   → NO consume nada al final
    //   → currentToken = ultimo token del update
    //   → expectedToken(RPAREN) avanza al )
    // ==========================================================================
    private fun parseForStatement(): ForStatement? {
    val token = currentToken
    if (!expectedToken(TokenType.LPAREN)) return null
    advanceTokens()

    // ── INIT ──
    val init: Statement?
    if (currentToken.tokenType == TokenType.SEMICOLON) {
        init = null
    } else if (currentToken.tokenType == TokenType.VAL) {
        val valToken = currentToken
        if (!expectedToken(TokenType.IDENTIFIER)) return null
        val name = Identifier(currentToken, currentToken.literal)
        if (!expectedToken(TokenType.ASSIGN)) return null
        advanceTokens()
        val value = parseExpression(Precedence.LOWEST)
        init = ValStatement(valToken, name, value)
    } else {
        init = parseExpressionStatement()
    }
    if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
    advanceTokens()

    // ── CONDICION ──
    val condition: Expression?
    if (currentToken.tokenType == TokenType.SEMICOLON) {
        condition = null
    } else {
        condition = parseExpression(Precedence.LOWEST)
        if (peekToken.tokenType == TokenType.SEMICOLON) advanceTokens()
        advanceTokens()
    }

    // ── UPDATE ──
    val update: Statement?
    if (currentToken.tokenType == TokenType.RPAREN) {
        update = null
    } else if (currentToken.tokenType == TokenType.VAL) {
        val valToken = currentToken
        if (!expectedToken(TokenType.IDENTIFIER)) return null
        val name = Identifier(currentToken, currentToken.literal)
        if (!expectedToken(TokenType.ASSIGN)) return null
        advanceTokens()
        val value = parseExpression(Precedence.LOWEST)
        update = ValStatement(valToken, name, value)
    } else {
        update = parseExpressionStatement()
    }

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

    private fun parseIdentifier(): Expression = Identifier(currentToken, currentToken.literal)

    private fun parseIntegerLiteral(): Expression? = try {
        IntegerLiteral(currentToken, currentToken.literal.toInt())
    } catch (e: NumberFormatException) {
        errors.add("No se pudo convertir '${currentToken.literal}' a entero"); null
    }

    private fun parseFloatLiteral(): Expression? = try {
        FloatLiteral(currentToken, currentToken.literal.toDouble())
    } catch (e: NumberFormatException) {
        errors.add("No se pudo convertir '${currentToken.literal}' a flotante"); null
    }

    private fun parseStringLiteral(): Expression = StringLiteral(currentToken, currentToken.literal)
    private fun parseBooleanLiteral(): Expression = BooleanLiteral(currentToken, currentToken.tokenType == TokenType.TRUE)

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

    private fun parseFunctionLiteral(): Expression? {
        val token = currentToken
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
        fn.name = name
        return fn
    }

    private fun parseFunctionParameters(): MutableList<Identifier> {
        val params = mutableListOf<Identifier>()
        if (peekToken.tokenType == TokenType.RPAREN) { advanceTokens(); return params }
        advanceTokens()
        params.add(Identifier(currentToken, currentToken.literal))
        while (peekToken.tokenType == TokenType.COMMA) {
            advanceTokens(); advanceTokens()
            params.add(Identifier(currentToken, currentToken.literal))
        }
        if (!expectedToken(TokenType.RPAREN)) return mutableListOf()
        return params
    }

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
        if (peekToken.tokenType == TokenType.RPAREN) { advanceTokens(); return args }
        advanceTokens()
        args.add(parseExpression(Precedence.LOWEST) ?: return mutableListOf())
        while (peekToken.tokenType == TokenType.COMMA) {
            advanceTokens(); advanceTokens()
            val arg = parseExpression(Precedence.LOWEST) ?: continue
            args.add(arg)
        }
        if (!expectedToken(TokenType.RPAREN)) return mutableListOf()
        return args
    }

    private fun advanceTokens() { currentToken = peekToken; peekToken = lexer.nextToken() }

    private fun expectedToken(type: TokenType): Boolean {
        return if (peekToken.tokenType == type) { advanceTokens(); true }
        else { errors.add("Se esperaba $type pero se encontró ${peekToken.tokenType}"); false }
    }

    private fun registerPrefix(type: TokenType, fn: PrefixParseFn) { prefixParseFns[type] = fn }
    private fun registerInfix(type: TokenType, fn: InfixParseFn) { infixParseFns[type] = fn }
    private fun peekPrecedence(): Precedence = TOKEN_PRECEDENCES[peekToken.tokenType] ?: Precedence.LOWEST
    private fun currentPrecedence(): Precedence = TOKEN_PRECEDENCES[currentToken.tokenType] ?: Precedence.LOWEST
}