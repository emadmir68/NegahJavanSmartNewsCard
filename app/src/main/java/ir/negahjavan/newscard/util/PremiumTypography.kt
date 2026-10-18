package ir.negahjavan.newscard.util

import android.content.Context
import android.graphics.Typeface
import ir.negahjavan.newscard.R

/** Bundled, offline Persian fonts shared by measurement and bitmap export. */
object PremiumTypography {
    lateinit var headline: Typeface
        private set
    lateinit var body: Typeface
        private set
    lateinit var bold: Typeface
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (::body.isInitialized) return
        headline = context.resources.getFont(R.font.lalezar)
        bold = context.resources.getFont(R.font.vazirmatn_bold)
        body = context.resources.getFont(R.font.vazirmatn_regular)
    }
}
