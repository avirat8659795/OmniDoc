package com.docopener.universal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocumentItem
import com.docopener.universal.domain.DocumentType
import com.docopener.universal.engine.DocTypeDetector
import com.docopener.universal.engine.PageMargin
import com.docopener.universal.engine.PageOrientation
import com.docopener.universal.engine.PdfCreationOptions
import com.docopener.universal.engine.PdfCreatorEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePdfScreen(
    onBack: () -> Unit,
    onPdfCreated: (DocumentItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfCreator = remember { PdfCreatorEngine(context) }

    var selectedMode by remember { mutableIntStateOf(0) } // 0: Images to PDF, 1: Text to PDF

    // Mode 0: Images to PDF State
    val selectedImages = remember { mutableStateListOf<Uri>() }
    var pdfTitle by remember { mutableStateOf("Doc_${System.currentTimeMillis() / 1000}") }
    var orientation by remember { mutableStateOf(PageOrientation.PORTRAIT) }
    var margin by remember { mutableStateOf(PageMargin.SMALL) }

    // Mode 1: Text to PDF State
    var textDocTitle by remember { mutableStateOf("Notes_${System.currentTimeMillis() / 1000}") }
    var textBody by remember { mutableStateOf("") }

    var isCreating by remember { mutableStateOf(false) }

    // Multi-Image Picker Launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create PDF Document",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedMode,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    icon = { Icon(Icons.Default.Image, contentDescription = null) },
                    text = { Text("Pictures to PDF") }
                )
                Tab(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    text = { Text("Text/Notes to PDF") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedMode == 0) {
                    // IMAGE TO PDF FLOW
                    Text(
                        text = "1. Document Name",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = pdfTitle,
                        onValueChange = { pdfTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    Text(
                        text = "2. Select Pictures (${selectedImages.size} selected)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Add Pictures Button & Thumbnails
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Choose Pictures / Photos")
                            }

                            if (selectedImages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(selectedImages) { index, uri ->
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Text(
                                                    text = "Page ${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Remove image button
                                            IconButton(
                                                onClick = { selectedImages.removeAt(index) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Page Orientation & Margin Settings
                    Text(
                        text = "3. Page Layout Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = orientation == PageOrientation.PORTRAIT,
                            onClick = { orientation = PageOrientation.PORTRAIT },
                            label = { Text("Portrait") }
                        )
                        FilterChip(
                            selected = orientation == PageOrientation.LANDSCAPE,
                            onClick = { orientation = PageOrientation.LANDSCAPE },
                            label = { Text("Landscape") }
                        )
                        FilterChip(
                            selected = orientation == PageOrientation.AUTO_MATCH_IMAGE,
                            onClick = { orientation = PageOrientation.AUTO_MATCH_IMAGE },
                            label = { Text("Auto Fit") }
                        )
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            if (selectedImages.isEmpty()) {
                                Toast.makeText(context, "Please select at least one picture", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isCreating = true
                            scope.launch {
                                val opts = PdfCreationOptions(
                                    title = pdfTitle,
                                    orientation = orientation,
                                    margin = margin
                                )
                                val res = pdfCreator.createPdfFromImages(selectedImages.toList(), opts)
                                res.onSuccess { uri ->
                                    isCreating = false
                                    Toast.makeText(context, "PDF Created Successfully!", Toast.LENGTH_SHORT).show()
                                    val name = DocTypeDetector.getFileName(context, uri)
                                    val size = DocTypeDetector.getFileSize(context, uri)
                                    val item = DocumentItem(
                                        uri = uri,
                                        name = name,
                                        sizeBytes = size,
                                        lastModified = System.currentTimeMillis(),
                                        type = DocumentType.PDF
                                    )
                                    onPdfCreated(item)
                                }.onFailure { err ->
                                    isCreating = false
                                    Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isCreating && selectedImages.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Building PDF...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convert ${selectedImages.size} Pictures to PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // TEXT TO PDF FLOW
                    Text(
                        text = "Document Title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = textDocTitle,
                        onValueChange = { textDocTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Document Text / Notes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = textBody,
                        onValueChange = { textBody = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        placeholder = { Text("Type or paste any text or notes to turn into a clean formatted PDF...") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (textBody.isBlank()) {
                                Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isCreating = true
                            scope.launch {
                                val opts = PdfCreationOptions(title = textDocTitle)
                                val res = pdfCreator.createPdfFromText(textDocTitle, textBody, opts)
                                res.onSuccess { uri ->
                                    isCreating = false
                                    Toast.makeText(context, "PDF Created Successfully!", Toast.LENGTH_SHORT).show()
                                    val name = DocTypeDetector.getFileName(context, uri)
                                    val size = DocTypeDetector.getFileSize(context, uri)
                                    val item = DocumentItem(
                                        uri = uri,
                                        name = name,
                                        sizeBytes = size,
                                        lastModified = System.currentTimeMillis(),
                                        type = DocumentType.PDF
                                    )
                                    onPdfCreated(item)
                                }.onFailure { err ->
                                    isCreating = false
                                    Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isCreating && textBody.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating PDF...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate PDF from Text", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
