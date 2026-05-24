// =============================================================================
// ObjectSystem.kt — Sistema de tipos en tiempo de ejecución
//
// Cuando el evaluador ejecuta código, necesita representar los valores
// en memoria. Este archivo define esos tipos.
//
// Python:  class ObjectType(str, Enum)
// Kotlin:  enum class ObjectType
// =============================================================================

enum class ObjectType {
    INTEGER,      // número entero: 42
    FLOAT,        // número decimal: 3.14
    BOOLEAN,      // true / false
    STRING,       // "hola mundo"
    NULL,         // valor nulo
    RETURN_VALUE, // valor siendo retornado por return
    ERROR,        // error en tiempo de ejecución
    FUNCTION,     // una función definida por el usuario
}

// =============================================================================
// Clase base — todos los objetos heredan de Object
// Python:  class Object(ABC)
// Kotlin:  abstract class Object
// =============================================================================
abstract class Object {
    abstract fun type(): ObjectType
    abstract fun inspect(): String  // equivalente a __str__ en Python
}

// =============================================================================
// Integer — número entero
// =============================================================================
class IntegerObj(val value: Int) : Object() {
    override fun type() = ObjectType.INTEGER
    override fun inspect() = value.toString()
}

// =============================================================================
// Float — número decimal
// =============================================================================
class FloatObj(val value: Double) : Object() {
    override fun type() = ObjectType.FLOAT
    override fun inspect() = value.toString()
}

// =============================================================================
// Boolean — verdadero o falso
// Solo existen DOS instancias: TRUE y FALSE (como singletons)
// =============================================================================
class BooleanObj(val value: Boolean) : Object() {
    override fun type() = ObjectType.BOOLEAN
    override fun inspect() = value.toString()
}

// Singletons — no crear nuevas instancias cada vez
// Python:  TRUE = Boolean(True) / FALSE = Boolean(False)
val TRUE_OBJ  = BooleanObj(true)
val FALSE_OBJ = BooleanObj(false)

fun nativeBoolToBooleanObj(value: Boolean): BooleanObj =
    if (value) TRUE_OBJ else FALSE_OBJ

// =============================================================================
// String — cadena de texto
// =============================================================================
class StringObj(val value: String) : Object() {
    override fun type() = ObjectType.STRING
    override fun inspect() = "\"$value\""
}

// =============================================================================
// Null — valor nulo (solo existe UNA instancia)
// =============================================================================
class NullObj : Object() {
    override fun type() = ObjectType.NULL
    override fun inspect() = "null"
}

val NULL_OBJ = NullObj()  // singleton

// =============================================================================
// ReturnValue — envuelve un valor que está siendo retornado
// Sirve para "burbujear" el return a través de bloques anidados
// =============================================================================
class ReturnValue(val value: Object) : Object() {
    override fun type() = ObjectType.RETURN_VALUE
    override fun inspect() = value.inspect()
}

// =============================================================================
// Error — error en tiempo de ejecución
// =============================================================================
class ErrorObj(val message: String) : Object() {
    override fun type() = ObjectType.ERROR
    override fun inspect() = "ERROR: $message"
}

fun newError(message: String) = ErrorObj(message)
fun isError(obj: Object?): Boolean = obj?.type() == ObjectType.ERROR

// =============================================================================
// Function — una función definida por el usuario
// Guarda: parámetros, cuerpo, y el entorno donde fue definida (closure)
// =============================================================================
class FunctionObj(
    val parameters: List<Identifier>,   // lista de parámetros
    val body: BlockStatement,           // cuerpo de la función
    val env: Environment                // entorno donde fue definida
) : Object() {
    override fun type() = ObjectType.FUNCTION
    override fun inspect(): String {
        val params = parameters.joinToString(", ")
        return "fun($params) {\n$body\n}"
    }
}

// =============================================================================
// Environment — donde se guardan las variables
//
// Es un diccionario que mapea nombres → objetos
// Tiene un entorno PADRE (outer) para soportar scopes anidados y closures
//
// Python:  class Environment:
//              _store: Dict[str, Object]
//              _outer: Optional[Environment]
// =============================================================================
class Environment(val outer: Environment? = null) {

    // el diccionario donde se guardan las variables
    // Python:  self._store: Dict[str, Object] = {}
    private val store = mutableMapOf<String, Object>()

    // Busca una variable por nombre
    // Primero en el entorno actual, luego en el padre (outer)
    // Python:  def get(self, name: str)
    fun get(name: String): Object? {
        return store[name] ?: outer?.get(name)
        // store[name] busca en el entorno actual
        // ?: outer?.get(name) si no está, busca en el padre
    }

    // Guarda una variable en el entorno actual
    // Python:  def set(self, name: str, value: Object)
    fun set(name: String, value: Object): Object {
        store[name] = value
        return value
    }
}