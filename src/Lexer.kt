class Lexer(private val source:String){

    private var character: Char = '\u0000'
    private var position: Int = 0
    private var readPosition: Int = 0

    init{
        readCharacter()
    }

    fun nextToken(): Token {
        skipWhitespaceAndComments()
        val token: Token = when{

            character =='\u0000' -> Token(TokenType.EOF, "")

            //Operadores Aritmeticos
            character == '+' -> Token(TokenType.PLUS, "+")
            character == '-' -> Token(TokenType.MINUS, "-")
            character == '%' -> Token(TokenType.MOD, "%")

            // ** para potencia
            character == '*' && peekCharacter() == '*' -> {
                readCharacter()
                Token(TokenType.POW, "**")
            }
            character =='*' -> Token(TokenType.MULTIPLY, "*")
            character == '/' -> Token(TokenType.DIVISION, "/")

            // == y == para comparacion y asignacion
            character == '=' && peekCharacter() == '=' -> {
                readCharacter()
                Token(TokenType.EQ, "==")
            }
            character == '=' -> Token(TokenType.ASSIGN, "=")

            // != y ! para comparacion y negacion
            character == '!' && peekCharacter() == '=' -> {
                readCharacter()
                Token(TokenType.DIF, "!=")
            }
            character == '!' -> Token(TokenType.NEGATION, "!")

            // >= y > para comparacion
            character == '>' && peekCharacter() == '=' -> { 
                readCharacter()
                Token(TokenType.GTE, ">=")
            }
            character == '>' -> Token(TokenType.GT, ">")

            //<= y < para comparacion
            character == '<' && peekCharacter() == '=' -> {
                readCharacter()
                Token(TokenType.LTE, "<=")
            }   
            character == '<' -> Token(TokenType.LT, "<")

            // && y || para operadores logicos
            character == '&' && peekCharacter() == '&' -> {
                readCharacter()
                Token(TokenType.AND, "&&")
            }
            character == '|' && peekCharacter() == '|' -> {
                readCharacter()
                Token(TokenType.OR, "||")
            }

            //Delimitadores
            character == ',' -> Token(TokenType.COMMA, ",")
            character == ';' -> Token(TokenType.SEMICOLON, ";")
            character == '(' -> Token(TokenType.LPAREN, "(")
            character == ')' -> Token(TokenType.RPAREN, ")")
            character == '{' -> Token(TokenType.LBRACE, "{")
            character == '}' -> Token(TokenType.RBRACE, "}")

            //strings
            character == '"' -> return Token(TokenType.STRING, readString())

            //Identificadores y keywords
            isLetter(character) -> {
                val literal = readIdentifier()
                return Token(lookupTokenType(literal), literal)
            }

            //Numeros
            character.isDigit() ->{
                val (literal, type) = readNumber()
                return Token(type, literal)
            }
            character == '?' -> Token(TokenType.QUESTION, "?")

            else -> Token(TokenType.ILEGAL, character.toString())
        }
     readCharacter()
     return token       
    }        

    private fun readCharacter(){
    character = if(readPosition >= source.length) '\u0000' 
                else source[readPosition]
    position = readPosition
    readPosition++
    }

    private fun peekCharacter(): Char =
     if(readPosition >= source.length) '\u0000' 
        else source[readPosition]

    private fun skipWhitespaceAndComments(){
        while(true){
            when{
                character.isWhitespace() -> readCharacter()
                character == '/' && peekCharacter() == '/' -> 
                    while(character != '\n' && character != '\u0000') readCharacter()
                else -> break
            }  
        }
    }   

    private fun isLetter(ch:Char): Boolean= ch.isLetter() || ch == '_'

    private fun readIdentifier(): String{
    val start = position
    while(isLetter(character) || character.isDigit()) readCharacter()
    return source.substring(start, position)
    }

    private fun readNumber(): Pair<String, TokenType>{
        val start = position
        var type = TokenType.INTEGER
        while(character.isDigit()) readCharacter()
        if(character == '.' && peekCharacter().isDigit()){
            type = TokenType.FLOAT
            readCharacter()
           while(character.isDigit()) readCharacter()
        }
    return source.substring(start, position) to type
    }

    private fun readString(): String{
        readCharacter() // saltar la comilla inicial
        val start = position 
        while (character != '"' && character != '\u0000') readCharacter()
        if (character == '\u0000') {
            return "ERROR string sin cerrar"
        }
        val literal = source.substring(start, position)
        readCharacter() // saltar la comilla final
        return literal
    }
}