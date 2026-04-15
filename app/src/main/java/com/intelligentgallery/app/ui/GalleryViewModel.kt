package com.intelligentgallery.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.intelligentgallery.app.data.model.GalleryImage
import com.intelligentgallery.app.data.model.PendingFaceLabel
import com.intelligentgallery.app.data.repo.GalleryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryUiState(
    val hasMediaPermission: Boolean = false,
    val query: String = "",
    val images: List<GalleryImage> = emptyList(),
    val pendingFace: PendingFaceLabel? = null,
    val syncedCount: Int = 0,
    val faceProcessedCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class GalleryViewModel(
    private val repository: GalleryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    private var importJob: Job? = null
    private var faceJob: Job? = null

    fun onPermissionChanged(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasMediaPermission = granted)
        if (granted) {
            startBackgroundSync()
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun runSearch() {
        // Placeholder for AI query search phase; current focus is gallery-first experience.
    }

    private fun startBackgroundSync() {
        if (importJob?.isActive == true || faceJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        var importFinished = false

        importJob = viewModelScope.launch {
            runCatching {
                while (true) {
                    val result = repository.importNextImageBatch(batchSize = 10)
                    val images = repository.getAllImages()
                    _uiState.value = _uiState.value.copy(
                        images = images,
                        syncedCount = result.totalInDb,
                        isLoading = images.isEmpty()
                    )
                    if (!result.hasMoreToImport) break
                    delay(150)
                }
                importFinished = true
            }.onFailure { error ->
                importFinished = true
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to import gallery images"
                )
            }
        }

        faceJob = viewModelScope.launch {
            runCatching {
                var idleRounds = 0
                while (true) {
                    val result = repository.processPendingFacesBatch(limit = 2)
                    val pending = repository.getNextPendingFace()
                    _uiState.value = _uiState.value.copy(
                        pendingFace = pending,
                        faceProcessedCount = _uiState.value.faceProcessedCount + result.processed,
                        isLoading = _uiState.value.images.isEmpty()
                    )
                    if (result.processed == 0) idleRounds += 1 else idleRounds = 0
                    if (importFinished && !result.hasMoreToProcess && idleRounds >= 3) break
                    delay(200)
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed during face processing"
                )
            }
        }
    }

    fun savePendingFaceName(name: String) {
        val pending = _uiState.value.pendingFace ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                repository.saveFaceLabel(pending.faceId, name)
                val pendingNext = repository.getNextPendingFace()
                val images = repository.getAllImages()
                _uiState.value = _uiState.value.copy(
                    pendingFace = pendingNext,
                    images = images,
                    syncedCount = images.size,
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to save face label"
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
