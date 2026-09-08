package com.tana.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Shape
import com.tana.ui.theme.PrimaryButtonGradient
import com.tana.ui.theme.DisabledButtonGradient
import com.tana.data.model.TransactionType
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentIncome
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.DarkGlassBackground
import com.tana.ui.theme.DarkGlassBorder
import com.tana.ui.theme.GlassGradientBorder
import com.tana.ui.viewmodel.FinanceViewModel

@Composable
fun MonochromeCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
    borderColor: Color? = null,
    borderBrush: Brush? = GlassGradientBorder,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderModifier = if (borderBrush != null) {
        Modifier.border(BorderStroke(1.dp, borderBrush), shape)
    } else if (borderColor != null) {
        Modifier.border(BorderStroke(1.dp, borderColor), shape)
    } else {
        Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), shape)
    }

    val baseModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .then(borderModifier)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier.padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    MonochromeCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        borderBrush = GlassGradientBorder,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content
    )
}

@Composable
fun CategoryAvatar(
    category: String,
    type: TransactionType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val (iconBg, iconFg) = when (type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f) to MaterialTheme.colorScheme.onSurface
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        TransactionType.SAVINGS -> MaterialTheme.colorScheme.surfaceVariant to AccentSavings
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(iconBg)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(category, type),
            contentDescription = category,
            tint = iconFg,
            modifier = Modifier.size((size.value * 0.5f).dp)
        )
    }
}

@Composable
fun AnimatedAmountText(
    targetAmount: Double,
    modifier: Modifier = Modifier,
    prefix: String = "Rp ",
    fontSize: TextUnit = 28.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = "animated_amount"
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetAmount.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "amount_animation"
    )

    Text(
        text = "$prefix${FinanceViewModel.formatRupiah(animatedValue.toDouble()).replace("Rp", "").trim()}",
        style = MaterialTheme.typography.displaySmall.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = (-0.5).sp
        ),
        color = color,
        modifier = modifier.testTag(testTag)
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { onActionClick() }
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionTypeBadge(
    type: TransactionType,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg, icon) = when (type) {
        TransactionType.EXPENSE -> Quadruple(
            "PENGELUARAN",
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            AccentExpense,
            Icons.Filled.ArrowDownward
        )
        TransactionType.INCOME -> Quadruple(
            "PEMASUKAN",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            Icons.Filled.ArrowUpward
        )
        TransactionType.SAVINGS -> Quadruple(
            "TABUNGAN",
            MaterialTheme.colorScheme.surfaceVariant,
            AccentSavings,
            Icons.Filled.Savings
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = fg
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun getCategoryIcon(category: String, type: TransactionType): ImageVector {
    val cat = category.lowercase()
    return when {
        cat.contains("makan") || cat.contains("minum") || cat.contains("kopi") || cat.contains("resto") -> Icons.Filled.Fastfood
        cat.contains("trans") || cat.contains("bensin") || cat.contains("parkir") || cat.contains("ojol") || cat.contains("gojek") || cat.contains("grab") -> Icons.Filled.DirectionsCar
        cat.contains("belanja") || cat.contains("shopping") || cat.contains("market") -> Icons.Filled.ShoppingBag
        cat.contains("tagihan") || cat.contains("listrik") || cat.contains("air") || cat.contains("wifi") || cat.contains("pulsa") -> Icons.Filled.Receipt
        cat.contains("hiburan") || cat.contains("game") || cat.contains("nonton") || cat.contains("bioskop") -> Icons.Filled.SportsEsports
        cat.contains("kesehatan") || cat.contains("obat") || cat.contains("dokter") || cat.contains("klinik") -> Icons.Filled.HealthAndSafety
        cat.contains("edukasi") || cat.contains("buku") || cat.contains("kursus") || cat.contains("sekolah") -> Icons.Filled.School
        cat.contains("rumah") || cat.contains("kos") || cat.contains("sewa") -> Icons.Filled.Home
        cat.contains("gaji") || cat.contains("freelance") || cat.contains("bonus") || cat.contains("proyek") || cat.contains("penjualan") -> Icons.Filled.Work
        cat.contains("tabung") || cat.contains("invest") || cat.contains("deposito") || cat.contains("emas") || type == TransactionType.SAVINGS -> Icons.Filled.Savings
        cat.contains("bank") || cat.contains("transfer") -> Icons.Filled.AccountBalance
        else -> when (type) {
            TransactionType.EXPENSE -> Icons.Filled.ArrowDownward
            TransactionType.INCOME -> Icons.Filled.AccountBalanceWallet
            TransactionType.SAVINGS -> Icons.Filled.Savings
        }
    }
}

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val digits = originalText.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formatted = StringBuilder()
        val len = digits.length
        for (i in 0 until len) {
            formatted.append(digits[i])
            if ((len - 1 - i) % 3 == 0 && i != len - 1) {
                formatted.append('.')
            }
        }

        val formattedString = formatted.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceAtMost(len)
                var transformedOffset = clampedOffset
                for (i in 0 until clampedOffset) {
                    if ((len - 1 - i) % 3 == 0 && i != len - 1) {
                        transformedOffset++
                    }
                }
                return transformedOffset.coerceAtMost(formattedString.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedOffset = offset.coerceAtMost(formattedString.length)
                var originalOffset = 0
                for (i in 0 until clampedOffset) {
                    if (formattedString[i] != '.') {
                        originalOffset++
                    }
                }
                return originalOffset.coerceAtMost(len)
            }
        }

        return TransformedText(
            AnnotatedString(formattedString),
            offsetMapping
        )
    }
}

/**
 * Animated moving gradient beam / specular lighting shimmer that sweeps smoothly across any card surface
 */
@Composable
fun Modifier.animatedSpecularGlow(
    glowColor: Color = Color(0xFF818CF8),
    glowAlpha: Float = 0.25f,
    durationMillis: Int = 3200
): Modifier {
    val transition = rememberInfiniteTransition(label = "specular_glow_transition")
    val offsetProgress by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "specular_offset"
    )

    return this.drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        val startX = width * offsetProgress
        val startY = 0f
        val endX = startX + width * 0.4f
        val endY = height

        val shimmerBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                glowColor.copy(alpha = glowAlpha * 0.2f),
                glowColor.copy(alpha = glowAlpha),
                glowColor.copy(alpha = glowAlpha * 0.2f),
                Color.Transparent
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY)
        )
        drawRect(brush = shimmerBrush)
    }
}

/**
 * Premium Anti-Slop Gradient Button with hardware-accelerated sleek surface,
 * specular border, and tactile pressed states.
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = PrimaryButtonGradient,
    disabledGradient: Brush = DisabledButtonGradient,
    shape: Shape = RoundedCornerShape(16.dp),
    border: BorderStroke? = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
    contentColor: Color = Color.White,
    disabledContentColor: Color = Color(0xFF94A3B8),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 13.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val bgBrush = if (enabled) gradient else disabledGradient
    val textColor = if (enabled) contentColor else disabledContentColor

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled) { onClick() },
        color = Color.Transparent,
        shape = shape,
        border = if (enabled) border else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textColor) {
                    content()
                }
            }
        }
    }
}


