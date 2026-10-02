package app.nothatcher.sunolab

internal fun normalizeLimit(selection: String?, original: Int): Int = when (selection) {
    null, "original" -> original
    "unlimited" -> Int.MAX_VALUE
    else -> selection.toIntOrNull()?.takeIf { it > 0 } ?: original
}

internal fun returnIntInstructions(value: Int): String = if (value == Int.MAX_VALUE) {
    "const v0, 0x7fffffff\nreturn v0"
} else {
    "const v0, $value\nreturn v0"
}
