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
import android.net.Uri
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = try {
        URLDecoder.decode(savedStateHandle.get<String>("query").orEmpty(), "UTF-8")
    } catch (_: Exception) {
        Uri.decode(savedStateHandle.get<String>("query").orEmpty())
    }
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    var isSummaryLoading by mutableStateOf(true)
    var isSummaryError by mutableStateOf(false)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        loadSummary()
        
        // Listen for filter changes
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter != null && viewStateMap[filter.value] == null) {
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

    fun loadSummary() {
        viewModelScope.launch {
            isSummaryLoading = true
            isSummaryError = false
            YouTube.searchSummary(query)
                .onSuccess {
                    summaryPage = it
                    isSummaryLoading = false
                    isSummaryError = false
                }
                .onFailure {
                    reportException(it)
                    isSummaryLoading = false
                    isSummaryError = true
                }
        }
    }

    fun retrySummary() {
        loadSummary()
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
