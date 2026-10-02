package ir.negahjavan.newscard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import ir.negahjavan.newscard.ui.NewsCardApp
import ir.negahjavan.newscard.ui.theme.NegahJavanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            NegahJavanTheme { NewsCardApp() }
        }
    }
}
