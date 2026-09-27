package com.docopener.universal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocumentType
import com.docopener.universal.ui.theme.DocBadgeCode
import com.docopener.universal.ui.theme.DocBadgeEpub
import com.docopener.universal.ui.theme.DocBadgeExcel
import com.docopener.universal.ui.theme.DocBadgeOther
import com.docopener.universal.ui.theme.DocBadgePdf
import com.docopener.universal.ui.theme.DocBadgePpt
import com.docopener.universal.ui.theme.DocBadgeText
import com.docopener.universal.ui.theme.DocBadgeWord

@Composable
fun DocBadge(type: DocumentType, modifier: Modifier = Modifier) {
    val (label, badgeColor) = when (type) {
        DocumentType.PDF -> "PDF" to DocBadgePdf
        DocumentType.DOCX, DocumentType.DOC, DocumentType.RTF -> "DOC" to DocBadgeWord
        DocumentType.XLSX, DocumentType.XLS, DocumentType.CSV -> "XLS" to DocBadgeExcel
        DocumentType.PPTX, DocumentType.PPT -> "PPT" to DocBadgePpt
        DocumentType.TXT -> "TXT" to DocBadgeText
        DocumentType.MD -> "MD" to DocBadgeText
        DocumentType.EPUB -> "EPUB" to DocBadgeEpub
        DocumentType.CODE -> "CODE" to DocBadgeCode
        DocumentType.UNKNOWN -> "FILE" to DocBadgeOther
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.12f))
            .border(0.75.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
    }
}
