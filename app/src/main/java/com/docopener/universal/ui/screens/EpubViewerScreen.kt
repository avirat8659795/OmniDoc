package com.docopener.universal.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.ParsedEpubDocument
import com.docopener.universal.engine.EpubEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubViewerScreen(
    uri: Uri,
    title: String,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val epubEngine = remember { EpubEngine(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var parsedDoc by remember { mutableStateOf<ParsedEpubDocument?>(null) }
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fontScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(uri) {
        isLoading = true
        val res = epubEngine.parse(uri, title)
        res.onSuccess { doc ->
            parsedDoc = doc
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Failed to read EPUB"
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                parsedDoc?.chapters?.forEachIndexed { idx, chapter ->
                    NavigationDrawerItem(
                        label = { Text(chapter.title) },
                        selected = idx == currentChapterIndex,
                        onClick = {
                            currentChapterIndex = idx
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            parsedDoc?.chapters?.getOrNull(currentChapterIndex)?.let {
                                Text(
                                    text = it.title,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Chapters")
                        }
                        IconButton(onClick = {
                            fontScale = if (fontScale >= 1.5f) 1f else fontScale + 0.2f
                        }) {
                            Icon(imageVector = Icons.Default.FormatSize, contentDescription = "Font Size")
                        }
                        IconButton(onClick = onShare) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                parsedDoc?.chapters?.let { chapters ->
                    if (chapters.size > 1) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (currentChapterIndex > 0) currentChapterIndex-- },
                                    enabled = currentChapterIndex > 0
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Prev Chapter")
                                }

                                Text(
                                    text = "${currentChapterIndex + 1} of ${chapters.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                IconButton(
                                    onClick = { if (currentChapterIndex < chapters.size - 1) currentChapterIndex++ },
                                    enabled = currentChapterIndex < chapters.size - 1
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Chapter")
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    errorMessage != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    parsedDoc != null -> {
                        val currentChapter = parsedDoc!!.chapters.getOrNull(currentChapterIndex)
                        if (currentChapter != null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = currentChapter.title,
                                        fontSize = (20 * fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = currentChapter.plainText,
                                        fontSize = (15 * fontScale).sp,
                                        lineHeight = (24 * fontScale).sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
