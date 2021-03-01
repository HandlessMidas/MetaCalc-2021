import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

sealed class Expr
data class Value(val value: Double): Expr()
data class Variable(val name: String): Expr()

sealed class BinaryOp(open val x: Expr, open val y: Expr): Expr()
data class Plus(override val x: Expr, override val y: Expr): BinaryOp(x, y)
data class Minus(override val x: Expr, override val y: Expr): BinaryOp(x, y)
data class Multiply(override val x: Expr, override val y: Expr): BinaryOp(x, y)
data class Divide(override val x: Expr, override val y: Expr): BinaryOp(x, y)
data class Pow(override val x: Expr, override val y: Expr): BinaryOp(x, y)

sealed class UnaryOp(open val x: Expr): Expr()
data class Sin(override val x: Expr): UnaryOp(x)
data class Cos(override val x: Expr): UnaryOp(x)

fun evalBinaryOp (op: BinaryOp, env: Map<Variable, Value>): Expr {
    val x: Expr = eval(op.x, env)
    val y: Expr = eval(op.y, env)
    return when(op) {
        is Plus -> evalPlus(x, y)
        is Minus -> evalMinus(x, y)
        is Multiply -> evalMultiply(x, y)
        is Divide -> evalDivide(x, y)
        is Pow -> evalPow(x, y)
    }
}

fun evalPlus(x: Expr, y: Expr): Expr {
    return when {
        (x is Value && x.value == 0.0) -> y
        (y is Value && y.value == 0.0) -> x
        (x is Value && y is Value) -> Value(x.value + y.value)
        else -> Plus(x, y)
    }
}


fun evalDivide(x: Expr, y: Expr): Expr {
    return when {
        (x is Value && y is Value) -> Value(x.value / y.value)
        (x is Value && x.value == 0.0) -> Value(0.0)
        (y is Value && y.value == 0.0) -> throw ArithmeticException("Division by zero")
        (y is Value && y.value == 1.0) -> x
        else -> Divide(x, y)
    }
}


fun evalMinus(x: Expr, y: Expr): Expr {
    return when {
        (x is Value && y is Value) -> Value(x.value - y.value)
        (y is Value && y.value == 0.0) -> x
        else -> Minus(x, y)
    }
}


fun evalMultiply(x: Expr, y: Expr): Expr {
    return when {
        (x is Value && x.value == 0.0) -> Value(0.0)
        (y is Value && y.value == 0.0) -> Value(0.0)
        (x is Value && x.value == 1.0) -> y
        (y is Value && y.value == 1.0) -> x
        (x is Value && y is Value) -> Value(x.value * y.value)
        else -> Multiply(x, y)
    }
}


fun evalPow(x: Expr, y: Expr): Expr {
    return when {
        (x is Value && x.value == 0.0) -> Value(x.value)
        (x is Value && x.value == 1.0) -> Value(x.value)
        (y is Value && y.value == 0.0) -> Value(1.0)
        (y is Value && y.value == 1.0) -> x
        (x is Value && y is Value) -> Value(x.value.pow(y.value))
        else -> Pow(x, y)
    }
}

fun evalUnaryOp(op: UnaryOp, env: Map<Variable, Value>): Expr {
    val x = eval(op.x, env)
    return when(op) {
        is Sin -> evalSin(x)
        is Cos -> evalCos(x)
    }
}

fun evalSin(x: Expr): Expr {
    return when {
        (x is Value) -> Value(sin(x.value))
        else -> Sin(x)
    }
}

fun evalCos(x: Expr): Expr {
    return when {
        (x is Value) -> Value(cos(x.value))
        else -> Cos(x)
    }
}

fun eval(expr: Expr, env: Map<Variable, Value>): Expr {
    return when(expr) {
        is Value -> expr
        is Variable -> env.getOrElse(expr) { return expr }
        is UnaryOp -> evalUnaryOp(expr, env)
        is BinaryOp -> evalBinaryOp(expr, env)
    }
}


fun diffPlus(expr: Plus, variable: Variable): Expr {
    return Plus(
        diff(expr.x, variable),
        diff(expr.y, variable)
    )
}

fun diffMinus(expr: Minus, variable: Variable): Expr {
    return Minus(
        diff(expr.x, variable),
        diff(expr.y, variable)
    )
}

fun diffMultiply(expr: Multiply, variable: Variable): Expr {
    return Plus(
        Multiply(expr.x, diff(expr.y, variable)),
        Multiply(expr.y, diff(expr.x, variable))
    )
}

fun diffDivide(expr: Divide, variable: Variable): Expr {
    return Divide(
        Minus(
            Multiply(diff(expr.x, variable), expr.y),
            Multiply(diff(expr.y, variable), expr.x)
        ),
        Pow(expr.y, Value(2.0))
    )
}

fun diffPow(expr: Pow, variable: Variable): Expr {
    return Multiply(
        Multiply(
            expr.y,
            Pow(
                expr.x,
                Minus(expr.y, Value(1.0))
            )
        ),
        diff(expr.x, variable)
    )
}

fun diffBinaryOp(expr: BinaryOp, variable: Variable): Expr {
    return when (expr) {
        is Plus -> diffPlus(expr, variable)
        is Minus -> diffMinus(expr, variable)
        is Multiply -> diffMultiply(expr, variable)
        is Divide -> diffDivide(expr, variable)
        is Pow -> diffPow(expr, variable)
    }
}

fun diffSin(expr: Sin, variable: Variable): Expr {
    return Multiply(
        Cos(expr.x),
        diff(expr.x, variable)
    )
}

fun diffCos(expr: Cos, variable: Variable): Expr {
    return Minus(
        Value(0.0),
        Multiply(
            Sin(expr.x),
            diff(expr.x, variable)
        )
    )
}

fun diffUnaryOp(expr: UnaryOp, variable: Variable): Expr {
    return when (expr) {
        is Sin -> diffSin(expr, variable)
        is Cos -> diffCos(expr, variable)
    }
}

fun diff(expr: Expr, variable: Variable): Expr {
    val diffExpr = when (expr) {
        is Value -> Value(0.0)
        is Variable ->
            if (expr.name == variable.name) {
                Value(1.0)
            } else {
                Value(0.0)
            }
        is BinaryOp -> diffBinaryOp(expr, variable)
        is UnaryOp -> diffUnaryOp(expr, variable)
    }
    return eval(diffExpr, emptyMap())
}




