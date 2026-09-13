package com.calculator.pcalculator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat
import kotlin.math.pow

class EmiCalculatorActivity : AppCompatActivity() {

    private lateinit var etPrincipal: TextInputEditText
    private lateinit var etRate: TextInputEditText
    private lateinit var etTenure: TextInputEditText
    private lateinit var rbYears: RadioButton
    
    private lateinit var tvEmiResult: TextView
    private lateinit var tvTotalInterest: TextView
    private lateinit var tvTotalAmount: TextView
    
    // নতুন কোড: চার্ট বাটনের জন্য ভেরিয়েবল
    private lateinit var btnChart: Button

    // নতুন কোড: চার্টে ডেটা পাঠানোর জন্য ভেরিয়েবল
    private var calcPrincipal = 0.0
    private var calcRate = 0.0
    private var calcMonths = 0
    private var calcEmi = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emi)

        // View Binding
        etPrincipal = findViewById(R.id.etPrincipal)
        etRate = findViewById(R.id.etRate)
        etTenure = findViewById(R.id.etTenure)
        rbYears = findViewById(R.id.rbYears)
        
        tvEmiResult = findViewById(R.id.tvEmiResult)
        tvTotalInterest = findViewById(R.id.tvTotalInterest)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        
        // নতুন কোড: চার্ট বাটন বাইন্ডিং
        btnChart = findViewById(R.id.btnChart)
        btnChart.visibility = View.GONE // শুরুতে বাটন লুকানো থাকবে

        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val btnReset = findViewById<Button>(R.id.btnReset)

        btnCalculate.setOnClickListener {
            calculateEmi()
            hideKeyboard()
        }

        btnReset.setOnClickListener {
            resetFields()
        }

        // নতুন কোড: চার্ট বাটনে ক্লিক করলে চার্ট পেজে নিয়ে যাবে
        btnChart.setOnClickListener {
            val intent = Intent(this, AmortizationActivity::class.java)
            intent.putExtra("PRINCIPAL", calcPrincipal)
            intent.putExtra("RATE", calcRate)
            intent.putExtra("MONTHS", calcMonths)
            intent.putExtra("EMI", calcEmi)
            startActivity(intent)
        }
    }

    private fun calculateEmi() {
        val principalStr = etPrincipal.text.toString()
        val rateStr = etRate.text.toString()
        val tenureStr = etTenure.text.toString()

        if (principalStr.isEmpty() || rateStr.isEmpty() || tenureStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // ── Safe parsing — toDoubleOrNull() দিয়ে crash এড়ানো ─────────────
        // (যেমন: কেউ যদি শুধু "." লিখে Calculate চাপে, raw toDouble() crash করত)
        val principal = principalStr.toDoubleOrNull()
        val rate = rateStr.toDoubleOrNull()
        var tenure = tenureStr.toDoubleOrNull()

        if (principal == null || rate == null || tenure == null || tenure <= 0) {
            Toast.makeText(this, "সঠিক সংখ্যা লিখুন", Toast.LENGTH_SHORT).show()
            return
        }

        // যদি বছর সিলেক্ট করা থাকে, সেটাকে মাসে কনভার্ট করা
        if (rbYears.isChecked) {
            tenure *= 12
        }

        // EMI সূত্র: [P x R x (1+R)^N]/[(1+R)^N-1]
        val r = rate / 12 / 100 // মাসিক সুদের হার
        
        val emi = if (r > 0) {
            (principal * r * (1 + r).pow(tenure)) / ((1 + r).pow(tenure) - 1)
        } else {
            principal / tenure // যদি সুদ ০% হয়
        }

        val totalPayment = emi * tenure
        val totalInterest = totalPayment - principal

        // রেজাল্ট ফরম্যাট করা (কমা সহ, যেমন: 10,000.00)
        val df = DecimalFormat("#,##0.00")

        tvEmiResult.text = df.format(emi)
        tvTotalInterest.text = df.format(totalInterest)
        tvTotalAmount.text = df.format(totalPayment)

        // নতুন কোড: সফল ক্যালকুলেশনের পর ডেটা সেভ করা এবং বাটন দেখানো
        calcPrincipal = principal
        calcRate = rate
        calcMonths = tenure.toInt()
        calcEmi = emi
        
        btnChart.visibility = View.VISIBLE
    }

    private fun resetFields() {
        etPrincipal.text?.clear()
        etRate.text?.clear()
        etTenure.text?.clear()
        rbYears.isChecked = true
        
        tvEmiResult.text = "0.00"
        tvTotalInterest.text = "0.00"
        tvTotalAmount.text = "0.00"

        // নতুন কোড: রিসেট করলে বাটন আবার লুকিয়ে ফেলা হবে
        btnChart.visibility = View.GONE
    }

    // কীবোর্ড লুকানোর ফাংশন (UX ভালো করার জন্য)
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
