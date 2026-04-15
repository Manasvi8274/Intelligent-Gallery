package com.intelligentgallery.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intelligentgallery.app.R
import com.intelligentgallery.app.data.model.GalleryImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(text = stringResource(id = R.string.search_hint)) }
                )
                Button(onClick = onSearchClicked) {
                    Text(text = "Go")
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            uiState.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.images.isEmpty() && !uiState.isLoading) {
                Text(text = stringResource(id = R.string.empty_results))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.images) { image ->
                        ImageCard(image = image)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCard(image: GalleryImage) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Image ID: ${image.id}", style = MaterialTheme.typography.titleSmall)
            Text(text = "URI: ${image.contentUri}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Time: ${formatter.format(Date(image.capturedAtEpochMs))}")
            Text(text = "Place: ${image.place ?: "-"}")
            Text(text = "Landmark: ${image.nearestLandmark ?: "-"}")
            Text(text = "Occasion: ${image.occasion ?: "-"}")
            Text(text = "People IDs: ${image.peopleIds.joinToString()}")
        }
    }
}
