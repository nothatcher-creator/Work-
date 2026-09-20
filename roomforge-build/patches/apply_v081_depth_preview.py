from pathlib import Path

root = Path("/tmp/roomforge/src/RoomForgeScanner-v0.5.0")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Patch target missing: {label}")
    return text.replace(old, new, 1)

gradle = root / "app/build.gradle.kts"
s = gradle.read_text()
s = s.replace("versionCode = 9", "versionCode = 10")
s = s.replace('versionName = "0.8.0"', 'versionName = "0.8.1"')
gradle.write_text(s)

scanner = root / "app/src/main/java/com/roomforge/scanner/scanner/DepthRoomScanner.kt"
s = scanner.read_text()
s = replace_once(
    s,
    "    private val liveChunkMeshes = java.util.concurrent.ConcurrentHashMap<LiveChunkKey, LiveChunkMesh>()",
    "    private val liveDepthPatches = java.util.concurrent.ConcurrentHashMap<LiveDepthPatchKey, LiveDepthPatch>()",
    "preview patch map",
)
s = replace_once(
    s,
    "        liveChunkMeshes.clear()",
    "        liveDepthPatches.clear()",
    "clear preview patches",
)
s = replace_once(
    s,
    "                    scheduleLiveMesh(generationAtSchedule)",
    "                    scheduleLiveMesh(generationAtSchedule, packet)",
    "preview scheduling call",
)

start = s.index("    private fun scheduleLiveMesh(generationAtSchedule: Long) {")
end = s.index("\n    fun save(repository: ScanRepository, name: String): ScanRecord {", start)
if start < 0 or end < 0:
    raise RuntimeError("scheduleLiveMesh bounds missing")
new_method = r'''    private fun scheduleLiveMesh(generationAtSchedule: Long, packet: DepthPacket) {
        val current = state.get()
        if (current.framesIntegrated < 1) return

        val now = System.nanoTime()
        val intervalNanos = if (current.framesIntegrated < 12) 120_000_000L else 190_000_000L
        if (now - lastLiveMeshRequestNanos < intervalNanos) return
        if (!liveMeshBusy.compareAndSet(false, true)) return
        lastLiveMeshRequestNanos = now

        liveMeshWorker.execute {
            try {
                if (generation.get() != generationAtSchedule) return@execute

                val patch = LiveDepthPreviewMesher.build(packet)
                if (patch.triangleCount > 0) {
                    liveDepthPatches[patch.key] = patch
                }

                while (liveDepthPatches.size > LiveDepthPreviewMesher.MAX_PATCHES) {
                    val first = liveDepthPatches.keys.firstOrNull() ?: break
                    liveDepthPatches.remove(first)
                }

                val revision = liveMeshRevision.incrementAndGet()
                val frame = LiveDepthPreviewMesher.combine(liveDepthPatches.values, revision)
                if (generation.get() == generationAtSchedule) {
                    liveMeshState.set(frame)
                }
            } catch (_: Throwable) {
                // Live preview failure must never interrupt the final TSDF scan.
            } finally {
                liveMeshBusy.set(false)
            }
        }
    }
'''
s = s[:start] + new_method + s[end:]
scanner.write_text(s)

tsdf = root / "app/src/main/java/com/roomforge/scanner/scanner/SparseTsdfVolume.kt"
s = tsdf.read_text()
start = s.find("    private fun markLiveChunkDirty(key: VoxelKey) {")
end = s.find("\n    private fun updateTsdf(", start)
if start >= 0 and end > start:
    s = s[:start] + "    private fun markLiveChunkDirty(key: VoxelKey) = Unit\n" + s[end:]
tsdf.write_text(s)
