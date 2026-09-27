package com.docopener.universal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.data.RecentDocsRepository
import com.docopener.universal.domain.DocumentCategory
import com.docopener.universal.domain.DocumentItem
import com.docopener.universal.ui.components.CategoryPills
import com.docopener.universal.ui.components.DocCard
import com.docopener.universal.ui.components.StorageSummaryBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    documents: List<DocumentItem>,
    isLoading: Boolean,
    recentRepo: RecentDocsRepository,
    onOpenDoc: (DocumentItem) -> Unit,
    onRefresh: () -> Unit,
    onPickFile: (Uri) -> Unit,
    onNavigateToCreatePdf: () -> Unit,
    onShare: (DocumentItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DocumentCategory.ALL) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Files, 1: Recent, 2: Bookmarks

    val recentDocs by recentRepo.recentDocs.collectAsState()
    val favoriteUris by recentRepo.favoriteUris.collectAsState()

    // File picker launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickFile(uri)
        }
    }

    // Counts for Category Pills
    val countsByCategory = remember(documents) {
        val map = mutableMapOf<DocumentCategory, Int>()
        DocumentCategory.values().forEach { cat ->
            map[cat] = if (cat == DocumentCategory.ALL) {
                documents.size
            } else {
                documents.count { it.type.category == cat }
            }
        }
        map
    }

    // Filtered documents based on active tab, search query, and category
    val displayedDocuments = remember(documents, recentDocs, favoriteUris, selectedTab, selectedCategory, searchQuery) {
        val baseList = when (selectedTab) {
            0 -> documents
            1 -> recentDocs
            2 -> documents.filter { favoriteUris.contains(it.uri.toString()) }
            else -> documents
        }

        baseList.filter { doc ->
            val matchesCategory = (selectedCategory == DocumentCategory.ALL) || (doc.type.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() || doc.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OmniDoc Viewer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Offline Universal Document Opener",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCreatePdf) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create PDF",
                            tint = Color(0xFFE53935)
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = onNavigateToCreatePdf,
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Make PDF")
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "Make PDF", fontWeight = FontWeight.Bold)
                    }
                }

                FloatingActionButton(
                    onClick = {
                        filePicker.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "application/vnd.ms-powerpoint",
                                "text/*",
                                "application/epub+zip",
                                "application/rtf",
                                "*/*"
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Open File")
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "Open File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search any document offline...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Storage Overview Dashboard
            StorageSummaryBar(
                totalFiles = documents.size,
                countsByCategory = countsByCategory
            )

            // Tabs: All Files, Recent, Bookmarks
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All Files (${documents.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recent (${recentDocs.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Bookmarks (${favoriteUris.size})") }
                )
            }

            // Category Filter Pills
            CategoryPills(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                counts = countsByCategory
            )

            // Document List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (displayedDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching documents found" else "No documents found",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Open File' or copy documents to your device.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedDocuments, key = { it.uri.toString() }) { doc ->
                        DocCard(
                            doc = doc,
                            isFavorite = favoriteUris.contains(doc.uri.toString()),
                            onClick = { onOpenDoc(doc) },
                            onToggleFavorite = { recentRepo.toggleFavorite(doc.uri) },
                            onShare = { onShare(doc) }
                        )
                    }
                }
            }
        }
    }
}
