package com.intelligentgallery.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.intelligentgallery.app.R
import com.intelligentgallery.app.data.model.GalleryImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onRequestPermissions: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit,
    onSavePendingFace: (String) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<AlbumGroup?>(null) }
    val albums = remember(uiState.images) { buildMonthlyAlbums(uiState.images) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedAlbum?.title ?: stringResource(id = R.string.app_name)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!uiState.hasMediaPermission) {
                PermissionSection(onRequestPermissions = onRequestPermissions)
                return@Column
            }

            SyncStatusRow(
                syncedCount = uiState.syncedCount,
                processedCount = uiState.faceProcessedCount,
                isLoading = uiState.isLoading
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(text = stringResource(id = R.string.search_hint)) }
                )
                Button(onClick = onSearchClicked, enabled = false) {
                    Text(text = "Soon")
                }
            }

            uiState.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.images.isEmpty() && !uiState.isLoading) {
                Text(text = "No images loaded yet.")
            } else {
                if (selectedAlbum == null) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(albums) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { selectedAlbum = album }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Button(onClick = { selectedAlbum = null }) {
                            Text("Back to Albums")
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(selectedAlbum!!.images) { image ->
                            GalleryTile(image = image, onClick = { selectedImageUri = image.contentUri })
                        }
                    }
                }
            }
        }

        uiState.pendingFace?.let { pending ->
            PendingFaceDialog(
                personNumber = pending.personNumber,
                bitmap = pending.cropBitmap,
                onSave = onSavePendingFace
            )
        }
        selectedImageUri?.let { uri ->
            FullImageDialog(imageUri = uri, onDismiss = { selectedImageUri = null })
        }
    }
}

@Composable
private fun SyncStatusRow(syncedCount: Int, processedCount: Int, isLoading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Images: $syncedCount  •  Faces processed: $processedCount",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun PermissionSection(onRequestPermissions: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Storage access is required to load your gallery.")
            Button(onClick = onRequestPermissions) {
                Text("Grant media permissions")
            }
        }
    }
}

@Composable
private fun PendingFaceDialog(
    personNumber: Int,
    bitmap: android.graphics.Bitmap,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Who is this person? (#$personNumber)",
                    style = MaterialTheme.typography.titleMedium
                )
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Detected face crop",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter person name") }
                )
                Button(
                    onClick = {
                        onSave(name)
                        name = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun GalleryTile(image: GalleryImage, onClick: () -> Unit) {
    AsyncImage(
        model = image.contentUri,
        contentDescription = "Gallery image",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun FullImageDialog(imageUri: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Full image view",
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxSize(0.9f),
            contentScale = ContentScale.Fit
        )
    }
}

private data class AlbumGroup(
    val title: String,
    val subtitle: String,
    val images: List<GalleryImage>
)

private fun buildMonthlyAlbums(images: List<GalleryImage>): List<AlbumGroup> {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)

    val grouped = images.groupBy { image ->
        val cal = Calendar.getInstance().apply { timeInMillis = image.capturedAtEpochMs }
        cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }.toList().sortedByDescending { (ym, _) ->
        val (year, month) = ym
        year * 100 + month
    }

    return grouped.map { (ym, monthImages) ->
        val (year, month) = ym
        val title = when {
            year == currentYear && month == currentMonth -> "MTD"
            year == currentYear && month == currentMonth - 1 -> "Last Month"
            currentMonth == Calendar.JANUARY && year == currentYear - 1 && month == Calendar.DECEMBER -> "Last Month"
            else -> SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }.time
            )
        }
        AlbumGroup(
            title = title,
            subtitle = "${monthImages.size} photos",
            images = monthImages.sortedByDescending { it.capturedAtEpochMs }
        )
    }
}

@Composable
private fun AlbumCard(album: AlbumGroup, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = album.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = album.subtitle, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val preview = album.images.take(min(3, album.images.size))
                preview.forEach { image ->
                    AsyncImage(
                        model = image.contentUri,
                        contentDescription = "Album preview",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                }
                repeat(3 - preview.size) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}
