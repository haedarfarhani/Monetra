package ir.heydar.monetra.core.text

/**
 * Normalizes bank messages before parsing: Persian/Arabic digits become Latin, Arabic yeh/kaf
 * become Persian, zero-width and direction marks become spaces and whitespace is collapsed.
 * Thousand separators are preserved as `,` so amounts can be read unambiguously.
 */
object TextNormalizer {

    private val whitespace = Regex("""\s+""")
    private val thousandSeparators = Regex("""[\u066C\u2019\u2018]""")

    fun normalize(input: String): String = buildString(input.length) {
        input.forEach { char ->
            when {
                char in '\u06F0'..'\u06F9' -> append('0' + (char - '\u06F0'))
                char in '\u0660'..'\u0669' -> append('0' + (char - '\u0660'))
                char == 'ي' -> append('ی')
                char == 'ك' -> append('ک')
                char == '\u200C' || char == '\u200E' || char == '\u200F' -> append(' ')
                else -> append(char)
            }
        }
    }.let { converted ->
        thousandSeparators.replace(converted, ",")
            .replace(whitespace, " ")
            .trim()
    }

    /** Digits only, used where a numeric sequence is expected. */
    fun digitsOf(input: String): String = input.filter { it in '0'..'9' }
}
