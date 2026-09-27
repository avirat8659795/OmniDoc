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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.docopener.universal.ui.theme.AccentPrimary
import com.docopener.universal.ui.theme.AccentPrimaryLight
import com.docopener.universal.ui.theme.DarkBackground
import com.docopener.universal.ui.theme.DarkBorder
import com.docopener.universal.ui.theme.DarkCard
import com.docopener.universal.ui.theme.DarkSurface
import com.docopener.universal.ui.theme.DocBadgePdf
import com.docopener.universal.ui.theme.TextMuted
import com.docopener.universal.ui.theme.TextPrimary
import com.docopener.universal.ui.theme.TextSecondary
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
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Make PDF",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
                containerColor = DarkSurface,
                contentColor = AccentPrimaryLight
            ) {
                Tab(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Pictures to PDF", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                )
                Tab(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Text/Notes to PDF", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
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
                        text = "Document Title",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = pdfTitle,
                        onValueChange = { pdfTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            focusedBorderColor = AccentPrimaryLight,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Text(
                        text = "Selected Pictures (${selectedImages.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    // Add Pictures Button & Thumbnails
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkCard)
                            .border(0.75.dp, DarkBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Select Pictures / Photos", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }

                            if (selectedImages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(selectedImages) { index, uri ->
                                        Box(
                                            modifier = Modifier
                                                .size(86.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(DarkSurface)
                                                .border(0.75.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = AccentPrimaryLight,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Page ${index + 1}",
                                                    fontSize = 11.sp,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            // Remove image button
                                            IconButton(
                                                onClick = { selectedImages.removeAt(index) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
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
                        text = "Page Layout",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = orientation == PageOrientation.PORTRAIT,
                            onClick = { orientation = PageOrientation.PORTRAIT },
                            label = { Text("Portrait") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = orientation == PageOrientation.LANDSCAPE,
                            onClick = { orientation = PageOrientation.LANDSCAPE },
                            label = { Text("Landscape") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = orientation == PageOrientation.AUTO_MATCH_IMAGE,
                            onClick = { orientation = PageOrientation.AUTO_MATCH_IMAGE },
                            label = { Text("Auto-Fit") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary,
                                selectedLabelColor = Color.White
                            )
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
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimaryLight)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Building PDF...", fontWeight = FontWeight.Bold, color = DarkBackground)
                        } else {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convert ${selectedImages.size} Pictures to PDF", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }
                } else {
                    // TEXT TO PDF FLOW
                    Text(
                        text = "Document Title",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = textDocTitle,
                        onValueChange = { textDocTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            focusedBorderColor = AccentPrimaryLight,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Text(
                        text = "Document Text / Notes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = textBody,
                        onValueChange = { textBody = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        placeholder = { Text("Type or paste any text or notes to turn into a clean formatted PDF...", color = TextMuted) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            focusedBorderColor = AccentPrimaryLight,
                            unfocusedBorderColor = DarkBorder
                        )
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
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimaryLight)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating PDF...", fontWeight = FontWeight.Bold, color = DarkBackground)
                        } else {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate PDF from Text", fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }
                }
            }
        }
    }
}
