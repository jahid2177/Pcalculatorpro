package com.calculator.pcalculator

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ক্যালকুলেটর ও তথ্য কার্ড সেটিংস
        setupCard(R.id.card1, "Standard\nCalculator", R.drawable.ic_calculate, StandardCalculatorActivity::class.java)
        setupCard(R.id.card2, "EMI\nCalculator", R.drawable.ic_calculate, EmiCalculatorActivity::class.java)
        setupCard(R.id.card3, "AGE\nCalculator", R.drawable.ic_calculate, AgeCalculatorActivity::class.java)
        setupCard(R.id.card4, "DBR\nCalculator", R.drawable.ic_finance, DbrCalculatorActivity::class.java)
        setupCard(R.id.card5, "DPS\nCalculator", R.drawable.ic_finance, DpsCalculatorActivity::class.java)
        setupCard(R.id.card6, "Employee\nLoan", R.drawable.ic_loan, EmployeeLoanActivity::class.java)
        setupCard(R.id.card7, "FDR\nCalculator", R.drawable.ic_calculate, FdrCalculatorActivity::class.java)
        setupCard(R.id.card8, "Debit\nCard", R.drawable.ic_debit_card, CardInfoActivity::class.java)
        setupCard(R.id.card9, "Credit\nCard", R.drawable.ic_credit_card, CreditCardInfoActivity::class.java)
        setupCard(R.id.card10, "Pre Foreign\nLoan", R.drawable.ic_foreign, PreForeignCalcActivity::class.java)
        setupCard(R.id.card11, "Islamic\nTools", R.drawable.ic_islamic, null) // Islamic - Coming Soon
        setupCard(R.id.card12, "About\nApp", R.drawable.ic_info, AboutActivity::class.java) // About App
    }

    private fun setupCard(cardId: Int, title: String, iconRes: Int, activityClass: Class<*>?) {
        val card = findViewById<MaterialCardView>(cardId)
        val titleView = card.findViewById<TextView>(R.id.cardTitle)
        val iconView = card.findViewById<ImageView>(R.id.cardIcon)

        titleView.text = title
        iconView.setImageResource(iconRes)

        // কার্ডে টাচ করলে স্কেল অ্যানিমেশন এবং ক্লিক লজিক
        card.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (event.action == MotionEvent.ACTION_UP) {
                        if (activityClass != null) {
                            // যদি অ্যাক্টিভিটি থাকে তবে সেটি ওপেন হবে
                            startActivity(Intent(this, activityClass))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        } else {
                            // ইসলামিক ফিচারের জন্য "Coming Soon" টোস্ট
                            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            true
        }
    }
}
