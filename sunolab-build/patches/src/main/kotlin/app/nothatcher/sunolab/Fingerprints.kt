package app.nothatcher.sunolab

import app.morphe.patcher.Fingerprint

private const val MODEL_INPUT_LIMITS = "Lcom/suno/android/common_data/billing/models/ModelInputLimits;"

internal val LyricsLimitFingerprint = Fingerprint(
    definingClass = MODEL_INPUT_LIMITS,
    name = "getPrompt",
    returnType = "I",
    parameters = emptyList(),
)

internal val StyleLimitFingerprint = Fingerprint(
    definingClass = MODEL_INPUT_LIMITS,
    name = "getTags",
    returnType = "I",
    parameters = emptyList(),
)

internal val DescriptionLimitFingerprint = Fingerprint(
    definingClass = MODEL_INPUT_LIMITS,
    name = "getGptDescriptionPrompt",
    returnType = "I",
    parameters = emptyList(),
)

internal val NegativeStyleLimitFingerprint = Fingerprint(
    definingClass = MODEL_INPUT_LIMITS,
    name = "getNegativeTags",
    returnType = "I",
    parameters = emptyList(),
)

internal val TitleLimitFingerprint = Fingerprint(
    definingClass = MODEL_INPUT_LIMITS,
    name = "getTitle",
    returnType = "I",
    parameters = emptyList(),
)
