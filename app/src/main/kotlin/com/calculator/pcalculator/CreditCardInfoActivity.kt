package com.calculator.pcalculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class CreditCardInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreditCardInfo"

        // ⚠️ এখানে আপনার server-এর PDF ফোল্ডারের URL বসান (শেষে "/" থাকা আবশ্যক)।
        // Firebase Storage হলে সেই ফাইলের public "getDownloadUrl" থেকে পাওয়া base অংশ,
        // নিজের PHP hosting হলে যেমন: "https://yourdomain.com/pdfs/"
        // এই একটা জায়গা বদলালেই নিচের সব setupCard() লাইন server থেকে PDF আনবে —
        // আলাদা করে প্রতিটা লাইন বদলাতে হবে না।
        private const val PDF_BASE_URL = "https://github.com/jahid2177/Mydemoproject/blob/main/assets/pdfs/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit_card_info)

        // কার্ড সেটআপ
        setupCard(R.id.row_consumer,              "Consumer Charges",          R.drawable.ic_credit_card, "consumer credit card charge.pdf")
        setupCard(R.id.row_corporate,             "Corporate Charges",         R.drawable.ic_credit_card, "corporate credit card charge.pdf")
        setupCard(R.id.row_emi,                   "Partner EMI",               R.drawable.ic_finance,     "emi.pdf")
        setupCard(R.id.row_discount,              "Partner Discount",          R.drawable.ic_finance,     "discount.pdf")
        setupCard(R.id.row_citizen,               "Senior Citizen",            R.drawable.ic_loan,        "citizen.pdf")
        setupCard(R.id.row_sanchay,               "Shadin Sanchay",            R.drawable.ic_loan,        "sanchay.pdf")
        setupCard(R.id.row_revised,               "Revised Interest",          R.drawable.ic_finance,     "revised.pdf")
        setupCard(R.id.row_annexure,              "Update Annexure-1",         R.drawable.ic_finance,     "Aannexure.pdf")
        setupCard(R.id.row_cib,                   "CIB Charge",                R.drawable.ic_finance,     "cibcharge.pdf")
        setupCard(R.id.row_support,               "Support Docs",              R.drawable.ic_loan,        "credit card support Doc.pdf")
        setupCard(R.id.row_allowance,             "Allowance Circular",        R.drawable.ic_loan,        "daily allowance.pdf")
        setupCard(R.id.row_supplementary,         "Supplementary Form",        R.drawable.ic_loan,        "suppli card.pdf")
        setupCard(R.id.row_credit_limit_revision, "Credit Limit Revision",     R.drawable.ic_credit_card, "Revision_of_Card_Type_Wise_Credit_Limit__2025.pdf")
        setupCard(R.id.row_platinum_lounge,       "Platinum Lounge",           R.drawable.ic_credit_card, "Platinum_Card_Lounge__Fees___Charges.pdf")
        setupCard(R.id.row_passport_endorsement,  "Passport Endorsement",      R.drawable.ic_loan,        "Passport_Endorsement_against_Pubali_Bank_Card_.pdf")
        setupCard(R.id.row_probation_employee,    "Probation Employee",        R.drawable.ic_loan,        "Newly_recruited_regular_employees_of_the_Bank_under_Probation__Provision.pdf")
        setupCard(R.id.row_manager_limit,         "Manager Limit",             R.drawable.ic_finance,     "Manager_Limit_Circular_.pdf")
        setupCard(R.id.row_card_ppg,              "Card PPG",                  R.drawable.ic_finance,     "Credit_Card_PPG.pdf")
        setupCard(R.id.row_application_form,      "Application Form",          R.drawable.ic_credit_card, "Credit_Card_Application_Form_.pdf")
        setupCard(R.id.row_ecommerce_facility,    "Ecommerce facility",        R.drawable.ic_credit_card, "International_Ecom_declaration_form.pdf")
        setupCard(R.id.row_Internet_Banking,      "Internet Banking Form",     R.drawable.ic_finance,     "Internet_Banking.pdf")
        setupCard(R.id.row_Application_Form_For_Instant_card, "Instant card Form", R.drawable.ic_finance, "Application_Form_For_Instant_Debit_Card.pdf")
        setupCard(R.id.row_Capture_Card_Return_form, "Capture Card Return Form", R.drawable.ic_finance, "Capture_Card_Return_form.pdf")
        setupCard(R.id.row_Card_Service_form,     "Card Service Form",         R.drawable.ic_finance,     "Card_Service_form.pdf")
        setupCard(R.id.row_Card_Surrender_form,   "Card Surrender Form",       R.drawable.ic_finance,     "Card_Surrender.pdf")
        setupCard(R.id.row_Dispute_Application_Form, "Dispute Application Form", R.drawable.ic_finance, "Dispute_Application_Form.pdf")
        setupCard(R.id.row_smart_emi,             "Smart EMI Form",            R.drawable.ic_finance,     "smart emi form.pdf")
        setupCard(R.id.row_lounge_facility,       "Lounge Facility",           R.drawable.ic_credit_card, "lounge facility.pdf")

        // নতুন ৭টি অপশন (assets ফোল্ডারে দেওয়া ৭টি PDF)
        setupCard(R.id.row_user_guidelines,        "User Guidelines",           R.drawable.ic_loan,        "User_Guidelines_of_Consumer_CC.pdf")
        setupCard(R.id.row_kpi_2026,               "KPI 2026",                  R.drawable.ic_finance,     "KPI_2026.pdf")
        setupCard(R.id.row_smart_emi_pi_banking,   "Smart EMI (PI Banking)",    R.drawable.ic_finance,     "Smart_EMI_PI_Banking.pdf")
        setupCard(R.id.row_ppg_v120,               "PPG Version 1.2.0",         R.drawable.ic_credit_card, "PPG_Version_1_2_0_April_2026.pdf")
        setupCard(R.id.row_pi_cc_application_circular, "PI App Application",    R.drawable.ic_credit_card, "PI_Credit_Card_Application_Circular.pdf")
        setupCard(R.id.row_revision_manager_limit, "Revision Manager Limit",    R.drawable.ic_finance,     "Rivision_Manager_Limit.pdf")
        setupCard(R.id.row_smart_emi_circular,     "Smart EMI Circular",        R.drawable.ic_finance,     "Smart_EMI.pdf")
        setupCard(R.id.row_staff_credit_limit_revision, "Staff Credit Limit Revision", R.drawable.ic_credit_card, "Revision_staff_credit_card_limit.pdf")
    }

    private fun setupCard(cardId: Int, title: String, iconRes: Int, assetName: String) {
        try {
            val card = findViewById<MaterialCardView>(cardId)
            if (card == null) {
                Log.e(TAG, "setupCard: card view NOT FOUND for id=$cardId title=\"$title\" — check that this id exists exactly once in activity_credit_card_info.xml")
                return
            }

            val titleView = card.findViewById<TextView>(R.id.cardTitle)
            val iconView = card.findViewById<ImageView>(R.id.cardIcon)

            if (titleView == null || iconView == null) {
                Log.e(TAG, "setupCard: cardTitle/cardIcon NOT FOUND inside card id=$cardId title=\"$title\" — item_dashboard_premium.xml may be wrong/stale")
                return
            }

            titleView.text = title
            iconView.setImageResource(iconRes)

            card.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        if (event.action == MotionEvent.ACTION_UP) {
                            openInAppViewer(assetName, title)
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            // একটা card-এ সমস্যা হলেও বাকি card গুলো ঠিকমতো সেটআপ হবে, পুরো স্ক্রিন ফাঁকা হবে না
            Log.e(TAG, "setupCard FAILED for id=$cardId title=\"$title\": ${e.message}", e)
        }
    }

    private fun openInAppViewer(assetName: String, title: String) {
        // assets ফোল্ডারের ফাইলনেম-ই থাকছে, শুধু server URL জুড়ে পাঠানো হচ্ছে —
        // PdfViewerActivity এই URL দেখে বুঝে নেবে যে এটা server থেকে ডাউনলোড করে দেখাতে হবে
        val fileUrl = PDF_BASE_URL + Uri.encode(assetName)
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_ASSET_NAME, fileUrl)
            putExtra(PdfViewerActivity.EXTRA_TITLE, title)
        }
        startActivity(intent)
    }
}
