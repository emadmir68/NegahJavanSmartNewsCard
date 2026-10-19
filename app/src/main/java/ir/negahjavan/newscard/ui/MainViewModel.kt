package ir.negahjavan.newscard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.negahjavan.newscard.model.SmartAnalysis
import ir.negahjavan.newscard.network.SmartNewsService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    private var analysisJob: Job? = null
    private var requestVersion = 0L

    fun analyze(text: String) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            clear()
            return
        }

        val request = ++requestVersion
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _loading.value = true
            try {
                val result = service.analyze(normalizedText)
                // A cancelled or stale request must never replace the newest result.
                if (request == requestVersion) {
                    _analysis.value = result
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the UI usable if analysis fails; the next request can retry.
                if (request == requestVersion) {
                    _analysis.value = null
                }
            } finally {
                if (request == requestVersion) {
                    _loading.value = false
                }
            }
        }
    }

    fun clear() {
        requestVersion++
        analysisJob?.cancel()
        analysisJob = null
        _analysis.value = null
        _loading.value = false
    }
}
