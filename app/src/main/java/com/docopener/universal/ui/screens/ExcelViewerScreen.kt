package com.docopener.universal.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocumentType
import com.docopener.universal.domain.ParsedExcelDocument
import com.docopener.universal.engine.XlsxEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelViewerScreen(
    uri: Uri,
    title: String,
    type: DocumentType,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val xlsxEngine = remember { XlsxEngine(context) }

    var parsedExcel by remember { mutableStateOf<ParsedExcelDocument?>(null) }
    var selectedSheetIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCellInfo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        isLoading = true
        val res = if (type == DocumentType.CSV) {
            xlsxEngine.parseCsv(uri, title)
        } else {
            xlsxEngine.parseXlsx(uri, title)
        }
        res.onSuccess { doc ->
            parsedExcel = doc
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Failed to open spreadsheet"
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
            selectedCellInfo?.let { cellText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = cellText,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                parsedExcel != null -> {
                    val sheets = parsedExcel!!.sheets

                    if (sheets.size > 1) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedSheetIndex,
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            sheets.forEachIndexed { index, sheet ->
                                Tab(
                                    selected = selectedSheetIndex == index,
                                    onClick = { selectedSheetIndex = index },
                                    text = { Text(sheet.name) }
                                )
                            }
                        }
                    }

                    val currentSheet = sheets.getOrNull(selectedSheetIndex) ?: sheets.firstOrNull()
                    if (currentSheet == null || currentSheet.rows.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Sheet is empty.")
                        }
                    } else {
                        val maxCols = currentSheet.rows.maxOfOrNull { it.size } ?: 0
                        val horizontalScrollState = rememberScrollState()

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Column Headers (A, B, C, ...)
                                item {
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        // Top-left blank corner
                                        Box(
                                            modifier = Modifier
                                                .width(48.dp)
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "#", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        for (col in 0 until maxCols) {
                                            val colLetter = getColumnLetter(col)
                                            Box(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = colLetter,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Rows
                                itemsIndexed(currentSheet.rows) { rowIndex, row ->
                                    Row(
                                        modifier = Modifier
                                            .background(if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    ) {
                                        // Row Index Number
                                        Box(
                                            modifier = Modifier
                                                .width(48.dp)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${rowIndex + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        for (colIndex in 0 until maxCols) {
                                            val cellText = row.getOrNull(colIndex) ?: ""
                                            val cellCoord = "${getColumnLetter(colIndex)}${rowIndex + 1}"
                                            Box(
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                    .clickable {
                                                        selectedCellInfo = "[$cellCoord]: $cellText"
                                                    }
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = cellText,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
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
        }
    }
}

private fun getColumnLetter(colIndex: Int): String {
    var num = colIndex
    var letter = ""
    while (num >= 0) {
        letter = ('A'.code + (num % 26)).toChar() + letter
        num = (num / 26) - 1
    }
    return letter
}
