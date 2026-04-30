
data class Token(val tokenType: TokenType, val literal: String){

    override fun toString(): String {
        return "Type : ${tokenType.name.padEnd(12)} Literal: '$literal'"
    }
}

fun lookupTokenType(literal: String): TokenType {
    
    val keywords = mapOf(
        "fun" to TokenType.FUN,
        "val" to TokenType.VAL,
        "return" to TokenType.RETURN,
        "if" to TokenType.IF,
        "elif" to TokenType.ELSEIF,
        "else" to TokenType.ELSE,
        "while" to TokenType.WHILE,
        "for" to TokenType.FOR,
        "break" to TokenType.BREAK,
        "continue" to TokenType.CONTINUE,
        "print" to TokenType.PRINT,
        "true" to TokenType.TRUE,
        "false" to TokenType.FALSE,
    )
    return keywords.getOrDefault(literal, TokenType.IDENTIFIER)
}
