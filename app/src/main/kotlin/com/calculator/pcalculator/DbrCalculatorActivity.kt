package com.calculator.pcalculator

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat

class DbrCalculatorActivity : AppCompatActivity() {

    private lateinit var etIncome           : TextInputEditText
    private lateinit var etExistingEmi      : TextInputEditText
    private lateinit var etProposedEmi      : TextInputEditText
    private lateinit var etExistingCardLimit: TextInputEditText
    private lateinit var etProposedCardLimit: TextInputEditText
    private lateinit var cardResult         : CardView
    private lateinit var tvDbrPercentage    : TextView
    private lateinit var tvStatus           : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dbr)

        etIncome            = findViewById(R.id.etIncome)
        etExistingEmi       = findViewById(R.id.etExistingEmi)
        etProposedEmi       = findViewById(R.id.etProposedEmi)
        etExistingCardLimit = findViewById(R.id.etExistingCardLimit)
        etProposedCardLimit = findViewById(R.id.etProposedCardLimit)
        cardResult          = findViewById(R.id.cardResult)
        tvDbrPercentage     = findViewById(R.id.tvDbrPercentage)
        tvStatus            = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnCalculateDbr).setOnClickListener {
            calculateDBR()
            hideKeyboard()
        }
    }

    private fun calculateDBR() {
        // ── Safe parsing — toDoubleOrNull() দিয়ে crash এড়ানো ─────────────
        val income = etIncome.text.toString().trim().toDoubleOrNull()
        if (income == null || income <= 0) {
            Toast.makeText(this, "সঠিক Monthly Income লিখুন", Toast.LENGTH_SHORT).show()
            return
        }

        val existingEmi      = etExistingEmi.text.toString().trim()
                                   .toDoubleOrNull() ?: 0.0
        val proposedEmi      = etProposedEmi.text.toString().trim()
                                   .toDoubleOrNull() ?: 0.0
        val existingCardLimit = etExistingCardLimit.text.toString().trim()
                                   .toDoubleOrNull() ?: 0.0
        val proposedCardLimit = etProposedCardLimit.text.toString().trim()
                                   .toDoubleOrNull() ?: 0.0

        // ── Calculation ───────────────────────────────────────────────────
        // Credit card limit-এর ৫% liability হিসাবে ধরা হয়
        val existingCardLiability = existingCardLimit * 0.05
        val proposedCardLiability = proposedCardLimit * 0.05
        val totalLiability = existingEmi + proposedEmi +
                             existingCardLiability + proposedCardLiability

        val dbr = (totalLiability / income) * 100

        // ── Show result ───────────────────────────────────────────────────
        val df = DecimalFormat("##.##")
        tvDbrPercentage.text = "${df.format(dbr)}%"
        cardResult.visibility = View.VISIBLE
        updateStatus(dbr)
    }

    private fun updateStatus(dbr: Double) {
        when {
            dbr <= 40 -> {
                tvStatus.text = "Excellent (Safe)"
                tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                tvStatus.setBackgroundColor(Color.parseColor("#C8E6C9"))
            }
            dbr <= 60 -> {
                tvStatus.text = "Moderate (Caution)"
                tvStatus.setTextColor(Color.parseColor("#F9A825"))
                tvStatus.setBackgroundColor(Color.parseColor("#FFF9C4"))
            }
            else -> {
                tvStatus.text = "Risky (High Burden)"
                tvStatus.setTextColor(Color.parseColor("#C62828"))
                tvStatus.setBackgroundColor(Color.parseColor("#FFCDD2"))
            }
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
