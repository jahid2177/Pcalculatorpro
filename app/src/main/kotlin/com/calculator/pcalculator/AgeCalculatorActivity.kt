package com.calculator.pcalculator

import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AgeCalculatorActivity : AppCompatActivity() {

    private lateinit var etDob: TextInputEditText
    private lateinit var etToday: TextInputEditText
    private lateinit var tvYears: TextView
    private lateinit var tvMonths: TextView
    private lateinit var tvDays: TextView
    private lateinit var tvZodiac: TextView
    private lateinit var tvNextBday: TextView
    private lateinit var tvLifeSummary: TextView
    private lateinit var resultCardLayout: View
    private lateinit var btnCalculateAge: MaterialButton

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_age)

        bindViews()
        setupInitialState()
        setupListeners()
    }

    private fun bindViews() {
        etDob            = findViewById(R.id.etDob)
        etToday          = findViewById(R.id.etToday)
        tvYears          = findViewById(R.id.tvYears)
        tvMonths         = findViewById(R.id.tvMonths)
        tvDays           = findViewById(R.id.tvDays)
        tvZodiac         = findViewById(R.id.tvZodiac)
        tvNextBday       = findViewById(R.id.tvNextBday)
        tvLifeSummary    = findViewById(R.id.tvLifeSummary)
        resultCardLayout = findViewById(R.id.resultCardLayout)
        btnCalculateAge  = findViewById(R.id.btnCalculateAge)
    }

    private fun setupInitialState() {
        etToday.setText(dateFormat.format(Date()))
        resultCardLayout.visibility = View.GONE
        resultCardLayout.alpha = 0f
    }

    private fun setupListeners() {
        etDob.setOnClickListener   { showDatePicker(etDob, maxDate = Date().time) }
        etToday.setOnClickListener { showDatePicker(etToday) }

        btnCalculateAge.setOnClickListener {
            animateButton()
            calculateAge()
        }
    }

    // ✅ UPDATED DATE PICKER (FULL FIX)
    private fun showDatePicker(editText: TextInputEditText, maxDate: Long? = null) {

        val year  = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day   = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            this,
            R.style.PremiumDatePicker,
            { _, y, m, d ->
                editText.setText(String.format("%02d / %02d / %04d", d, m + 1, y))
            },
            year, month, day
        )

        maxDate?.let { dialog.datePicker.maxDate = it }

        // 🔥 IMPORTANT FIX (Button color)
        dialog.setOnShowListener {

            // OK button → BLACK
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(Color.BLACK)

            // Cancel button → DARK GRAY
            dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.DKGRAY)
        }

        dialog.show()
    }

    private fun calculateAge() {

        val dobStr   = etDob.text.toString().trim()
        val todayStr = etToday.text.toString().trim()

        if (dobStr.isEmpty()) {
            showToast("⚠️ Please select your date of birth")
            shakeView(etDob)
            return
        }

        runCatching {

            val dobDate   = dateFormat.parse(dobStr)!!
            val todayDate = dateFormat.parse(todayStr)!!

            if (dobDate.after(todayDate)) {
                showToast("⚠️ Birth date cannot be in the future")
                shakeView(etDob)
                return
            }

            val start = Calendar.getInstance().apply { time = dobDate }
            val end   = Calendar.getInstance().apply { time = todayDate }

            var years  = end.get(Calendar.YEAR) - start.get(Calendar.YEAR)
            var months = end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
            var days   = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH)

            if (days < 0) {
                months--
                val prevMonth = (end.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            if (months < 0) { years--; months += 12 }

            val nextBday = Calendar.getInstance().apply {
                time = dobDate
                set(Calendar.YEAR, end.get(Calendar.YEAR))
                if (!after(end)) add(Calendar.YEAR, 1)
            }

            val diffMs = nextBday.timeInMillis - end.timeInMillis
            val bDayDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()

            val isTodayBirthday = (bDayDays == 0)

            val totalDays  = TimeUnit.MILLISECONDS.toDays(todayDate.time - dobDate.time)
            val totalWeeks = totalDays / 7
            val totalHours = totalDays * 24
            val totalMins  = totalHours * 60

            val zodiac = getZodiacSign(
                start.get(Calendar.DAY_OF_MONTH),
                start.get(Calendar.MONTH) + 1
            )

            tvYears.text  = "%02d".format(years)
            tvMonths.text = "%02d".format(months)
            tvDays.text   = "%02d".format(days)

            tvZodiac.text = zodiac

            tvNextBday.text = if (isTodayBirthday) {
                "🎂 Happy Birthday!"
            } else {
                "$bDayDays days remaining"
            }

            tvLifeSummary.text =
                "⏱ $totalWeeks weeks\n📅 $totalDays days\n🕐 $totalHours hours\n⚡ $totalMins minutes"

            showResultCard()

        }.onFailure {
            showToast("Something went wrong")
        }
    }

    private fun getZodiacSign(day: Int, month: Int): String = when (month) {
        1  -> if (day < 20) "Capricorn ♑" else "Aquarius ♒"
        2  -> if (day < 19) "Aquarius ♒" else "Pisces ♓"
        3  -> if (day < 21) "Pisces ♓" else "Aries ♈"
        4  -> if (day < 20) "Aries ♈" else "Taurus ♉"
        5  -> if (day < 21) "Taurus ♉" else "Gemini ♊"
        6  -> if (day < 21) "Gemini ♊" else "Cancer ♋"
        7  -> if (day < 23) "Cancer ♋" else "Leo ♌"
        8  -> if (day < 23) "Leo ♌" else "Virgo ♍"
        9  -> if (day < 23) "Virgo ♍" else "Libra ♎"
        10 -> if (day < 23) "Libra ♎" else "Scorpio ♏"
        11 -> if (day < 22) "Scorpio ♏" else "Sagittarius ♐"
        12 -> if (day < 22) "Sagittarius ♐" else "Capricorn ♑"
        else -> "Unknown"
    }

    private fun showResultCard() {
        resultCardLayout.visibility = View.VISIBLE

        val alpha = ObjectAnimator.ofFloat(resultCardLayout, "alpha", 0f, 1f)
        val slide = ObjectAnimator.ofFloat(resultCardLayout, "translationY", 70f, 0f)

        AnimatorSet().apply {
            duration = 500
            playTogether(alpha, slide)
            start()
        }
    }

    private fun animateButton() {
        ObjectAnimator.ofFloat(btnCalculateAge, "scaleX", 1f, 0.95f, 1f).apply {
            duration = 200
            start()
        }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, -10f, 10f, 0f).apply {
            duration = 300
            start()
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}