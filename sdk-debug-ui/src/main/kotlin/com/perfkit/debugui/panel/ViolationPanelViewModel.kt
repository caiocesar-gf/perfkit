package com.perfkit.debugui.panel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.perfkit.core.PerfKit
import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.domain.ViolationSummary
import com.perfkit.api.usecase.ObserveViolationSummaries
import com.perfkit.api.usecase.ObserveViolations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ViolationFilter(
    val category: ViolationCategory? = null,
    val minSeverity: ViolationSeverity = ViolationSeverity.LOW,
    val threadQuery: String = "",
)

data class ViolationPanelUiState(
    val violations: List<ViolationEvent> = emptyList(),
    val summaries: List<ViolationSummary> = emptyList(),
    val filter: ViolationFilter = ViolationFilter(),
    val selectedEvent: ViolationEvent? = null,
)

/**
 * ViewModel do painel de debug — sem Hilt para manter o SDK agnóstico.
 * Recebe use cases via factory para injeção manual.
 */
internal class ViolationPanelViewModel(
    observeViolations: ObserveViolations,
    observeSummaries: ObserveViolationSummaries,
) : ViewModel() {

    private val _filter = MutableStateFlow(ViolationFilter())
    val filter: StateFlow<ViolationFilter> = _filter.asStateFlow()

    private val _selectedEvent = MutableStateFlow<ViolationEvent?>(null)

    val uiState: StateFlow<ViolationPanelUiState> = combine(
        observeViolations(),
        observeSummaries(),
        _filter,
        _selectedEvent,
    ) { violations, summaries, filter, selected ->
        ViolationPanelUiState(
            violations = violations.filter(filter).reversed(), // mais recente primeiro
            summaries = summaries,
            filter = filter,
            selectedEvent = selected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ViolationPanelUiState(),
    )

    fun onFilterCategory(category: ViolationCategory?) {
        _filter.update { it.copy(category = category) }
    }

    fun onFilterSeverity(minSeverity: ViolationSeverity) {
        _filter.update { it.copy(minSeverity = minSeverity) }
    }

    fun onFilterThread(query: String) {
        _filter.update { it.copy(threadQuery = query) }
    }

    fun onSelectEvent(event: ViolationEvent?) {
        _selectedEvent.value = event
    }

    fun clearViolations() {
        PerfKit.clearViolations()
    }

    private fun List<ViolationEvent>.filter(f: ViolationFilter): List<ViolationEvent> =
        this.filter { event ->
            (f.category == null || event.category == f.category) &&
            event.severity >= f.minSeverity &&
            (f.threadQuery.isBlank() || event.threadName.contains(f.threadQuery, ignoreCase = true))
        }

    companion object {
        fun factory(
            observeViolations: ObserveViolations,
            observeSummaries: ObserveViolationSummaries,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ViolationPanelViewModel(observeViolations, observeSummaries) as T
        }
    }
}
