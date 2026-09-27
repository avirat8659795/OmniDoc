package com.docopener.universal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.data.RecentDocsRepository
import com.docopener.universal.domain.DocumentCategory
import com.docopener.universal.domain.DocumentItem
import com.docopener.universal.ui.components.CategoryPills
import com.docopener.universal.ui.components.DocCard
import com.docopener.universal.ui.components.FloatingDock
import com.docopener.universal.ui.components.StorageSummaryBar
import com.docopener.universal.ui.theme.AccentPrimaryLight
import com.docopener.universal.ui.theme.DarkBackground
import com.docopener.universal.ui.theme.DarkBorder
import com.docopener.universal.ui.theme.DarkCard
import com.docopener.universal.ui.theme.DarkSurface
import com.docopener.universal.ui.theme.TextMuted
import com.docopener.universal.ui.theme.TextPrimary
import com.docopener.universal.ui.theme.TextSecondary

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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Files, 1: Recent, 2: Saved/Bookmarks

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
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OmniDoc",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentPrimaryLight.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPrimaryLight,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            FloatingDock(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onMakePdf = onNavigateToCreatePdf,
                onOpenFile = {
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Minimal Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = "Search documents by name or extension...",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedBorderColor = AccentPrimaryLight,
                    unfocusedBorderColor = DarkBorder
                )
            )

            // Storage Overview
            StorageSummaryBar(
                totalFiles = documents.size,
                countsByCategory = countsByCategory
            )

            // Category Filter Pills
            CategoryPills(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                counts = countsByCategory
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Document List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = AccentPrimaryLight,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            } else if (displayedDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching documents" else "No documents found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Browse' or 'Make PDF' to get started.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
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
