package com.calculator.pcalculator

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat

class FdrCalculatorActivity : AppCompatActivity() {

    private lateinit var etPrincipal: TextInputEditText
    private lateinit var etRate: TextInputEditText
    private lateinit var toggleTenure: MaterialButtonToggleGroup
    private lateinit var toggleTax: MaterialButtonToggleGroup
    private lateinit var switchExciseDuty: SwitchMaterial
    private lateinit var resultLayout: View
    private lateinit var tvNetInterest: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var rowExciseDutyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fdr)

        etPrincipal      = findViewById(R.id.etPrincipal)
        etRate           = findViewById(R.id.etInterestRate)
        toggleTenure     = findViewById(R.id.toggleTenure)
        toggleTax        = findViewById(R.id.toggleTax)
        switchExciseDuty = findViewById(R.id.switchExciseDuty)
        resultLayout     = findViewById(R.id.resultCardLayout)
        tvNetInterest    = findViewById(R.id.tvNetInterest)
        tvTotalAmount    = findViewById(R.id.tvTotalAmount)
        rowExciseDutyView = findViewById(R.id.rowExciseDuty)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { calculateFDR() }
            override fun afterTextChanged(s: Editable?) {}
        }

        etPrincipal.addTextChangedListener(watcher)
        etRate.addTextChangedListener(watcher)
        toggleTenure.addOnButtonCheckedListener { _, _, _ -> calculateFDR() }
        toggleTax.addOnButtonCheckedListener { _, _, _ -> calculateFDR() }

        // Excise Duty toggle — recalculate & show/hide row on change
        switchExciseDuty.setOnCheckedChangeListener { _, _ ->
            rowExciseDutyView.visibility = if (switchExciseDuty.isChecked) View.VISIBLE else View.GONE
            calculateFDR()
        }
    }

    private fun calculateFDR() {
        val p = etPrincipal.text.toString().toDoubleOrNull() ?: 0.0
        val r = etRate.text.toString().toDoubleOrNull() ?: 0.0

        val months = when (toggleTenure.checkedButtonId) {
            R.id.btn1m  -> 1.0
            R.id.btn3m  -> 3.0
            R.id.btn6m  -> 6.0
            R.id.btn12m -> 12.0
            else        -> 3.0
        }

        val taxRatePercent = if (toggleTax.checkedButtonId == R.id.btn15tax) 15.0 else 10.0
        val applyExcise    = switchExciseDuty.isChecked

        if (p > 0 && r > 0) {
            resultLayout.visibility = View.VISIBLE

            // 1. Gross interest  I = (P × R × T) / (100 × 12)
            val grossInterest = (p * (r / 100.0) * months) / 12.0

            // 2. Tax deduction
            val taxAmount = grossInterest * (taxRatePercent / 100.0)

            // 3. Excise duty (slab-based, applied only when switch is ON)
            val exciseDuty = if (applyExcise) calculateExciseDuty(p) else 0.0

            // 4. Net figures
            val netInterest = grossInterest - taxAmount - exciseDuty
            val monthlyAvg  = netInterest / months
            val totalAmount = p + netInterest

            updateUI(grossInterest, taxRatePercent, taxAmount, exciseDuty, monthlyAvg, netInterest, totalAmount, applyExcise)
        } else {
            resultLayout.visibility = View.GONE
        }
    }

    /**
     * Bangladesh govt. excise duty slabs on bank account balance.
     * Source: Finance Act (updated slabs).
     */
    private fun calculateExciseDuty(balance: Double): Double = when {
        balance <= 100_000.0    -> 0.0
        balance <= 500_000.0    -> 150.0
        balance <= 1_000_000.0  -> 500.0
        balance <= 5_000_000.0  -> 3_000.0
        balance <= 10_000_000.0 -> 5_000.0
        balance <= 20_000_000.0 -> 10_000.0
        balance <= 50_000_000.0 -> 20_000.0
        else                    -> 50_000.0
    }

    private fun updateUI(
        gross: Double,
        taxP: Double,
        taxA: Double,
        excise: Double,
        avg: Double,
        net: Double,
        total: Double,
        showExcise: Boolean
    ) {
        val df = DecimalFormat("৳ #,##,##0.00")

        setRowData(findViewById(R.id.rowGrossInterest), "Gross Interest",       df.format(gross))
        setRowData(findViewById(R.id.rowTax),           "Tax (${taxP.toInt()}%)", df.format(taxA))
        setRowData(findViewById(R.id.rowMonthlyAvg),    "Monthly Average",      df.format(avg))

        // Excise duty row — only update content when visible
        if (showExcise) {
            setRowData(rowExciseDutyView, "Excise Duty", df.format(excise))
        }

        tvNetInterest.text = df.format(net)
        tvTotalAmount.text = df.format(total)
    }

    private fun setRowData(view: View, label: String, value: String) {
        view.findViewById<TextView>(R.id.label).text = label
        view.findViewById<TextView>(R.id.value).text = value
    }
}
