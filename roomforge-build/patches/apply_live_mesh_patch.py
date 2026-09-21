from pathlib import Path

root = Path("/tmp/roomforge/src/RoomForgeScanner-v0.5.0")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Patch target missing: {label}")
    return text.replace(old, new, 1)

gradle = root / "app/build.gradle.kts"
s = gradle.read_text()
s = s.replace("versionCode = 5", "versionCode = 7")
s = s.replace('versionName = "0.5.0"', 'versionName = "0.6.1"')
gradle.write_text(s)

scanner = root / "app/src/main/java/com/roomforge/scanner/scanner/DepthRoomScanner.kt"
s = scanner.read_text()
s = replace_once(
    s,
    "    private val worker = Executors.newSingleThreadExecutor()\n    private val scanning = AtomicBoolean(false)",
    "    private val worker = Executors.newSingleThreadExecutor()\n"
    "    private val liveMeshWorker = Executors.newSingleThreadExecutor()\n"
    "    private val liveMeshBusy = AtomicBoolean(false)\n"
    "    private val liveMeshState = AtomicReference(LiveMeshFrame.EMPTY)\n"
    "    private val liveMeshRevision = AtomicLong(0L)\n"
    "    private val scanning = AtomicBoolean(false)",
    "scanner workers",
)
s = replace_once(
    s,
    "    private var lastScheduleNanos = 0L\n    private var lastIntegratedNanos = 0L",
    "    private var lastScheduleNanos = 0L\n"
    "    private var lastIntegratedNanos = 0L\n"
    "    private var lastLiveMeshRequestNanos = 0L",
    "scanner timing",
)
s = replace_once(
    s,
    "        lastIntegratedNanos = 0L\n        lastTranslation = null",
    "        lastIntegratedNanos = 0L\n"
    "        lastLiveMeshRequestNanos = 0L\n"
    "        liveMeshState.set(LiveMeshFrame(revision = liveMeshRevision.incrementAndGet()))\n"
    "        lastTranslation = null",
    "scanner reset",
)
s = replace_once(
    s,
    "    fun stats(): ScannerStats = state.get()\n\n    fun onFrame(frame: Frame) {",
    "    fun stats(): ScannerStats = state.get()\n\n"
    "    fun liveMesh(): LiveMeshFrame = liveMeshState.get()\n\n"
    "    fun onFrame(frame: Frame) {",
    "scanner live getter",
)
s = replace_once(
    s,
    "                    lastTranslation = translation\n"
    "                    lastForward = forward\n"
    "                    lastIntegratedNanos = now",
    "                    lastTranslation = translation\n"
    "                    lastForward = forward\n"
    "                    lastIntegratedNanos = now\n"
    "                    scheduleLiveMesh(generationAtSchedule)",
    "scanner schedule call",
)
live_method = r'''
    private fun scheduleLiveMesh(generationAtSchedule: Long) {
        val current = state.get()
        if (current.framesIntegrated < 3) return

        val now = System.nanoTime()
        val intervalNanos = if (current.voxelCount < 45_000) 550_000_000L else 850_000_000L
        if (now - lastLiveMeshRequestNanos < intervalNanos) return
        if (!liveMeshBusy.compareAndSet(false, true)) return
        lastLiveMeshRequestNanos = now

        // This method is called on the scanner worker immediately after integration. Snapshot here,
        // before handing immutable data to the mesher, so the TSDF map is never read concurrently
        // while the next depth frame mutates it.
        val snapshot = try {
            grid.snapshot()
        } catch (_: Throwable) {
            liveMeshBusy.set(false)
            return
        }

        liveMeshWorker.execute {
            try {
                if (generation.get() != generationAtSchedule) return@execute
                val revision = liveMeshRevision.incrementAndGet()
                val mesh = LiveTsdfPreviewMesher.build(snapshot, grid.voxelSizeMeters, revision)
                if (generation.get() == generationAtSchedule) liveMeshState.set(mesh)
            } catch (_: Throwable) {
                // Live visualization is optional. Never stop depth fusion if preview meshing fails.
            } finally {
                liveMeshBusy.set(false)
            }
        }
    }
'''
s = replace_once(
    s,
    "\n    fun save(repository: ScanRepository, name: String): ScanRecord {",
    live_method + "\n    fun save(repository: ScanRepository, name: String): ScanRecord {",
    "scanner live method",
)
s = replace_once(
    s,
    "    override fun close() {\n        scanning.set(false)\n        worker.shutdownNow()\n    }",
    "    override fun close() {\n"
    "        scanning.set(false)\n"
    "        worker.shutdownNow()\n"
    "        liveMeshWorker.shutdownNow()\n"
    "    }",
    "scanner close",
)
scanner.write_text(s)

ui = root / "app/src/main/java/com/roomforge/scanner/ui/RoomForgeApp.kt"
s = ui.read_text()
if "import com.roomforge.scanner.scanner.LiveMeshFrame" not in s:
    s = replace_once(
        s,
        "import com.roomforge.scanner.scanner.DepthRoomScanner\n",
        "import com.roomforge.scanner.scanner.DepthRoomScanner\n"
        "import com.roomforge.scanner.scanner.LiveMeshFrame\n",
        "ui live import",
    )
s = replace_once(
    s,
    "    var stats by remember { mutableStateOf(ScannerStats()) }\n"
    "    var saving by remember { mutableStateOf(false) }",
    "    var stats by remember { mutableStateOf(ScannerStats()) }\n"
    "    var liveMesh by remember { mutableStateOf(LiveMeshFrame.EMPTY) }\n"
    "    var showLiveMesh by remember { mutableStateOf(true) }\n"
    "    var saving by remember { mutableStateOf(false) }",
    "ui live state",
)
s = replace_once(
    s,
    "        while (true) {\n"
    "            stats = scanner.stats()\n"
    "            delay(250)\n"
    "        }",
    "        while (true) {\n"
    "            stats = scanner.stats()\n"
    "            val latestMesh = scanner.liveMesh()\n"
    "            if (latestMesh.revision != liveMesh.revision) liveMesh = latestMesh\n"
    "            delay(120)\n"
    "        }",
    "ui polling",
)
s = replace_once(
    s,
    "            onSessionUpdated = { _, frame ->\n"
    "                if (depthSupported == true) scanner.onFrame(frame)\n"
    "            }\n"
    "        )",
    "            onSessionUpdated = { _, frame ->\n"
    "                if (depthSupported == true) scanner.onFrame(frame)\n"
    "            }\n"
    "        ) {\n"
    "            if (showLiveMesh && liveMesh.triangleCount > 0) {\n"
    "                LiveScanMeshNode(liveMesh)\n"
    "            }\n"
    "        }",
    "ui AR content",
)
toggle = r'''                if (stats.framesIntegrated > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Live AR mesh", style = MaterialTheme.typography.labelLarge)
                            Text(
                                if (liveMesh.triangleCount > 0) "${liveMesh.triangleCount} preview triangles • updates as you move" else "Live mesh starts after tracking settles…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showLiveMesh,
                            onCheckedChange = { showLiveMesh = it }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
'''
s = replace_once(
    s,
    "                Text(\n"
    "                    scanTip(stats),\n"
    "                    style = MaterialTheme.typography.bodySmall\n"
    "                )",
    toggle
    + "                Text(\n"
    + "                    scanTip(stats),\n"
    + "                    style = MaterialTheme.typography.bodySmall\n"
    + "                )",
    "ui live toggle",
)
ui.write_text(s)
