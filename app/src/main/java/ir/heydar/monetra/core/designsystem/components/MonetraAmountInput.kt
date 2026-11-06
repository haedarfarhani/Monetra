package ir.heydar.monetra.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.core.text.TextNormalizer
import ir.heydar.monetra.domain.model.TransactionType
import java.util.Locale

/**
 * Large amount field for fast entry.
 *
 * The state always holds plain Latin digits (optionally with a `.` decimal part), while the
 * displayed text is grouped by thousands with a [VisualTransformation]. Keeping formatting out of
 * the state avoids the cursor jumps and dropped digits that happen when a field rewrites its own
 * value on every keystroke.
 */
@Composable
fun MonetraAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    currencyCode: String,
    transactionType: TransactionType,
    modifier: Modifier = Modifier,
    fractionDigits: Int = 2,
    locale: Locale = Locale.getDefault(),
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val extendedColors = MaterialTheme.monetraColors
    val amountColor: Color = when (transactionType) {
        TransactionType.INCOME -> extendedColors.income
        TransactionType.EXPENSE -> extendedColors.expense
        TransactionType.TRANSFER -> extendedColors.transfer
    }

    val isPersian = locale.language == "fa"
    val groupingSeparator = if (isPersian) '\u066C' else ','
    val visualTransformation = remember(groupingSeparator, isPersian) {
        GroupedAmountVisualTransformation(
            groupingSeparator = groupingSeparator,
            localizeDigits = isPersian
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacingMd),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currencyCode,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSm))

        BasicTextField(
            value = value,
            onValueChange = { raw ->
                onValueChange(
                    sanitizeAmountInput(
                        raw = raw,
                        fractionDigits = fractionDigits,
                        groupingSeparator = groupingSeparator
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.displayLarge.copy(
                color = if (isError) MaterialTheme.colorScheme.error else amountColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (fractionDigits > 0) KeyboardType.Decimal else KeyboardType.Number
            ),
            singleLine = true,
            cursorBrush = SolidColor(amountColor),
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = if (isPersian) "۰" else "0",
                        style = MaterialTheme.typography.displayLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        )

        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Persian/Arabic digits become Latin, separators are dropped and decimals are capped. */
private fun sanitizeAmountInput(
    raw: String,
    fractionDigits: Int,
    groupingSeparator: Char
): String {
    val normalized = TextNormalizer.normalize(raw)
    val cleaned = normalized
        .replace(groupingSeparator.toString(), "")
        .replace(",", "")
        .replace(" ", "")
        .filter { it.isDigit() || it == '.' }

    val parts = cleaned.split('.')
    var integerPart = parts.firstOrNull()?.filter { it.isDigit() }.orEmpty().take(15)
    val wantedDecimal = fractionDigits > 0 && cleaned.contains('.')
    val decimalPart = if (wantedDecimal) {
        parts.getOrNull(1)?.filter { it.isDigit() }.orEmpty().take(fractionDigits)
    } else {
        ""
    }

    if (wantedDecimal && integerPart.isEmpty()) {
        integerPart = "0"
    }

    return if (wantedDecimal) "$integerPart.$decimalPart" else integerPart
}

/**
 * Inserts the locale's thousands separator into the integer part without changing the underlying
 * value. Offsets are mapped so the caret stays between the same digits while typing.
 */
private class GroupedAmountVisualTransformation(
    private val groupingSeparator: Char,
    private val localizeDigits: Boolean
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val dotIndex = raw.indexOf('.')
        val integerPart = if (dotIndex >= 0) raw.substring(0, dotIndex) else raw
        val decimalPart = if (dotIndex >= 0) raw.substring(dotIndex) else ""

        val groupedInteger = groupDigits(integerPart)
        val transformedText = buildString(groupedInteger.length + decimalPart.length) {
            groupedInteger.forEach { char -> append(localize(char)) }
            decimalPart.forEach { char -> append(localize(char)) }
        }

        val integerLength = integerPart.length
        val groupedIntegerLength = groupedInteger.length
        val firstGroupSize = when {
            integerLength == 0 -> 0
            integerLength % 3 == 0 -> 3
            else -> integerLength % 3
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= integerLength) {
                    val separators = if (offset <= firstGroupSize) {
                        0
                    } else {
                        1 + (offset - firstGroupSize - 1) / 3
                    }
                    return offset + separators
                }
                return groupedIntegerLength + (offset - integerLength)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= groupedIntegerLength) {
                    val separators = groupedInteger.take(offset).count { it == groupingSeparator }
                    return offset - separators
                }
                return integerLength + (offset - groupedIntegerLength)
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }

    private fun groupDigits(integerPart: String): String {
        if (integerPart.isEmpty()) return ""
        val firstGroup = integerPart.length % 3
        val firstGroupSize = if (firstGroup == 0) 3 else firstGroup
        return buildString(integerPart.length + integerPart.length / 3) {
            integerPart.forEachIndexed { index, char ->
                if (index >= firstGroupSize && (index - firstGroupSize) % 3 == 0) {
                    append(groupingSeparator)
                }
                append(char)
            }
        }
    }

    private fun localize(char: Char): Char {
        if (!localizeDigits || char !in '0'..'9') return char
        return '\u06F0' + (char - '0')
    }
}
