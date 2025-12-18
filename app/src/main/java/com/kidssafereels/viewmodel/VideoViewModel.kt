package com.kidssafereels.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidssafereels.data.Video
import com.kidssafereels.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for the video player screen
 */
sealed class VideoUiState {
    object Loading : VideoUiState()
    data class Success(val videos: List<Video>) : VideoUiState()
    data class Error(val message: String) : VideoUiState()
}

/**
 * ViewModel for managing video list and playback state
 */
class VideoViewModel : ViewModel() {
    
    private val repository = VideoRepository()
    
    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Loading)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()
    
    private val _currentVideoIndex = MutableStateFlow(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()
    
    init {
        loadVideos()
    }
    
    /**
     * Load videos from the repository
     * Called on app start and can be called to refresh
     */
    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = VideoUiState.Loading
            
            val result = repository.fetchVideos()
            
            result.fold(
                onSuccess = { videos ->
                    if (videos.isNotEmpty()) {
                        _uiState.value = VideoUiState.Success(videos)
                        _currentVideoIndex.value = 0
                    } else {
                        _uiState.value = VideoUiState.Error("No videos found")
                    }
                },
                onFailure = { error ->
                    _uiState.value = VideoUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
    
    /**
     * Update the current video index when user swipes
     */
    fun setCurrentVideoIndex(index: Int) {
        _currentVideoIndex.value = index
    }
    
    /**
     * Refresh videos - called when app resumes
     */
    fun refresh() {
        loadVideos()
    }
}

