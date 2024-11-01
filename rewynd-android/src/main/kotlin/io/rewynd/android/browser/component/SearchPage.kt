package io.rewynd.android.browser.component

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.rewynd.android.browser.BrowserNavigationActions
import io.rewynd.android.browser.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    viewModel: BrowserViewModel,
    actions: BrowserNavigationActions,
    modifier: Modifier = Modifier,
    onActiveChange: (Boolean) -> Unit = { }
) {
    val searchText by viewModel.searchText.collectAsStateWithLifecycle("")
    val results by viewModel.searchResults.collectAsStateWithLifecycle(emptyList())
    val scrollState = rememberScrollState()

    LaunchedEffect(searchText) {
        scrollState.scrollTo(0)
    }

    Log.i("RewyndSearch", results.toString())
    SearchBar(
        modifier = modifier,
        query = searchText,
        onQueryChange = {
            viewModel.search(it)
            viewModel.viewModelScope.launch {
                scrollState.scrollTo(0)
            }
        },
        onSearch = {
            viewModel.search(it)
        },
        active = true,
        onActiveChange = {
            onActiveChange(it)
        },
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            results.forEach {
                SearchResultCard(it, viewModel.imageLoader, actions, Modifier.padding(vertical = 1.dp))
            }
        }
    }
}
