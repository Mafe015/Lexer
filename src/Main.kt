//union reply.py y main.py en un solo archivo main.kt

fun startRepl(){
    val eofToken = Token(TokenType.EOF, "")

    while(true){
        print(">> ")
        val source = readLine() ?: break
        if(source.lowercase() == "exit") break

        val lexer = Lexer(source)
        var token = lexer.nextToken()
        while (token != eofToken){
            when{
                token.tokenType == TokenType.ILEGAL -> 
                    println("ERROR: Caracter no reconocido '${token.literal}'")
                
                token.tokenType == TokenType.STRING &&
                token.literal=="ERROR string sin cerrar" -> 
                    println("ERROR: String sin cerrar u abrir, le falta '\"'")
                
                else -> println(token)
            }
           
            
            token = lexer.nextToken()
        }
    }
}

fun main(){
    val eofToken = Token(TokenType.EOF, "")
    println("Bienvenido al Lexer de Kotlin, para salir ingresa 'exit'")
    startRepl()
}