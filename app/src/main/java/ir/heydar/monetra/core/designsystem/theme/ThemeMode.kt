package ir.heydar.monetra.core.designsystem.theme

/** How the app picks between the light and dark color schemes. */
enum class ThemeMode(val key: String) {
    SYSTEM(""),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
