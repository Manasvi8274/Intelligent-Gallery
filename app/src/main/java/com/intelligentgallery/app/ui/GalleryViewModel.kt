package com.intelligentgallery.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.intelligentgallery.app.data.model.GalleryImage
import com.intelligentgallery.app.data.repo.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryUiState(
    val query: String = "",
    val images: List<GalleryImage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GalleryViewModel(
    private val repository: GalleryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDemoData()
            _uiState.value = _uiState.value.copy(images = repository.getAllImages())
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun runSearch() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                if (_uiState.value.query.isBlank()) repository.getAllImages()
                else repository.search(_uiState.value.query)
            }.onSuccess { images ->
                _uiState.value = _uiState.value.copy(images = images, isLoading = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to run search"
                )
            }
        }
    }
}

class GalleryViewModelFactory(
    private val repository: GalleryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(repository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
