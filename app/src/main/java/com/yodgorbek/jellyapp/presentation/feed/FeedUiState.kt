
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yodgorbek.jellyapp.data.model.VideoFeedItem
import com.yodgorbek.jellyapp.domain.usacase.FetchFeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val videos: List<VideoFeedItem>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel(
    private val fetchFeedUseCase: FetchFeedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        fetchFeed()
    }

    private fun fetchFeed() {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading
            fetchFeedUseCase().fold(
                onSuccess = { _uiState.value = FeedUiState.Success(it) },
                onFailure = { _uiState.value = FeedUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}