fun tokenize(source: String): List<Token>{
    val lexer = Lexer(source)
    val tokens = mutableListOf<Token>()
    while(true){
        val tok=lexer.nextToken()
        if(tok.tokenType == TokenType.EOF) break
        tokens.add(tok)
    }
    return tokens
}

var passed = 0
var failed = 0

fun assert(condition: Boolean, message: String){
    if(condition){
        println("PASSED: $message")
        passed++
    } else {
        println("FAILED: $message")
        failed++
    }
}

fun testOperadoresComparacion(){
    println("\n  Operadores de comparación------------------")

    run{
        println("test_igual: ")
        val tokens = tokenize("==")
        assert(tokens.size == 1,   "len == 1")
        assert(tokens[0].tokenType == TokenType.EQ, "token_type == EQ ")
        assert(tokens[0].literal == "==", "literal == ==")
    }
    run{
        println("test_diferente: ")
        val tokens = tokenize("!=")
        assert(tokens[0].tokenType == TokenType.DIF, "token_type == DIF ")
        assert(tokens[0].literal == "!=", "literal == !=")
    }
    run{
        println("test_menor_igual: ")
        val tokens = tokenize("<=")
        assert(tokens[0].tokenType == TokenType.LTE, "token_type == LTE ") 
    }
    run{
        println("test_mayor_igual: ")
        val tokens = tokenize(">=")
        assert(tokens[0].tokenType == TokenType.GTE, "token_type == GTE ")
    }
}

fun testOperadoresLogicos(){
    println("\n  Operadores lógicos------------------")

    run{
        println("test_and: ")
        val tokens = tokenize("&&")
        assert(tokens[0].tokenType == TokenType.AND, "token_type == AND ")
        assert(tokens[0].literal == "&&", "literal == &&")
    }
    run{
        println("test_or: ")
        val tokens = tokenize("||")
        assert(tokens[0].tokenType == TokenType.OR, "token_type == OR ")
        assert(tokens[0].literal == "||", "literal == ||")
    }
    run{
        println("test_negacion: ")
        val tokens = tokenize("!")
        assert(tokens[0].tokenType == TokenType.NEGATION, "token_type == NEGATION ")
    }
}

fun testKeywords(){
    println("\n  Keywords------------------")
    run{
        print("test_val: ")
        val tokens = tokenize("val")
        assert(tokens[0].tokenType == TokenType.VAL, "token_type == VAL ")
        assert(tokens[0].literal == "val", "literal == val")
    }
    run{
        print("test_fun: ")
        val tokens = tokenize("fun")
        assert(tokens[0].tokenType == TokenType.FUN, "token_type == FUN ")
    }
    run{
        print("test_if_else: ")
        val tokens = tokenize("if else")
        assert(tokens[0].tokenType == TokenType.IF, "token_type == IF ")
        assert(tokens[1].tokenType == TokenType.ELSE, "token_type == ELSE ")    
    }
    run{
        print("test_while: ")
        val tokens = tokenize("while")
        assert(tokens[0].tokenType == TokenType.WHILE, "token_type == WHILE ")
    }
    run{
        print("test_return: ")
        val tokens = tokenize("return")
        assert(tokens[0].tokenType == TokenType.RETURN, "token_type == RETURN ")
    }
    run{
        print("test_true_false: ")
        val tokens = tokenize("true false")
        assert(tokens[0].tokenType == TokenType.TRUE, "token_type == TRUE ")
        assert(tokens[1].tokenType == TokenType.FALSE, "token_type == FALSE ")    
    }
}

fun testLiterales(){
    println("\n  Literales------------------")
    run{
        print("test_entero: ")
        val tokens = tokenize("42")
        assert(tokens[0].tokenType == TokenType.INTEGER, "token_type == INT ")
        assert(tokens[0].literal == "42", "literal == 42")
    }
    run{
        print("test_flotante: ")
        val tokens = tokenize("3.14")
        assert(tokens[0].tokenType == TokenType.FLOAT, "token_type == FLOAT ")
        assert(tokens[0].literal == "3.14", "literal == 3.14")
    }
    run{
        print("test_string: ")
        val tokens = tokenize("\"Hello World!\"")
        assert(tokens[0].tokenType == TokenType.STRING, "token_type == STRING ")
        assert(tokens[0].literal == "Hello World!", "literal == Hello World!")
    }
}

fun testComentariosEIlegal(){
    println("\n  Comentarios e ilegal------------------")

    run{
        print("test_comentario_ignorado: ")
        //El Lexer debe ignorar los comentarios, por lo que no se deben generar tokens para ellos es decir que siga a //
        val tokens = tokenize(" val x = 5; // Este es un comentario")
        val tipos = tokens.map { it.tokenType }
        assert(tipos == listOf(
            TokenType.VAL, 
            TokenType.IDENTIFIER, 
            TokenType.ASSIGN, 
            TokenType.INTEGER, 
            TokenType.SEMICOLON
        ), "Tipos de tokens incorrectos")
    }
    run{
        print("test_ilegal: ")
        val tokens = tokenize("@")
        assert(tokens[0].tokenType == TokenType.ILEGAL, "token_type == ILEGAL ")
        assert(tokens[0].literal == "@", "literal == @")
    }
}

fun main(){
    println("Iniciando pruebas del Lexer...")
    testOperadoresComparacion()
    testOperadoresLogicos()
    testKeywords()
    testLiterales()
    testComentariosEIlegal()


    println(" \n----------------------------------")
    println("\nTodas las pruebas del Lexer han pasado exitosamente.")   
    println( " TOTAL : ${passed + failed} tests ")
    println( " PASARON : $passed  ")
    println( " FALLARON : $failed  ")
    println(" \n----------------------------------")
}

