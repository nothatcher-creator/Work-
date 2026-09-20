package com.roomforge.scanner.scanner

import kotlin.math.abs

data class LiveChunkKey(val x: Int, val y: Int, val z: Int)

data class LiveChunkSnapshot(
    val key: LiveChunkKey,
    val cells: Map<VoxelKey, TsdfCell>
)

data class LiveChunkMesh(
    val key: LiveChunkKey,
    val positions: FloatArray,
    val triangleCount: Int
)

/**
 * Incremental live-mesh extraction: only dirty TSDF chunks are polygonized.
 * The saved model still uses the full-quality final exporter.
 */
object LiveChunkMesher {
    const val CHUNK_VOXELS = 16
    const val MAX_TRIANGLES_PER_CHUNK = 1_500
    const val MAX_TOTAL_TRIANGLES = 24_000

    private data class Corner(val x: Int, val y: Int, val z: Int, val cell: TsdfCell?) {
        val value: Float get() = cell?.tsdf ?: 1f
        val observed: Boolean get() = cell != null && cell.weight >= 0.18f
    }

    private data class P(val x: Float, val y: Float, val z: Float)

    private val cubeOffsets = arrayOf(
        intArrayOf(0, 0, 0), intArrayOf(1, 0, 0), intArrayOf(1, 1, 0), intArrayOf(0, 1, 0),
        intArrayOf(0, 0, 1), intArrayOf(1, 0, 1), intArrayOf(1, 1, 1), intArrayOf(0, 1, 1)
    )

    private val tetrahedra = arrayOf(
        intArrayOf(0, 5, 1, 6), intArrayOf(0, 1, 2, 6), intArrayOf(0, 2, 3, 6),
        intArrayOf(0, 3, 7, 6), intArrayOf(0, 7, 4, 6), intArrayOf(0, 4, 5, 6)
    )

    private val tetraEdges = arrayOf(
        intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(0, 3),
        intArrayOf(1, 2), intArrayOf(1, 3), intArrayOf(2, 3)
    )

    fun build(snapshot: LiveChunkSnapshot, voxelSize: Float): LiveChunkMesh {
        val minX = snapshot.key.x * CHUNK_VOXELS
        val minY = snapshot.key.y * CHUNK_VOXELS
        val minZ = snapshot.key.z * CHUNK_VOXELS
        val maxX = minX + CHUNK_VOXELS
        val maxY = minY + CHUNK_VOXELS
        val maxZ = minZ + CHUNK_VOXELS

        val out = FloatArray(MAX_TRIANGLES_PER_CHUNK * 9)
        var outIndex = 0
        var triangles = 0

        fun emit(a: P, b: P, c: P) {
            if (triangles >= MAX_TRIANGLES_PER_CHUNK) return
            out[outIndex++] = a.x; out[outIndex++] = a.y; out[outIndex++] = a.z
            out[outIndex++] = b.x; out[outIndex++] = b.y; out[outIndex++] = b.z
            out[outIndex++] = c.x; out[outIndex++] = c.y; out[outIndex++] = c.z
            triangles++
        }

        fun interpolate(a: Corner, b: Corner): P {
            val denom = a.value - b.value
            val t = if (abs(denom) < 1e-6f) 0.5f else (a.value / denom).coerceIn(0f, 1f)
            return P(
                (a.x + (b.x - a.x) * t) * voxelSize,
                (a.y + (b.y - a.y) * t) * voxelSize,
                (a.z + (b.z - a.z) * t) * voxelSize
            )
        }

        loop@ for (z in minZ until maxZ) {
            for (y in minY until maxY) {
                for (x in minX until maxX) {
                    val corners = Array(8) { i ->
                        val o = cubeOffsets[i]
                        val key = VoxelKey(x + o[0], y + o[1], z + o[2])
                        Corner(key.x, key.y, key.z, snapshot.cells[key])
                    }
                    if (corners.count { it.observed } < 3) continue
                    if (!corners.any { it.observed && it.value < 0f }) continue
                    if (!corners.any { it.observed && it.value >= 0f }) continue

                    for (tet in tetrahedra) {
                        val local = Array(4) { corners[tet[it]] }
                        if (local.count { it.observed } < 3) continue

                        val intersections = ArrayList<P>(4)
                        for (edge in tetraEdges) {
                            val a = local[edge[0]]
                            val b = local[edge[1]]
                            if (!a.observed || !b.observed) continue
                            if ((a.value < 0f) == (b.value < 0f)) continue
                            intersections += interpolate(a, b)
                        }

                        if (intersections.size == 3) {
                            emit(intersections[0], intersections[1], intersections[2])
                        } else if (intersections.size == 4) {
                            emit(intersections[0], intersections[1], intersections[2])
                            emit(intersections[0], intersections[2], intersections[3])
                        }
                        if (triangles >= MAX_TRIANGLES_PER_CHUNK) break@loop
                    }
                }
            }
        }

        return LiveChunkMesh(snapshot.key, out.copyOf(outIndex), triangles)
    }

    fun combine(chunks: Collection<LiveChunkMesh>, revision: Long): LiveMeshFrame {
        if (chunks.isEmpty()) return LiveMeshFrame(revision = revision)

        var totalTriangles = 0
        for (chunk in chunks) {
            totalTriangles += chunk.triangleCount
            if (totalTriangles >= MAX_TOTAL_TRIANGLES) {
                totalTriangles = MAX_TOTAL_TRIANGLES
                break
            }
        }

        if (totalTriangles <= 0) {
            return LiveMeshFrame(revision = revision, sourceCellCount = chunks.size)
        }

        val out = FloatArray(totalTriangles * 9)
        var dst = 0
        var remainingTriangles = totalTriangles
        for (chunk in chunks) {
            if (remainingTriangles <= 0) break
            val keep = minOf(chunk.triangleCount, remainingTriangles)
            val floats = keep * 9
            chunk.positions.copyInto(out, destinationOffset = dst, startIndex = 0, endIndex = floats)
            dst += floats
            remainingTriangles -= keep
        }

        return LiveMeshFrame(
            revision = revision,
            positions = if (dst == out.size) out else out.copyOf(dst),
            triangleCount = dst / 9,
            sourceCellCount = chunks.size
        )
    }
}
