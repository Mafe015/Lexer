enum class TokenType {
    //operadores aritmenticos 
    PLUS,       // +
    MINUS,      // -
    MULTIPLY,   // * 
    DIVISION,   // /
    MOD,        // %
    POW,        // ^


    //Comparadores
    EQ,         // ==
    DIF,        // !=
    LT,         // <
    LTE,        // <=
    GT,         // >
    GTE,        // >=

    //Logicos
    AND,        // &&
    OR,         // ||
    NEGATION,   // !

    //Asignacion
    ASSIGN,     // =

    //Delimitadores
    COMMA,      // ,
    SEMICOLON,  // ;
    LPAREN,     // (
    RPAREN,     // )
    LBRACE,     // {
    RBRACE,     // }

    //Literales
    INTEGER,    //42
    FLOAT,      //3.14
    STRING,     // "Hello, World!"
    TRUE,       // true
    FALSE,      // false

    //Identificadores 
    IDENTIFIER,


    //Keywords-Kotlin
    FUN,        //fun
    VAL,        //val
    RETURN,     // return
    IF,         // if
    ELSEIF,     // else if
    ELSE,       // else
    WHILE,      // while
    FOR,        //for
    BREAK,      // break
    CONTINUE,   // continue
    PRINT,      // print

    //Especiales
    EOF,
    ILEGAL,

    //Preguntas
    QUESTION   // ?
}