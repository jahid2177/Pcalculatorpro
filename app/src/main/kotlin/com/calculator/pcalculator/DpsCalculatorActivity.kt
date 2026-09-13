package com.calculator.pcalculator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat
import kotlin.math.pow

class DpsCalculatorActivity : AppCompatActivity() {

    private lateinit var etAmount    : TextInputEditText
    private lateinit var etRate      : TextInputEditText
    private lateinit var rgTenure    : RadioGroup
    private lateinit var tvMaturity  : TextView
    private lateinit var tvDeposit   : TextView
    private lateinit var tvInterest  : TextView
    private lateinit var tvAppliedRate: TextView
    private lateinit var cardResult  : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dps)

        etAmount      = findViewById(R.id.etMonthlyAmount)
        etRate        = findViewById(R.id.etCustomRate)
        rgTenure      = findViewById(R.id.rgTenure)
        tvMaturity    = findViewById(R.id.tvMaturityAmount)
        tvDeposit     = findViewById(R.id.tvTotalDeposit)
        tvInterest    = findViewById(R.id.tvTotalInterest)
        tvAppliedRate = findViewById(R.id.tvAppliedRate)
        cardResult    = findViewById(R.id.cardDpsResult)

        findViewById<Button>(R.id.btnCalculateDps).setOnClickListener { calculateDPS() }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewPdf)
            .setOnClickListener { openDpsPdf() }
    }

    private fun calculateDPS() {
        // ── Monthly amount ───────────────────────────────────────────────
        val p = etAmount.text.toString().trim().toDoubleOrNull()
        if (p == null || p <= 0) {
            Toast.makeText(this, "সঠিক Monthly amount লিখুন", Toast.LENGTH_SHORT).show()
            return
        }

        // ── Interest rate ────────────────────────────────────────────────
        val annualRate = etRate.text.toString().trim().toDoubleOrNull()
        if (annualRate == null || annualRate <= 0 || annualRate > 100) {
            Toast.makeText(this, "সঠিক Interest rate লিখুন (0–100%)", Toast.LENGTH_SHORT).show()
            return
        }

        // ── Tenure ───────────────────────────────────────────────────────
        val years = when (rgTenure.checkedRadioButtonId) {
            R.id.rb3  -> 3
            R.id.rb5  -> 5
            R.id.rb7  -> 7
            R.id.rb10 -> 10
            else      -> 0
        }
        if (years == 0) {
            Toast.makeText(this, "Tenure বেছে নিন", Toast.LENGTH_SHORT).show()
            return
        }

        // ── DPS Maturity — Annuity Due formula ───────────────────────────
        // M = P × [{(1 + r)^n − 1} / r] × (1 + r)
        val r              = annualRate / 100.0 / 12.0
        val n              = (years * 12).toDouble()
        val maturityAmount = p * (((1 + r).pow(n) - 1) / r) * (1 + r)
        val totalDeposit   = p * n
        val totalInterest  = maturityAmount - totalDeposit

        // ── Show result ──────────────────────────────────────────────────
        val df        = DecimalFormat("৳#,##0.00")
        val rateLabel = DecimalFormat("#,##0.##").format(annualRate)

        tvAppliedRate.text = "Rate: $rateLabel% p.a.  |  $years Years"
        tvMaturity.text    = df.format(maturityAmount)
        tvDeposit.text     = df.format(totalDeposit)
        tvInterest.text    = df.format(totalInterest)

        cardResult.visibility = View.VISIBLE
    }

    private fun openDpsPdf() {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_ASSET_NAME, "dps.pdf")
            putExtra(PdfViewerActivity.EXTRA_TITLE, "DPS Chart")
        }
        startActivity(intent)
    }
}
