from pathlib import Path

root = Path("/tmp/roomforge/src/RoomForgeScanner-v0.5.0")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Patch target missing: {label}")
    return text.replace(old, new, 1)

gradle = root / "app/build.gradle.kts"
s = gradle.read_text()
s = s.replace("versionCode = 8", "versionCode = 9")
s = s.replace('versionName = "0.7.0"', 'versionName = "0.8.0"')
gradle.write_text(s)

tsdf = root / "app/src/main/java/com/roomforge/scanner/scanner/SparseTsdfVolume.kt"
s = tsdf.read_text()
s = replace_once(
    s,
    "    private val cells = HashMap<VoxelKey, MutableTsdf>(96_000)\n"
    "    private val glass = HashMap<VoxelKey, MutableGlass>(4_000)",
    "    private val cells = HashMap<VoxelKey, MutableTsdf>(96_000)\n"
    "    private val glass = HashMap<VoxelKey, MutableGlass>(4_000)\n"
    "    private val dirtyLiveChunks = LinkedHashSet<LiveChunkKey>()",
    "dirty chunk field",
)
s = replace_once(
    s,
    "        cells.clear()\n        glass.clear()",
    "        cells.clear()\n        glass.clear()\n        dirtyLiveChunks.clear()",
    "clear dirty chunks",
)
old_snapshot = """    @Synchronized
    fun snapshot(): Map<VoxelKey, TsdfCell> = cells.mapValues { (_, c) ->
        val inv = if (c.weight <= 1e-6f) 0f else 1f / c.weight
        val colorInv = if (c.colorWeight <= 1e-6f) 0f else 1f / c.colorWeight
        TsdfCell(
            tsdf = (c.distanceSum * inv).coerceIn(-1f, 1f),
            weight = c.weight,
            viewMask = c.viewMask,
            red = (c.redSum * colorInv).roundToInt().coerceIn(0, 255),
            green = (c.greenSum * colorInv).roundToInt().coerceIn(0, 255),
            blue = (c.blueSum * colorInv).roundToInt().coerceIn(0, 255),
            colorWeight = c.colorWeight
        )
    }
"""
new_snapshot = """    private fun immutable(c: MutableTsdf): TsdfCell {
        val inv = if (c.weight <= 1e-6f) 0f else 1f / c.weight
        val colorInv = if (c.colorWeight <= 1e-6f) 0f else 1f / c.colorWeight
        return TsdfCell(
            tsdf = (c.distanceSum * inv).coerceIn(-1f, 1f),
            weight = c.weight,
            viewMask = c.viewMask,
            red = (c.redSum * colorInv).roundToInt().coerceIn(0, 255),
            green = (c.greenSum * colorInv).roundToInt().coerceIn(0, 255),
            blue = (c.blueSum * colorInv).roundToInt().coerceIn(0, 255),
            colorWeight = c.colorWeight
        )
    }

    @Synchronized
    fun snapshot(): Map<VoxelKey, TsdfCell> = cells.mapValues { (_, c) -> immutable(c) }

    @Synchronized
    fun takeDirtyLiveChunkSnapshots(maxChunks: Int = 10): List<LiveChunkSnapshot> {
        if (dirtyLiveChunks.isEmpty()) return emptyList()

        val selected = dirtyLiveChunks.take(maxChunks.coerceAtLeast(1))
        selected.forEach { dirtyLiveChunks.remove(it) }
        val n = LiveChunkMesher.CHUNK_VOXELS
        val result = ArrayList<LiveChunkSnapshot>(selected.size)

        for (chunk in selected) {
            val minX = chunk.x * n - 1
            val minY = chunk.y * n - 1
            val minZ = chunk.z * n - 1
            val maxX = (chunk.x + 1) * n + 1
            val maxY = (chunk.y + 1) * n + 1
            val maxZ = (chunk.z + 1) * n + 1
            val local = HashMap<VoxelKey, TsdfCell>(2_048)

            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    for (x in minX..maxX) {
                        val key = VoxelKey(x, y, z)
                        cells[key]?.let { local[key] = immutable(it) }
                    }
                }
            }
            result += LiveChunkSnapshot(chunk, local)
        }
        return result
    }
"""
s = replace_once(s, old_snapshot, new_snapshot, "snapshot helper")

update_marker = """    private fun updateTsdf(
        key: VoxelKey,
        distance: Float,
        weight: Float,
        viewBit: Int,
        rgb: IntArray?,
        nearSurface: Boolean
    ) {
"""
dirty_fun = """    private fun markLiveChunkDirty(key: VoxelKey) {
        val n = LiveChunkMesher.CHUNK_VOXELS
        val cx = Math.floorDiv(key.x, n)
        val cy = Math.floorDiv(key.y, n)
        val cz = Math.floorDiv(key.z, n)
        dirtyLiveChunks += LiveChunkKey(cx, cy, cz)

        val lx = Math.floorMod(key.x, n)
        val ly = Math.floorMod(key.y, n)
        val lz = Math.floorMod(key.z, n)
        if (lx <= 1) dirtyLiveChunks += LiveChunkKey(cx - 1, cy, cz)
        if (lx >= n - 2) dirtyLiveChunks += LiveChunkKey(cx + 1, cy, cz)
        if (ly <= 1) dirtyLiveChunks += LiveChunkKey(cx, cy - 1, cz)
        if (ly >= n - 2) dirtyLiveChunks += LiveChunkKey(cx, cy + 1, cz)
        if (lz <= 1) dirtyLiveChunks += LiveChunkKey(cx, cy, cz - 1)
        if (lz >= n - 2) dirtyLiveChunks += LiveChunkKey(cx, cy, cz + 1)
    }

"""
if update_marker not in s:
    raise RuntimeError("updateTsdf marker missing")
s = s.replace(update_marker, dirty_fun + update_marker, 1)
s = replace_once(
    s,
    "            cells[key] = cell\n            return",
    "            cells[key] = cell\n            markLiveChunkDirty(key)\n            return",
    "mark new dirty",
)
s = replace_once(
    s,
    """        if (applied > 0f) {
            existing.distanceSum += distance * applied
            existing.weight += applied
        }
        existing.viewMask = existing.viewMask or viewBit
""",
    """        if (applied > 0f) {
            existing.distanceSum += distance * applied
            existing.weight += applied
            markLiveChunkDirty(key)
        }
        existing.viewMask = existing.viewMask or viewBit
""",
    "mark existing dirty",
)
tsdf.write_text(s)

scanner = root / "app/src/main/java/com/roomforge/scanner/scanner/DepthRoomScanner.kt"
s = scanner.read_text()
s = replace_once(
    s,
    "    private val liveMeshState = AtomicReference(LiveMeshFrame.EMPTY)\n"
    "    private val liveMeshRevision = AtomicLong(0L)",
    "    private val liveMeshState = AtomicReference(LiveMeshFrame.EMPTY)\n"
    "    private val liveMeshRevision = AtomicLong(0L)\n"
    "    private val liveChunkMeshes = java.util.concurrent.ConcurrentHashMap<LiveChunkKey, LiveChunkMesh>()",
    "chunk mesh map",
)
s = replace_once(
    s,
    "        liveMeshState.set(LiveMeshFrame(revision = liveMeshRevision.incrementAndGet()))\n"
    "        lastTranslation = null",
    "        liveChunkMeshes.clear()\n"
    "        liveMeshState.set(LiveMeshFrame(revision = liveMeshRevision.incrementAndGet()))\n"
    "        lastTranslation = null",
    "clear chunk meshes",
)
start = s.index("    private fun scheduleLiveMesh(generationAtSchedule: Long) {")
end = s.index("\n    fun save(repository: ScanRepository, name: String): ScanRecord {", start)
if start < 0 or end < 0:
    raise RuntimeError("scheduleLiveMesh bounds missing")
new_method = r'''    private fun scheduleLiveMesh(generationAtSchedule: Long) {
        val current = state.get()
        if (current.framesIntegrated < 1) return

        val now = System.nanoTime()
        val intervalNanos = if (current.voxelCount < 55_000) 110_000_000L else 180_000_000L
        if (now - lastLiveMeshRequestNanos < intervalNanos) return
        if (!liveMeshBusy.compareAndSet(false, true)) return
        lastLiveMeshRequestNanos = now

        val dirty = try {
            grid.takeDirtyLiveChunkSnapshots(maxChunks = if (current.voxelCount < 75_000) 10 else 6)
        } catch (_: Throwable) {
            liveMeshBusy.set(false)
            return
        }
        if (dirty.isEmpty()) {
            liveMeshBusy.set(false)
            return
        }

        liveMeshWorker.execute {
            try {
                if (generation.get() != generationAtSchedule) return@execute

                for (snapshot in dirty) {
                    if (generation.get() != generationAtSchedule) return@execute
                    val mesh = LiveChunkMesher.build(snapshot, grid.voxelSizeMeters)
                    if (mesh.triangleCount <= 0) {
                        liveChunkMeshes.remove(snapshot.key)
                    } else {
                        liveChunkMeshes[snapshot.key] = mesh
                    }
                }

                val revision = liveMeshRevision.incrementAndGet()
                val frame = LiveChunkMesher.combine(liveChunkMeshes.values, revision)
                if (generation.get() == generationAtSchedule) liveMeshState.set(frame)
            } catch (_: Throwable) {
                // A failed preview chunk must never stop scanning.
            } finally {
                liveMeshBusy.set(false)
            }
        }
    }
'''
s = s[:start] + new_method + s[end:]
scanner.write_text(s)

renderer = root / "app/src/main/java/com/roomforge/scanner/ui/LiveScanMeshNode.kt"
s = renderer.read_text()
if "import com.roomforge.scanner.scanner.LiveChunkMesher" not in s:
    s = s.replace(
        "import com.roomforge.scanner.scanner.LiveMeshFrame\n",
        "import com.roomforge.scanner.scanner.LiveMeshFrame\n"
        "import com.roomforge.scanner.scanner.LiveChunkMesher\n",
    )
s = s.replace(
    "private val maxVertices = LiveTsdfPreviewMesher.MAX_TRIANGLES * 3",
    "private val maxVertices = LiveChunkMesher.MAX_TOTAL_TRIANGLES * 3",
)
s = s.replace("Color(0.08f, 0.78f, 1.0f, 0.34f)", "Color(0.10f, 0.78f, 1.0f, 0.58f)")
renderer.write_text(s)

ui = root / "app/src/main/java/com/roomforge/scanner/ui/RoomForgeApp.kt"
s = ui.read_text()
s = s.replace(
    '"\${liveMesh.triangleCount} preview triangles • updates as you move"',
    '"\${liveMesh.triangleCount} live triangles • \${liveMesh.sourceCellCount} active mesh chunks"',
)
s = s.replace("Building the first live 3D chunk…", "Building the first live 3D chunk…")
ui.write_text(s)
