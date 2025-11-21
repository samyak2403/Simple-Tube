package com.samyak.simpletube.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.pages.SearchSummaryPage
import com.samyak.simpletube.models.ItemsPage
import com.samyak.simpletube.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // Decode URL-encoded query (e.g., "mama+chi+porgi" → "mama chi porgi")
    val query = URLDecoder.decode(savedStateHandle.get<String>("query")!!, "UTF-8")
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        // Load search results immediately on initialization
        viewModelScope.launch {
            // Load summary (ALL filter) immediately
            YouTube.searchSummary(query)
                .onSuccess {
                    summaryPage = it
                }
                .onFailure {
                    reportException(it)
                    // Don't set empty summary - keep it null to show error state properly
                    // The UI will handle null state differently than empty state
                }
        }
        
        // Listen for filter changes
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    // Already loaded in init above
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube.search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] = ItemsPage(result.items.distinctBy { it.id }, result.continuation)
                            }
                            .onFailure {
                                reportException(it)
                                // Set empty results to stop shimmer
                                viewStateMap[filter.value] = ItemsPage(emptyList(), null)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult = YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
            }
        }
    }
}
