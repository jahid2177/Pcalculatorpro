package com.calculator.pcalculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class StandardCalculatorActivity : AppCompatActivity() {

    private lateinit var tvInput: TextView
    private lateinit var tvResult: TextView

    private var currentInput = StringBuilder()
    private var isResultShown = false

    private val decimalFormat = DecimalFormat("#.##########")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_standard_calculator)

        tvInput = findViewById(R.id.tvInput)
        tvResult = findViewById(R.id.tvResult)

        setupNumberButtons()
        setupOperatorButtons()
        setupSpecialButtons()
        
        // প্রথমবার ডিসপ্লে আপডেট
        updateDisplay()
    }

    private fun setupNumberButtons() {
        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        )
        
        numberButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onDigitClick(it) }
        }
    }

    private fun setupOperatorButtons() {
        val opButtons = listOf(
            R.id.btnPlus, R.id.btnMinus, R.id.btnMul, 
            R.id.btnDiv, R.id.btnPercent, R.id.btnBracket
        )
        
        opButtons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onOperatorClick(it) }
        }
    }

    private fun setupSpecialButtons() {
        findViewById<Button>(R.id.btnEqual).setOnClickListener { onEqualClick() }
        findViewById<Button>(R.id.btnAc).setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { onBackClick() }
    }

    private fun onDigitClick(view: View) {
        val button = view as Button
        val digit = button.text.toString()

        if (isResultShown) {
            currentInput.clear()
            isResultShown = false
        }

        // ডট লজিক: একটা নাম্বারে শুধু একবার ডট
        if (digit == "." && currentInput.endsWith(".")) return
        if (digit == "." && currentInput.isNotEmpty()) {
            val lastPart = currentInput.split(Regex("[+\\-×÷(]")).last()
            if (lastPart.contains(".")) return
        }

        currentInput.append(digit)
        updateDisplay()
    }

    private fun onOperatorClick(view: View) {
        val button = view as Button
        var op = button.text.toString()

        if (isResultShown) {
            isResultShown = false
        }

        when (op) {
            "( )" -> handleBracket()
            "%" -> handlePercent()
            else -> handleBinaryOrSignOperator(op)
        }
        updateDisplay()
    }

    // ── ঋণাত্মক সংখ্যা (negative number) সাপোর্টের জন্য fix ─────────────────
    // আগে এই লজিকে অপারেটরের পরে "-" চাপলে আগের অপারেটরটাই মুছে যেত,
    // ফলে "5×-3" টাইপ করলে হয়ে যেত "5-3" — গুণটাই হারিয়ে যেত।
    // এখন "-" কে অপারেটরের পরে আলাদাভাবে (sign হিসেবে) রাখা হচ্ছে।
    private fun handleBinaryOrSignOperator(op: String) {
        if (currentInput.isEmpty()) {
            // এক্সপ্রেশনের শুরুতে শুধু "-" অনুমোদিত (নেগেটিভ নাম্বার শুরু করার জন্য)
            if (op == "-") currentInput.append(op)
            return
        }

        val last = currentInput.last()

        when {
            // "(" এর পরে শুধু "-" বসতে পারবে (যেমন: "(-5+3)")
            last == '(' -> {
                if (op == "-") currentInput.append(op)
            }

            // আগে থেকেই একটা "অপারেটর-" (যেমন "×-") বসে আছে আর নতুন কিছু চাপা হলো
            currentInput.length >= 2 &&
            last == '-' &&
            "+-×÷(".contains(currentInput[currentInput.length - 2]) -> {
                if (op != "-") {
                    // পুরোনো "অপারেটর-" সরিয়ে নতুন অপারেটর বসানো
                    currentInput.deleteCharAt(currentInput.length - 1)
                    currentInput.deleteCharAt(currentInput.length - 1)
                    currentInput.append(op)
                }
                // আবার "-" চাপলে কিছু হবে না (একাধিক sign স্ট্যাক হওয়া আটকানো)
            }

            // শেষ ক্যারেক্টার একটা সাধারণ অপারেটর
            "+-×÷".contains(last) -> {
                if (op == "-") {
                    // পরের নাম্বারের জন্য sign হিসেবে যুক্ত হবে, যেমন "5×" → "5×-"
                    currentInput.append(op)
                } else {
                    // অপারেটর রিপ্লেস (যেমন "5+" → "5×")
                    currentInput.deleteCharAt(currentInput.length - 1)
                    currentInput.append(op)
                }
            }

            // স্বাভাবিক কেস: শেষে সংখ্যা, ডট বা ")"
            else -> currentInput.append(op)
        }
    }

    private fun handleBracket() {
        if (currentInput.isEmpty() || 
            "+-×÷(".contains(currentInput.last().toString())) {
            currentInput.append("(")
        } else {
            // ব্র্যাকেট ব্যালেন্স চেক
            if (currentInput.count { it == '(' } > currentInput.count { it == ')' }) {
                currentInput.append(")")
            } else {
                currentInput.append("(")
            }
        }
    }

    private fun handlePercent() {
        if (currentInput.isNotEmpty() && 
            !"+-×÷".contains(currentInput.last())) {
            currentInput.append("%")
        }
    }

    private fun onEqualClick() {
        if (currentInput.isEmpty()) return

        try {
            val result = ExpressionEvaluator.evaluate(currentInput.toString())
            tvResult.text = formatResult(result)
            isResultShown = true
        } catch (e: Exception) {
            tvResult.text = "Error"
            isResultShown = true
        }
    }

    private fun onClearClick() {
        currentInput.clear()
        tvResult.text = "0"
        updateDisplay()
    }

    private fun onBackClick() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        tvInput.text = currentInput.toString().ifEmpty { "0" }

        // রিয়েল-টাইম প্রিভিউ
        if (!isResultShown && currentInput.isNotEmpty()) {
            try {
                val preview = ExpressionEvaluator.evaluate(currentInput.toString())
                tvResult.text = formatResult(preview)
            } catch (e: Exception) {
                tvResult.text = ""
            }
        }
    }

    private fun formatResult(result: Double): String {
        return if (result % 1 == 0.0) {
            result.toLong().toString()
        } else {
            decimalFormat.format(result)
        }
    }
}