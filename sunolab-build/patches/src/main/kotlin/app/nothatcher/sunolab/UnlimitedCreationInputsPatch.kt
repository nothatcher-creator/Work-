package app.nothatcher.sunolab

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption

private val longLimitChoices = mapOf(
    "Original" to "original",
    "10,000" to "10000",
    "25,000" to "25000",
    "50,000" to "50000",
    "100,000" to "100000",
    "1,000,000" to "1000000",
    "Unlimited (experimental)" to "unlimited",
)

private val mediumLimitChoices = mapOf(
    "Original" to "original",
    "2,000" to "2000",
    "5,000" to "5000",
    "10,000" to "10000",
    "25,000" to "25000",
    "100,000" to "100000",
    "Unlimited (experimental)" to "unlimited",
)

private fun BytecodePatchContext.patchLimit(
    selection: String?,
    originalFallback: Int,
    fingerprint: Fingerprint,
) {
    if (selection == null || selection == "original") return
    val value = normalizeLimit(selection, originalFallback)
    fingerprint.method.addInstructions(0, returnIntInstructions(value))
}

@Suppress("unused")
val sunoUnlimitedCreationInputsPatch = bytecodePatch(
    name = "Suno Lab: Unlimited creation inputs",
    description = "Raises Suno's local lyrics, style, description, negative-style, and title limits. Server-side limits still apply.",
    default = false,
) {
    compatibleWith(SUNO_191_COMPATIBILITY)

    val lyricsLimit by stringOption(
        key = "sunoLyricsLimit",
        default = "unlimited",
        values = longLimitChoices,
        title = "Lyrics character limit",
        description = "Raises the local lyrics limit. Unlimited uses Int.MAX_VALUE.",
    )

    val styleLimit by stringOption(
        key = "sunoStyleLimit",
        default = "unlimited",
        values = mediumLimitChoices,
        title = "Style character limit",
        description = "Raises the local style/tags limit.",
    )

    val descriptionLimit by stringOption(
        key = "sunoDescriptionLimit",
        default = "unlimited",
        values = longLimitChoices,
        title = "Description character limit",
        description = "Raises the local song-description prompt limit.",
    )

    val negativeStyleLimit by stringOption(
        key = "sunoNegativeStyleLimit",
        default = "unlimited",
        values = mediumLimitChoices,
        title = "Negative-style character limit",
        description = "Raises the local negative-style/tags limit.",
    )

    val titleLimit by stringOption(
        key = "sunoTitleLimit",
        default = "1000",
        values = mapOf(
            "Original" to "original",
            "250" to "250",
            "500" to "500",
            "1,000" to "1000",
            "10,000" to "10000",
            "Unlimited (experimental)" to "unlimited",
        ),
        title = "Title character limit",
        description = "Raises the local title limit.",
    )

    execute {
        patchLimit(lyricsLimit, 5000, LyricsLimitFingerprint)
        patchLimit(styleLimit, 1000, StyleLimitFingerprint)
        patchLimit(descriptionLimit, 3000, DescriptionLimitFingerprint)
        patchLimit(negativeStyleLimit, 1000, NegativeStyleLimitFingerprint)
        patchLimit(titleLimit, 100, TitleLimitFingerprint)
    }
}
