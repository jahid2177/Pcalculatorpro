package com.calculator.pcalculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import kotlin.math.roundToInt

class PreForeignCalcActivity : AppCompatActivity() {

    private var isDetailsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_foreign_calc)

        val rgOption = findViewById<RadioGroup>(R.id.rgOption)
        val rbFDR = findViewById<RadioButton>(R.id.rbFDR)
        val rbSB = findViewById<RadioButton>(R.id.rbSB)
        
        val tilFdrRate = findViewById<TextInputLayout>(R.id.tilFdrRate)
        val tilTaxRate = findViewById<TextInputLayout>(R.id.tilTaxRate)

        val etLoanAmount = findViewById<TextInputEditText>(R.id.etLoanAmount)
        val etLoanRate = findViewById<TextInputEditText>(R.id.etLoanRate)
        val etLoanTenure = findViewById<TextInputEditText>(R.id.etLoanTenure)
        val etFdrRate = findViewById<TextInputEditText>(R.id.etFdrRate)
        val etTaxRate = findViewById<TextInputEditText>(R.id.etTaxRate)
        val etProcessingFee = findViewById<TextInputEditText>(R.id.etProcessingFee)
        
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val tvShowDetails = findViewById<TextView>(R.id.tvShowDetails)
        
        val resultLayout = findViewById<LinearLayout>(R.id.resultLayout)
        val detailsLayout = findViewById<LinearLayout>(R.id.detailsLayout)
        
        val tvMargin = findViewById<TextView>(R.id.tvMargin)
        val tvTotalCost = findViewById<TextView>(R.id.tvTotalCost)
        val tvLoanInterest = findViewById<TextView>(R.id.tvLoanInterest)
        val tvFdrNetInterest = findViewById<TextView>(R.id.tvFdrNetInterest)
        val tvOtherCost = findViewById<TextView>(R.id.tvOtherCost)

        val formatter = DecimalFormat("#,##,##0.00")

        // 🟢 Toggle UI between FDR and SB
        rgOption.setOnCheckedChangeListener { _, checkedId ->
            val layoutParams = tilTaxRate.layoutParams as LinearLayout.LayoutParams
            if (checkedId == R.id.rbSB) {
                tilFdrRate.visibility = View.GONE
                layoutParams.marginStart = 0 // Remove margin so it fills the space perfectly
            } else {
                tilFdrRate.visibility = View.VISIBLE
                layoutParams.marginStart = resources.displayMetrics.density.toInt() * 6 // 6dp margin back
            }
            tilTaxRate.layoutParams = layoutParams
        }

        btnCalculate.setOnClickListener {
            try {
                val amount = etLoanAmount.text.toString().toDoubleOrNull() ?: 0.0
                val loanRate = etLoanRate.text.toString().toDoubleOrNull() ?: 0.0
                val tenure = etLoanTenure.text.toString().toDoubleOrNull() ?: 0.0
                val taxRate = etTaxRate.text.toString().toDoubleOrNull() ?: 0.0
                val processingFee = etProcessingFee.text.toString().toDoubleOrNull() ?: 0.0

                if (amount <= 0 || tenure <= 0) {
                    Toast.makeText(this, "Please enter valid amount and tenure", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 🟢 Determine Deposit Rate based on selection
                val isFDR = rbFDR.isChecked
                val depositRate = if (isFDR) {
                    etFdrRate.text.toString().toDoubleOrNull() ?: 0.0
                } else {
                    0.0 // Default SB Rate is considered 0.0% here. 
                        // আপনি চাইলে এখানে আপনার ব্যাংকের নির্দিষ্ট SB রেট (যেমন 3.0) বসাতে পারেন।
                }

                // ১. Loan Interest
                val loanInterest = (amount * (loanRate / 100)) / 12 * tenure

                // ২. Deposit Interest (FDR or SB)
                val grossDepositInterest = (amount * (depositRate / 100)) / 12 * tenure
                val depositTax = grossDepositInterest * (taxRate / 100)
                val netDepositInterest = grossDepositInterest - depositTax

                // ৩. Other Costs (Excise Duty + Processing Fee)
                val exciseDutySingle = calculateExciseDuty(amount)
                val totalExciseDuty = exciseDutySingle * 2
                val totalOtherCost = totalExciseDuty + processingFee

                // ৪. Results Calculation
                val margin = loanInterest - netDepositInterest
                val totalCost = (loanInterest + totalOtherCost) - netDepositInterest // Overall Cost

                // Update UI Texts
                tvMargin.text = "৳${formatter.format(margin.roundToInt())}"
                tvTotalCost.text = "৳${formatter.format(totalCost.roundToInt())}"
                
                tvLoanInterest.text = "৳${formatter.format(loanInterest.roundToInt())}"
                tvFdrNetInterest.text = "৳${formatter.format(netDepositInterest.roundToInt())}"
                tvOtherCost.text = "৳${formatter.format(totalOtherCost.roundToInt())} (Excise: ${formatter.format(totalExciseDuty)})"

                // Show basic results and "Show Details" button
                resultLayout.visibility = View.VISIBLE
                tvShowDetails.visibility = View.VISIBLE
                
                // Reset details visibility if new calculation is made
                detailsLayout.visibility = View.GONE
                tvShowDetails.text = "SHOW DETAILS"
                isDetailsVisible = false

            } catch (e: Exception) {
                Toast.makeText(this, "Error in calculation", Toast.LENGTH_SHORT).show()
            }
        }

        // Show Details Button Click Event
        tvShowDetails.setOnClickListener {
            isDetailsVisible = !isDetailsVisible
            if (isDetailsVisible) {
                detailsLayout.visibility = View.VISIBLE
                tvShowDetails.text = "HIDE DETAILS"
            } else {
                detailsLayout.visibility = View.GONE
                tvShowDetails.text = "SHOW DETAILS"
            }
        }
    }

    private fun calculateExciseDuty(balance: Double): Double {
        return when {
            balance <= 100000 -> 0.0
            balance <= 500000 -> 150.0
            balance <= 1000000 -> 500.0
            balance <= 5000000 -> 3000.0
            balance <= 10000000 -> 5000.0
            balance <= 20000000 -> 10000.0
            balance <= 50000000 -> 20000.0
            else -> 50000.0
        }
    }
}
