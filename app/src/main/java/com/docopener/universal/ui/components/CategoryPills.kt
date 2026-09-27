package com.docopener.universal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docopener.universal.domain.DocumentCategory
import com.docopener.universal.ui.theme.AccentPrimary
import com.docopener.universal.ui.theme.AccentPrimaryLight
import com.docopener.universal.ui.theme.DarkBorder
import com.docopener.universal.ui.theme.DarkCard
import com.docopener.universal.ui.theme.TextMuted
import com.docopener.universal.ui.theme.TextPrimary
import com.docopener.universal.ui.theme.TextSecondary

@Composable
fun CategoryPills(
    selectedCategory: DocumentCategory,
    onSelectCategory: (DocumentCategory) -> Unit,
    counts: Map<DocumentCategory, Int>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DocumentCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            val count = counts[category] ?: 0

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) AccentPrimary else DarkCard,
                label = "pill_bg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) AccentPrimaryLight else DarkBorder,
                label = "pill_border"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSecondary,
                label = "pill_text"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(bgColor)
                    .border(0.75.dp, borderColor, RoundedCornerShape(22.dp))
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.label,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    if (count > 0) {
                        Text(
                            text = count.toString(),
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
