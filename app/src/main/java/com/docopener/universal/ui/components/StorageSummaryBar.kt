package com.docopener.universal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocumentCategory
import com.docopener.universal.ui.theme.AccentPrimaryLight
import com.docopener.universal.ui.theme.DarkBorder
import com.docopener.universal.ui.theme.DarkCard
import com.docopener.universal.ui.theme.DocBadgeEpub
import com.docopener.universal.ui.theme.DocBadgeExcel
import com.docopener.universal.ui.theme.DocBadgePdf
import com.docopener.universal.ui.theme.DocBadgePpt
import com.docopener.universal.ui.theme.DocBadgeText
import com.docopener.universal.ui.theme.DocBadgeWord
import com.docopener.universal.ui.theme.TextMuted
import com.docopener.universal.ui.theme.TextPrimary

@Composable
fun StorageSummaryBar(
    totalFiles: Int,
    countsByCategory: Map<DocumentCategory, Int>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(0.75.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = AccentPrimaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Storage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "$totalFiles documents",
                    fontSize = 12.sp,
                    color = AccentPrimaryLight,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-segment progress indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkBorder)
            ) {
                if (totalFiles > 0) {
                    val pdfWeight = (countsByCategory[DocumentCategory.PDF] ?: 0).toFloat() / totalFiles
                    val wordWeight = (countsByCategory[DocumentCategory.WORD] ?: 0).toFloat() / totalFiles
                    val excelWeight = (countsByCategory[DocumentCategory.EXCEL] ?: 0).toFloat() / totalFiles
                    val pptWeight = (countsByCategory[DocumentCategory.PPT] ?: 0).toFloat() / totalFiles
                    val textWeight = (countsByCategory[DocumentCategory.TEXT] ?: 0).toFloat() / totalFiles
                    val epubWeight = (countsByCategory[DocumentCategory.EPUB] ?: 0).toFloat() / totalFiles

                    if (pdfWeight > 0) Box(modifier = Modifier.weight(pdfWeight.coerceAtLeast(0.02f)).height(5.dp).background(DocBadgePdf))
                    if (wordWeight > 0) Box(modifier = Modifier.weight(wordWeight.coerceAtLeast(0.02f)).height(5.dp).background(DocBadgeWord))
                    if (excelWeight > 0) Box(modifier = Modifier.weight(excelWeight.coerceAtLeast(0.02f)).height(5.dp).background(DocBadgeExcel))
                    if (pptWeight > 0) Box(modifier = Modifier.weight(pptWeight.coerceAtLeast(0.02f)).height(5.dp).background(DocBadgePpt))
                    if (textWeight > 0) Box(modifier = Modifier.weight(textWeight.coerceAtLeast(0.02f)).height(5.dp).background(DocBadgeText))
                    if (epubWeight > 0) Box(modifier = Modifier.weight(epubWeight.coerceAtLeast(0.02f)).height(5.dp).background(DocBadgeEpub))
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(DarkBorder))
                }
            }
        }
    }
}
