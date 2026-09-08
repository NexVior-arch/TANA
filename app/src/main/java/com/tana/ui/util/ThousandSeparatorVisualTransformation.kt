package com.tana.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Visual transformation that automatically formats numbers with thousand separators (dots)
 * e.g., 50000 -> 50.000, 1000000 -> 1.000.000
 * Handles cursor position offset mapping correctly.
 */
class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)

        val parsed = originalText.toLongOrNull()
        val formatted = if (parsed != null) formatter.format(parsed) else originalText

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= originalText.length) return formatted.length

                val subOriginal = originalText.substring(0, offset)
                val subParsed = subOriginal.toLongOrNull()
                val subFormatted = if (subParsed != null) formatter.format(subParsed) else subOriginal
                return subFormatted.length.coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= formatted.length) return originalText.length

                val subTransformed = formatted.substring(0, offset)
                val digitsCount = subTransformed.count { it.isDigit() }
                return digitsCount.coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
