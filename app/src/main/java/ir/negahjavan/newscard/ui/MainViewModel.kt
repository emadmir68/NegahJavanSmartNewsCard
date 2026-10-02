package ir.negahjavan.newscard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.network.SmartNewsService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val service = SmartNewsService()

    private val _analysis = MutableStateFlow<SmartAnalysis?>(null)
    val analysis: StateFlow<SmartAnalysis?> = _analysis.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun analyze(text: String) {
        if (text.isBlank() || _loading.value) return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _analysis.value = service.analyze(text)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _error.value = "تحلیل خبر انجام نشد. اتصال را بررسی کنید و دوباره تلاش کنید."
            } finally {
                _loading.value = false
            }
        }
    }

    fun clear() {
        _analysis.value = null
        _error.value = null
    }

    fun dismissError() {
        _error.value = null
    }
}
