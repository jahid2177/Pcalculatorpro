package com.calculator.pcalculator

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TableLayout
import java.text.DecimalFormat
import kotlin.math.pow

class AmortizationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_amortization)

        // Get data passed from EmiCalculatorActivity
        val principal = intent.getDoubleExtra("PRINCIPAL", 0.0)
        val rate = intent.getDoubleExtra("RATE", 0.0)
        val months = intent.getIntExtra("MONTHS", 0)
        val emi = intent.getDoubleExtra("EMI", 0.0)

        setupHeader(principal, rate, months, emi)
        generateChart(principal, rate, months, emi)
    }

    private fun setupHeader(principal: Double, rate: Double, months: Int, emi: Double) {
        val df = DecimalFormat("৳#,##0.00")
        
        val tvHeaderInfo = findViewById<TextView>(R.id.tvHeaderInfo)
        val tvHeaderEmi = findViewById<TextView>(R.id.tvHeaderEmi)

        tvHeaderInfo.text = "Loan: ${df.format(principal)} @ $rate% p.a. for $months Months"
        tvHeaderEmi.text = "EMI: ${df.format(emi)}"
    }

    private fun generateChart(principal: Double, rate: Double, months: Int, emi: Double) {
        val tableLayout = findViewById<TableLayout>(R.id.tableLayout)
        val df = DecimalFormat("৳#,##0") // পয়সা বাদে দেখানোর জন্য (Clean look), পয়সা চাইলে "৳#,##0.00" দিন

        var balance = principal
        val monthlyRate = rate / 12 / 100

        for (i in 1..months) {
            val interest = balance * monthlyRate
            val principalComponent = emi - interest
            balance -= principalComponent

            if (balance < 0) balance = 0.0 // শেষ মাসে নেগেটিভ না হওয়ার জন্য

            // Create Row
            val row = TableRow(this)
            row.setPadding(0, 10, 0, 10)

            // 1. Month No
            row.addView(createTextView(i.toString(), Gravity.START))
            // 2. Interest
            row.addView(createTextView(df.format(interest), Gravity.END))
            // 3. Principal
            row.addView(createTextView(df.format(principalComponent), Gravity.END))
            // 4. Balance
            row.addView(createTextView(df.format(balance), Gravity.END))

            tableLayout.addView(row)
        }
    }

    private fun createTextView(text: String, gravity: Int): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(Color.WHITE)
        tv.textSize = 14f
        tv.gravity = gravity
        // Layout Params to distribute weight equally
        val params = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        tv.layoutParams = params
        return tv
    }
}
