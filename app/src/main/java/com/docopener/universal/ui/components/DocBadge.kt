package com.docopener.universal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocumentType

@Composable
fun DocBadge(type: DocumentType, modifier: Modifier = Modifier) {
    val color = Color(type.color)
    val label = when (type) {
        DocumentType.PDF -> "PDF"
        DocumentType.DOCX, DocumentType.DOC -> "DOC"
        DocumentType.XLSX, DocumentType.XLS, DocumentType.CSV -> "XLS"
        DocumentType.PPTX, DocumentType.PPT -> "PPT"
        DocumentType.TXT -> "TXT"
        DocumentType.MD -> "MD"
        DocumentType.EPUB -> "EPUB"
        DocumentType.RTF -> "RTF"
        DocumentType.CODE -> "CODE"
        DocumentType.UNKNOWN -> "FILE"
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
