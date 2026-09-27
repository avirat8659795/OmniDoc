package com.docopener.universal.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocumentCategory

@Composable
fun StorageSummaryBar(
    totalFiles: Int,
    countsByCategory: Map<DocumentCategory, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Documents",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$totalFiles files found",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment progress indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (totalFiles > 0) {
                    val pdfWeight = (countsByCategory[DocumentCategory.PDF] ?: 0).toFloat() / totalFiles
                    val wordWeight = (countsByCategory[DocumentCategory.WORD] ?: 0).toFloat() / totalFiles
                    val excelWeight = (countsByCategory[DocumentCategory.EXCEL] ?: 0).toFloat() / totalFiles
                    val pptWeight = (countsByCategory[DocumentCategory.PPT] ?: 0).toFloat() / totalFiles
                    val textWeight = (countsByCategory[DocumentCategory.TEXT] ?: 0).toFloat() / totalFiles

                    if (pdfWeight > 0) Box(modifier = Modifier.weight(pdfWeight.coerceAtLeast(0.01f)).height(6.dp).background(Color(0xFFE53935)))
                    if (wordWeight > 0) Box(modifier = Modifier.weight(wordWeight.coerceAtLeast(0.01f)).height(6.dp).background(Color(0xFF1E88E5)))
                    if (excelWeight > 0) Box(modifier = Modifier.weight(excelWeight.coerceAtLeast(0.01f)).height(6.dp).background(Color(0xFF43A047)))
                    if (pptWeight > 0) Box(modifier = Modifier.weight(pptWeight.coerceAtLeast(0.01f)).height(6.dp).background(Color(0xFFFB8C00)))
                    if (textWeight > 0) Box(modifier = Modifier.weight(textWeight.coerceAtLeast(0.01f)).height(6.dp).background(Color(0xFF546E7A)))
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}
