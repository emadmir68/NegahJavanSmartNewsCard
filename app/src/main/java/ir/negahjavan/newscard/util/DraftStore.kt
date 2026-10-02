package ir.negahjavan.newscard.util

import android.content.Context

object DraftStore {
    private const val PREFS = "negahjavan_draft"
    private const val KEY_TEXT = "text"
    private const val KEY_IMAGE = "image_uri"

    data class Draft(
        val text: String,
        val imageUri: String?
    )

    fun load(context: Context): Draft {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Draft(
            text = prefs.getString(KEY_TEXT, "").orEmpty(),
            imageUri = prefs.getString(KEY_IMAGE, null)
        )
    }

    fun saveText(context: Context, text: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXT, text)
            .apply()
    }

    fun saveImage(context: Context, uri: String?) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (uri == null) editor.remove(KEY_IMAGE) else editor.putString(KEY_IMAGE, uri)
        editor.apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
