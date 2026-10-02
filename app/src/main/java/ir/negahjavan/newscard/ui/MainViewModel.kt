package ir.negahjavan.newscard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.network.SmartNewsService
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

    fun analyze(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            _analysis.value = service.analyze(text)
            _loading.value = false
        }
    }

    fun clear() {
        _analysis.value = null
    }
}
