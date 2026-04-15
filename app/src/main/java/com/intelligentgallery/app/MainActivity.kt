package com.intelligentgallery.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.intelligentgallery.app.ai.AiModule
import com.intelligentgallery.app.data.local.AppDatabase
import com.intelligentgallery.app.data.repo.GalleryRepository
import com.intelligentgallery.app.ui.GalleryScreen
import com.intelligentgallery.app.ui.GalleryViewModel
import com.intelligentgallery.app.ui.GalleryViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: GalleryViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val aiApi = AiModule.createApi(BuildConfig.AI_BASE_URL)
        val repository = GalleryRepository(db.galleryDao(), aiApi)
        GalleryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            GalleryScreen(
                uiState = uiState,
                onQueryChanged = viewModel::onQueryChange,
                onSearchClicked = viewModel::runSearch
            )
        }
    }
}
