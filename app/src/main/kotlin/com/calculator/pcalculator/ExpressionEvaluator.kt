package com.calculator.pcalculator

import java.util.Stack

object ExpressionEvaluator {

    fun evaluate(expression: String): Double {
        val cleaned = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("%", "/100")

        return evaluateExpression(cleaned)
    }

    private fun evaluateExpression(expr: String): Double {
        val numStack = Stack<Double>()
        val opStack = Stack<Char>()
        var i = 0

        while (i < expr.length) {
            when {
                expr[i].isDigit() || expr[i] == '.' -> {
                    val num = parseNumber(expr, i)
                    numStack.push(num.first)
                    i = num.second - 1
                }
                // ── Unary +/- ফিক্স ───────────────────────────────────────
                // এক্সপ্রেশনের শুরুতে, বা '(' / অন্য কোনো অপারেটরের ঠিক পরে
                // '+' / '-' আসলে সেটা বাইনারি অপারেটর না, বরং পরের সংখ্যার sign।
                // আগে এই কেসটা হ্যান্ডল না হওয়ায় "-5+3" বা "5×-3" টাইপ
                // এক্সপ্রেশনে Stack underflow হয়ে ক্যালকুলেটর crash করত।
                (expr[i] == '-' || expr[i] == '+') && isUnaryContext(expr, i) -> {
                    val num = parseSignedNumber(expr, i)
                    numStack.push(num.first)
                    i = num.second - 1
                }
                expr[i] == '(' -> opStack.push('(')
                expr[i] == ')' -> {
                    while (opStack.isNotEmpty() && opStack.peek() != '(') {
                        applyOperation(numStack, opStack)
                    }
                    if (opStack.isNotEmpty()) opStack.pop() // remove '('
                }
                isOperator(expr[i]) -> {
                    while (opStack.isNotEmpty() && 
                           precedence(opStack.peek()) >= precedence(expr[i])) {
                        applyOperation(numStack, opStack)
                    }
                    opStack.push(expr[i])
                }
            }
            i++
        }

        while (opStack.isNotEmpty()) {
            applyOperation(numStack, opStack)
        }

        return numStack.pop()
    }

    private fun isUnaryContext(expr: String, i: Int): Boolean {
        if (i == 0) return true
        val prev = expr[i - 1]
        return prev == '(' || isOperator(prev)
    }

    private fun parseSignedNumber(expr: String, start: Int): Pair<Double, Int> {
        val sign = if (expr[start] == '-') -1.0 else 1.0
        var i = start + 1
        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
        val numPart = expr.substring(start + 1, i)
        val value = if (numPart.isEmpty() || numPart == ".") 0.0 else numPart.toDouble()
        return Pair(sign * value, i)
    }

    private fun parseNumber(expr: String, start: Int): Pair<Double, Int> {
        var i = start
        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
        return Pair(expr.substring(start, i).toDouble(), i)
    }

    private fun isOperator(c: Char): Boolean = c in "+-*/"

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        else -> 0
    }

    private fun applyOperation(numStack: Stack<Double>, opStack: Stack<Char>) {
        if (numStack.size < 2) {
            // অসম্পূর্ণ এক্সপ্রেশন (যেমন: শুধু "5+") — crash না করে গণনা থামানো
            opStack.clear()
            return
        }
        val b = numStack.pop()
        val a = numStack.pop()
        when (opStack.pop()) {
            '+' -> numStack.push(a + b)
            '-' -> numStack.push(a - b)
            '*' -> numStack.push(a * b)
            '/' -> numStack.push(a / b)
        }
    }
}
