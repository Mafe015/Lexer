//union reply.py y main.py en un solo archivo main.kt

fun main(args: Array<String>) {
    val env = Environment()  // un solo entorno para todo

    // Si se pasa un archivo, lo ejecuta primero
    if (args.isNotEmpty()) {
        val source = java.io.File(args[0]).readText()
        val lexer = Lexer(source)
        val parser = Parser(lexer)
        val program = parser.parseProgram()

        if (parser.errors.isNotEmpty()) {
            println("Errores del parser:")
            parser.errors.forEach { println("  ❌ $it") }
        } else {
            evaluate(program, env)
        }
    }

    // Siempre abre el REPL después
    println("Bienvenido al Intérprete Kotlin — escribe 'exit' para salir")
    startRepl(env)  // pasa el mismo entorno
}

fun startRepl(env: Environment = Environment()) {
    while (true) {
        print("~> ")
        val source = readLine() ?: break
        if (source.lowercase() == "exit") break

        val lexer = Lexer(source)
        val parser = Parser(lexer)
        val program = parser.parseProgram()

        if (parser.errors.isNotEmpty()) {
            println("Errores del parser:")
            parser.errors.forEach { println("  ❌ $it") }
            continue
        }

        val result = evaluate(program, env)
        if (result != null && result !is NullObj) {
            println(result.inspect())
        }
    }
}