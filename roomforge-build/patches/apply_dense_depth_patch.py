from pathlib import Path

root = Path("/tmp/roomforge/src/RoomForgeScanner-v0.5.0")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Patch target missing: {label}")
    return text.replace(old, new, 1)

# Version bump after the v0.6.1 live-mesh patch.
gradle = root / "app/build.gradle.kts"
s = gradle.read_text()
s = s.replace("versionCode = 7", "versionCode = 8")
s = s.replace('versionName = "0.6.1"', 'versionName = "0.7.0"')
gradle.write_text(s)

# DepthPacket can now distinguish dense AUTOMATIC depth from sparse Raw Depth,
# while still carrying Raw Depth/confidence as a secondary glass signal.
voxel = root / "app/src/main/java/com/roomforge/scanner/scanner/VoxelGrid.kt"
s = voxel.read_text()
s = replace_once(
    s,
    "    val confidencePixelStride: Int,\n"
    "    val fx: Float,",
    "    val confidencePixelStride: Int,\n"
    "    val isDenseDepth: Boolean = false,\n"
    "    val rawDepthBytes: ByteArray = ByteArray(0),\n"
    "    val rawDepthRowStride: Int = 0,\n"
    "    val rawDepthPixelStride: Int = 0,\n"
    "    val rawConfidenceBytes: ByteArray = ByteArray(0),\n"
    "    val rawConfidenceRowStride: Int = 0,\n"
    "    val rawConfidencePixelStride: Int = 0,\n"
    "    val fx: Float,",
    "DepthPacket dense/raw fields",
)
voxel.write_text(s)

# Dense depth is the primary geometry source. Raw Depth remains useful for transparent-surface evidence.
tsdf = root / "app/src/main/java/com/roomforge/scanner/scanner/SparseTsdfVolume.kt"
s = tsdf.read_text()
s = replace_once(
    s,
    "                val confidence = confidenceAt(packet, x, y)\n"
    "                var acceptedThisPixel = false",
    "                val confidence = if (packet.isDenseDepth) 220 else confidenceAt(packet, x, y)\n"
    "                var acceptedThisPixel = false",
    "dense confidence",
)
s = replace_once(
    s,
    """                    val cutoff = when {
                        depth >= 4.0f -> 156
                        depth >= 3.0f -> 140
                        else -> 128
                    }
                    val edgeRisk = isDepthDiscontinuity(packet, x, y, mm)
                    val required = if (edgeRisk) 205 else cutoff
                    if (confidence >= required && depth in 0.5f..5.0f) {
                        val rgb = packet.color?.let { sampleColor(it, x, y, packet.width, packet.height) }
                        val baseWeight = (confidence / 255f).coerceIn(0.35f, 1f) * if (edgeRisk) 0.72f else 1f
""",
    """                    val edgeRisk = isDepthDiscontinuity(packet, x, y, mm)
                    val cutoff = when {
                        depth >= 4.0f -> 156
                        depth >= 3.0f -> 140
                        else -> 128
                    }
                    val required = if (edgeRisk) 205 else cutoff
                    val inRange = if (packet.isDenseDepth) depth in 0.28f..8.0f else depth in 0.5f..5.0f
                    if (inRange && (packet.isDenseDepth || confidence >= required)) {
                        val rgb = packet.color?.let { sampleColor(it, x, y, packet.width, packet.height) }
                        val baseWeight = if (packet.isDenseDepth) {
                            if (edgeRisk) 0.38f else 0.70f
                        } else {
                            (confidence / 255f).coerceIn(0.35f, 1f) * if (edgeRisk) 0.72f else 1f
                        }
""",
    "dense acceptance",
)
s = replace_once(
    s,
    """                // Weak/missing raw depth can be a useful *signal* for glass only when nearby depth
                // supports a coherent plane and the camera patch is visually non-uniform/specular.
                if (!acceptedThisPixel && packet.color != null && (mm == 0 || confidence < 110)) {
                    accumulateGlassEvidence(packet, x, y, step, confidence, viewBit)
                }
""",
    """                // Dense depth builds geometry immediately. Raw depth/confidence is retained as
                // an independent signal for glass because transparent surfaces often lose raw depth.
                if (packet.color != null && packet.rawDepthBytes.isNotEmpty()) {
                    val rawMm = rawDepthMm(packet, x, y)
                    val rawConfidence = rawConfidenceAt(packet, x, y)
                    if (rawMm == 0 || rawConfidence < 110) {
                        accumulateGlassEvidence(packet, x, y, step, rawConfidence, viewBit)
                    }
                }
""",
    "glass raw signal",
)
s = s.replace("            val nmm = depthMm(packet, nx, ny)\n            val nc = confidenceAt(packet, nx, ny)",
              "            val nmm = rawDepthMm(packet, nx, ny)\n            val nc = rawConfidenceAt(packet, nx, ny)")
insert = '''
    private fun rawDepthMm(packet: DepthPacket, x: Int, y: Int): Int {
        if (packet.rawDepthBytes.isEmpty() || x !in 0 until packet.width || y !in 0 until packet.height) return 0
        val offset = y * packet.rawDepthRowStride + x * packet.rawDepthPixelStride
        if (offset < 0 || offset + 1 >= packet.rawDepthBytes.size) return 0
        return (packet.rawDepthBytes[offset].toInt() and 0xff) or
            ((packet.rawDepthBytes[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun rawConfidenceAt(packet: DepthPacket, x: Int, y: Int): Int {
        if (packet.rawConfidenceBytes.isEmpty() || x !in 0 until packet.width || y !in 0 until packet.height) return 0
        val offset = y * packet.rawConfidenceRowStride + x * packet.rawConfidencePixelStride
        if (offset !in packet.rawConfidenceBytes.indices) return 0
        return packet.rawConfidenceBytes[offset].toInt() and 0xff
    }

'''
s = replace_once(
    s,
    "    private fun depthMm(packet: DepthPacket, x: Int, y: Int): Int {",
    insert + "    private fun depthMm(packet: DepthPacket, x: Int, y: Int): Int {",
    "raw depth helpers",
)
tsdf.write_text(s)

# Scanner: acquire full AUTOMATIC depth for geometry; try Raw Depth alongside it for glass.
scanner = root / "app/src/main/java/com/roomforge/scanner/scanner/DepthRoomScanner.kt"
s = scanner.read_text()
start = s.index("        var depth: Image? = null\n")
end = s.index("            val pathIncrement =", start)
old = s[start:end]
new = '''        var depth: Image? = null
        var rawDepth: Image? = null
        var confidence: Image? = null
        try {
            var denseDepth = true
            depth = try {
                frame.acquireDepthImage16Bits()
            } catch (_: Throwable) {
                denseDepth = false
                null
            }

            if (depth == null) {
                rawDepth = frame.acquireRawDepthImage16Bits()
                depth = rawDepth
                confidence = frame.acquireRawDepthConfidenceImage()
                if (depth.timestamp == lastDepthTimestamp.get()) return
                lastDepthTimestamp.set(depth.timestamp)
            } else {
                // Raw depth is optional in AUTOMATIC mode. Keep it only as a secondary
                // confidence/glass cue; dense depth drives the visible reconstruction.
                runCatching {
                    rawDepth = frame.acquireRawDepthImage16Bits()
                    confidence = frame.acquireRawDepthConfidenceImage()
                }
                lastDepthTimestamp.set(depth.timestamp)
            }

            val primaryDepth = depth ?: return
            val depthPlane = primaryDepth.planes[0]

            fun copyPlaneBytes(plane: Image.Plane): ByteArray = plane.buffer.duplicate().run {
                rewind()
                val bytes = ByteArray(remaining())
                get(bytes)
                bytes
            }

            val depthBytes = copyPlaneBytes(depthPlane)
            val confidencePlane = confidence?.planes?.firstOrNull()
            val confidenceBytes = confidencePlane?.let(::copyPlaneBytes) ?: ByteArray(0)

            val rawDepthPlane = rawDepth?.planes?.firstOrNull()
            val rawDepthBytes = when {
                rawDepthPlane == null -> ByteArray(0)
                rawDepth === primaryDepth -> depthBytes
                else -> copyPlaneBytes(rawDepthPlane)
            }
            val rawConfidenceBytes = confidenceBytes

            val intrinsics = frame.camera.textureIntrinsics
            val intrinsicSize = intrinsics.imageDimensions
            val focal = intrinsics.focalLength
            val principal = intrinsics.principalPoint
            val width = primaryDepth.width
            val height = primaryDepth.height
            val cameraToWorld = FloatArray(16).also { pose.toMatrix(it, 0) }
            val viewSector = viewSectorForPose(pose)
            val colorPacket = if (state.get().framesIntegrated % 2 == 0) acquireColorPacket(frame) else null

            val packet = DepthPacket(
                width = width,
                height = height,
                depthBytes = depthBytes,
                depthRowStride = depthPlane.rowStride,
                depthPixelStride = depthPlane.pixelStride,
                confidenceBytes = confidenceBytes,
                confidenceRowStride = confidencePlane?.rowStride ?: 0,
                confidencePixelStride = confidencePlane?.pixelStride ?: 0,
                isDenseDepth = denseDepth,
                rawDepthBytes = rawDepthBytes,
                rawDepthRowStride = rawDepthPlane?.rowStride ?: 0,
                rawDepthPixelStride = rawDepthPlane?.pixelStride ?: 0,
                rawConfidenceBytes = rawConfidenceBytes,
                rawConfidenceRowStride = confidencePlane?.rowStride ?: 0,
                rawConfidencePixelStride = confidencePlane?.pixelStride ?: 0,
                fx = focal[0] * width / intrinsicSize[0].toFloat(),
                fy = focal[1] * height / intrinsicSize[1].toFloat(),
                cx = principal[0] * width / intrinsicSize[0].toFloat(),
                cy = principal[1] * height / intrinsicSize[1].toFloat(),
                cameraToWorld = cameraToWorld,
                viewSector = viewSector,
                color = colorPacket
            )

'''
s = s[:start] + new + s[end:]
s = s.replace(
    'result.acceptedPoints < 120 || result.depthYieldPercent < 5 -> "Weak depth — move closer and aim across textured edges"',
    'result.acceptedPoints < 120 || result.depthYieldPercent < 5 -> "Waiting for usable depth — move slowly and keep walls or furniture in view"'
)
s = s.replace(
    "        if (current.framesIntegrated < 3) return",
    "        if (current.framesIntegrated < 1) return"
)
s = s.replace(
    "        val intervalNanos = if (current.voxelCount < 45_000) 550_000_000L else 850_000_000L",
    "        val intervalNanos = if (current.voxelCount < 45_000) 180_000_000L else 320_000_000L"
)
s = s.replace(
    "            depth?.close()\n            confidence?.close()",
    "            depth?.close()\n            if (rawDepth !== depth) rawDepth?.close()\n            confidence?.close()"
)
scanner.write_text(s)

# Live preview: permit a one-frame surface instead of requiring repeated Raw Depth views.
mesher = root / "app/src/main/java/com/roomforge/scanner/scanner/LiveTsdfPreviewMesher.kt"
s = mesher.read_text()
s = s.replace("const val MAX_TRIANGLES = 3_000", "const val MAX_TRIANGLES = 5_000")
s = s.replace("private const val MAX_SOURCE_CELLS = 6_000", "private const val MAX_SOURCE_CELLS = 9_000")
s = s.replace(
    """        fun isPreviewSurface(cell: TsdfCell?): Boolean = cell != null &&
            cell.weight >= 0.62f &&
            abs(cell.tsdf) <= 0.58f &&
            (cell.viewCount >= 2 || cell.weight >= 1.45f)
""",
    """        fun isPreviewSurface(cell: TsdfCell?): Boolean = cell != null &&
            cell.weight >= 0.28f &&
            abs(cell.tsdf) <= 0.78f
"""
)
s = s.replace("if (surfaceEntries.size < 36)", "if (surfaceEntries.size < 8)")
mesher.write_text(s)

# UI: use AUTOMATIC Depth when available, hide ARCore's plane grid, and show the model from frame one.
ui = root / "app/src/main/java/com/roomforge/scanner/ui/RoomForgeApp.kt"
s = ui.read_text()
s = replace_once(
    s,
    "            planeRenderer = true,\n"
    "            sessionConfiguration = { session, config ->\n"
    "                val supported = session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)\n"
    "                depthSupported = supported\n"
    "                config.depthMode = if (supported) Config.DepthMode.RAW_DEPTH_ONLY else Config.DepthMode.DISABLED",
    "            planeRenderer = false,\n"
    "            sessionConfiguration = { session, config ->\n"
    "                val automatic = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)\n"
    "                val raw = session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)\n"
    "                depthSupported = automatic || raw\n"
    "                config.depthMode = when {\n"
    "                    automatic -> Config.DepthMode.AUTOMATIC\n"
    "                    raw -> Config.DepthMode.RAW_DEPTH_ONLY\n"
    "                    else -> Config.DepthMode.DISABLED\n"
    "                }",
    "scan depth mode",
)
s = s.replace(
    'if (showLiveMesh && stats.framesIntegrated >= 3 && liveMesh.triangleCount > 0)',
    'if (showLiveMesh && stats.framesIntegrated >= 1 && liveMesh.triangleCount > 0)'
)
s = s.replace('false -> "Raw Depth is not available on this phone/camera configuration."',
              'false -> "ARCore Depth is not available on this phone/camera configuration."')
s = s.replace('null -> "Checking Raw Depth support…"', 'null -> "Checking ARCore Depth support…"')
s = s.replace("Live mesh starts after tracking settles…", "Waiting for the first depth surface…")
s = s.replace(
    'stats.framesIntegrated == 0 -> "Start near one wall, then walk the perimeter. Keep the camera moving slowly instead of standing in one spot."',
    'stats.framesIntegrated == 0 -> "Start near a wall or piece of furniture. The blue 3D surface should begin appearing almost immediately as depth arrives."'
)
ui.write_text(s)
