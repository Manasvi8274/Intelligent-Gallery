package com.intelligentgallery.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.intelligentgallery.app.data.local.AppDatabase
import com.intelligentgallery.app.data.repo.GalleryRepository
import com.intelligentgallery.app.ui.GalleryScreen
import com.intelligentgallery.app.ui.GalleryViewModel
import com.intelligentgallery.app.ui.GalleryViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: GalleryViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = GalleryRepository(applicationContext, db.galleryDao())
        GalleryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val mediaPermissions = rememberMediaPermissions()
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                viewModel.onPermissionChanged(result.values.all { it })
            }
            var askedOnce by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                viewModel.onPermissionChanged(hasAllPermissions(mediaPermissions))
            }
            LaunchedEffect(uiState.hasMediaPermission) {
                if (!uiState.hasMediaPermission && !askedOnce) {
                    askedOnce = true
                    permissionLauncher.launch(mediaPermissions.toTypedArray())
                }
            }
            GalleryScreen(
                uiState = uiState,
                onRequestPermissions = {
                    permissionLauncher.launch(mediaPermissions.toTypedArray())
                },
                onQueryChanged = viewModel::onQueryChange,
                onSearchClicked = viewModel::runSearch,
                onSavePendingFace = viewModel::savePendingFaceName
            )
        }
    }

    private fun hasAllPermissions(permissions: List<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun rememberMediaPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
