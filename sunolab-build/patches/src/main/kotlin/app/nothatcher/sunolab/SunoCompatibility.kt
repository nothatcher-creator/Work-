package app.nothatcher.sunolab

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal val SUNO_191_COMPATIBILITY = Compatibility(
    name = "Suno",
    packageName = "com.suno.android",
    apkFileType = ApkFileType.XAPK,
    appIconColor = 0xFF6B35,
    targets = listOf(
        AppTarget(version = "1.91.0", versionCode = 437),
    ),
)
