
package com.docopener.universal.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocxElement
import com.docopener.universal.domain.DocxParagraph
import com.docopener.universal.domain.DocxTable
import com.docopener.universal.domain.ParsedDocxDocument
import com.docopener.universal.engine.DocxEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocxViewerScreen(
    uri: Uri,
    title: String,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val docxEngine = remember { DocxEngine(context) }

    var parsedDoc by remember { mutableStateOf<ParsedDocxDocument?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fontScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(uri) {
        isLoading = true
        val res = docxEngine.parse(uri, title)
        res.onSuccess { doc ->
            parsedDoc = doc
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Failed to read Word document"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        fontScale = if (fontScale >= 1.5f) 1f else fontScale + 0.2f
                    }) {
                        Icon(imageVector = Icons.Default.FormatSize, contentDescription = "Text Size")
                    }
                    IconButton(onClick = onShare) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                    val elements = parsedDoc!!.elements
                    if (elements.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Word document contains no readable text or layout.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(elements) { element ->
                                when (element) {
                                    is DocxElement.ParagraphElement -> {
                                        RenderParagraph(element.paragraph, fontScale)
                                    }
                                    is DocxElement.TableElement -> {
                                        RenderTable(element.table, fontScale)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderParagraph(paragraph: DocxParagraph, fontScale: Float) {
    val annotatedString = buildAnnotatedString {
        if (paragraph.isBullet) {
            append("• ")
        }
        if (paragraph.runs.isNotEmpty()) {
            paragraph.runs.forEach { run ->
                withStyle(
                    style = SpanStyle(
                        fontWeight = if (run.isBold || paragraph.isBold || paragraph.isHeading) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (run.isItalic || paragraph.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (run.isUnderline) TextDecoration.Underline else TextDecoration.None,
                        fontSize = ((if (paragraph.isHeading) 18f else 15f) * fontScale).sp
                    )
                ) {
                    append(run.text)
                }
            }
        } else {
            append(paragraph.text)
        }
    }

    Text(
        text = annotatedString,
        color = MaterialTheme.colorScheme.onBackground,
        lineHeight = (22 * fontScale).sp
    )
}

@Composable
private fun RenderTable(table: DocxTable, fontScale: Float) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(8.dp)
        ) {
            table.rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rowIndex % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(vertical = 6.dp)
                ) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = cell,
                                fontSize = (13 * fontScale).sp,
                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
