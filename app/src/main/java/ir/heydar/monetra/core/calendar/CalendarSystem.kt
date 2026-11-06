package ir.heydar.monetra.core.calendar

/**
 * Calendar used for *presentation only*. Transaction timestamps are always stored as epoch
 * milliseconds, so changing this value never rewrites stored dates.
 */
enum class CalendarSystem(val key: String) {
    GREGORIAN("gregorian"),
    JALALI("jalali");

    companion object {
        fun fromKey(key: String?): CalendarSystem =
            entries.firstOrNull { it.key == key } ?: GREGORIAN
    }
}
