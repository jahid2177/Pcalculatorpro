package com.calculator.pcalculator

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class CardInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_info)

        setupCard(R.id.btnDebitCardCharges, "Debit Card\nCharges", R.drawable.ic_debit_card, "debitcard.pdf")
    }

    private fun setupCard(cardId: Int, title: String, iconRes: Int, assetName: String) {
        val card      = findViewById<MaterialCardView>(cardId)
        val titleView = card.findViewById<TextView>(R.id.cardTitle)
        val iconView  = card.findViewById<ImageView>(R.id.cardIcon)

        titleView.text = title
        iconView.setImageResource(iconRes)

        card.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (event.action == MotionEvent.ACTION_UP)
                        openInAppViewer(assetName, title)
                }
            }
            true
        }
    }

    private fun openInAppViewer(assetName: String, title: String) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_ASSET_NAME, assetName)
            putExtra(PdfViewerActivity.EXTRA_TITLE, title)
        }
        startActivity(intent)
    }
}
