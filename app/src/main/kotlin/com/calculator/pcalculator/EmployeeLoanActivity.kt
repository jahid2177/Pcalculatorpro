package com.calculator.pcalculator

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat

class EmployeeLoanActivity : AppCompatActivity() {

    private lateinit var etGross: TextInputEditText
    private lateinit var etTakeHome: TextInputEditText
    private lateinit var etSpouse: TextInputEditText
    private lateinit var etOtherLoan: TextInputEditText
    private lateinit var etDeductionAuto: TextInputEditText
    private lateinit var toggleTenure: MaterialButtonToggleGroup

    private lateinit var tvEligibleLimit: TextView
    private lateinit var tvMonthlyEmi: TextView
    private lateinit var tvDeductionAfterTakeHome: TextView
    private lateinit var resultLayout: LinearLayout

    // Java code অনুযায়ী কিস্তির ফ্যাক্টরসমূহ
    private val installmentFactors = mapOf(
        R.id.btn3Y to 3110.0,
        R.id.btn4Y to 2410.0,
        R.id.btn5Y to 1980.0,
        R.id.btn6Y to 1680.0,
        R.id.btn7Y to 1480.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_loan)

        // View Initialization
        etGross           = findViewById(R.id.etGrossSalary)
        etTakeHome        = findViewById(R.id.etTakeHome)
        etSpouse          = findViewById(R.id.etSpouseSalary)
        etOtherLoan       = findViewById(R.id.etOtherLoanInstallment)
        etDeductionAuto   = findViewById(R.id.etDeductionAuto)
        toggleTenure      = findViewById(R.id.toggleTenure)

        tvEligibleLimit          = findViewById(R.id.tvEligibleLimit)
        tvMonthlyEmi             = findViewById(R.id.tvMonthlyEmi)
        tvDeductionAfterTakeHome = findViewById(R.id.tvDeductionAfterTakeHome)
        resultLayout             = findViewById(R.id.resultLayout)

        // Real-time calculation using TextWatcher
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateLoan()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etGross.addTextChangedListener(watcher)
        etTakeHome.addTextChangedListener(watcher)
        etSpouse.addTextChangedListener(watcher)
        etOtherLoan.addTextChangedListener(watcher)

        toggleTenure.addOnButtonCheckedListener { _, _, _ -> calculateLoan() }
    }

    private fun calculateLoan() {
        val gross     = etGross.text.toString().toDoubleOrNull() ?: 0.0
        val takeHome  = etTakeHome.text.toString().toDoubleOrNull() ?: 0.0
        val spouse    = etSpouse.text.toString().toDoubleOrNull() ?: 0.0
        val otherLoan = etOtherLoan.text.toString().toDoubleOrNull() ?: 0.0

        // Java সূত্র ১: Calculations (Total values)
        val totalGross    = gross + spouse
        val totalTakeHome = takeHome + spouse

        // Java সূত্র ২: Present Deduction (Total Gross - Total Take Home)
        val deduction = totalGross - totalTakeHome
        etDeductionAuto.setText(deduction.toInt().toString())

        // Java সূত্র ৩: 50% of gross salary
        val halfGross = totalGross * 0.50

        // Java সূত্র ৪: Maximum installment size (Half Gross - Deduction)
        val maxInstallmentSize = halfGross - deduction

        // কিস্তির হার (Factor) নির্ধারণ
        val perLac = installmentFactors[toggleTenure.checkedButtonId] ?: 1980.0

        if (maxInstallmentSize > 0 && gross > 0) {
            resultLayout.visibility = View.VISIBLE

            // Java সূত্র ৫: Eligible limit raw (Max Inst / Per Lac * 100,000)
            val eligibleLimitRaw = (maxInstallmentSize / perLac) * 100000

            // Java সূত্র ৬: Capping at 1,000,000 and rounding last 3 digits to zero
            var finalLimit = Math.min(eligibleLimitRaw, 1000000.0)
            finalLimit = Math.floor(finalLimit / 1000) * 1000

            // Java সূত্র ৭: Monthly Installment (Employee Loan EMI)
            val rawEmi          = (finalLimit / 100000) * perLac
            val employeeLoanEmi = Math.round(rawEmi).toDouble()

            // নতুন সূত্র: Deduction After Take Home Salary
            val deductionAfterTakeHome = takeHome - otherLoan - employeeLoanEmi

            // রেজাল্ট প্রদর্শন
            val df = DecimalFormat("৳ #,##,###")

            // Eligible limit — label text removed (shown as card caption already)
            tvEligibleLimit.text          = df.format(finalLimit)
            tvMonthlyEmi.text             = df.format(employeeLoanEmi)
            tvDeductionAfterTakeHome.text = df.format(deductionAfterTakeHome)

        } else {
            resultLayout.visibility = View.GONE
        }
    }
}
