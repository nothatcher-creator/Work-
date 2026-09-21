package com.roomforge.scanner.scanner

import kotlin.math.abs
import kotlin.math.ceil

/**
 * Lightweight room surface used only while scanning.
 *
 * The saved model still uses the full marching-tetrahedra reconstruction. This preview turns the
 * reliable near-zero TSDF band into an exposed-face triangle shell, which is substantially cheaper
 * to rebuild often enough to look live in AR.
 */
data class LiveMeshFrame(
    val revision: Long = 0L,
    val positions: FloatArray = FloatArray(0),
    val triangleCount: Int = 0,
    val sourceCellCount: Int = 0
) {
    companion object {
        val EMPTY = LiveMeshFrame()
    }
}

object LiveTsdfPreviewMesher {
    const val MAX_TRIANGLES = 8_000
    private const val MAX_SOURCE_CELLS = 14_000

    private val directions = arrayOf(
        intArrayOf(1, 0, 0), intArrayOf(-1, 0, 0),
        intArrayOf(0, 1, 0), intArrayOf(0, -1, 0),
        intArrayOf(0, 0, 1), intArrayOf(0, 0, -1)
    )

    fun build(
        volume: Map<VoxelKey, TsdfCell>,
        voxelSize: Float,
        revision: Long
    ): LiveMeshFrame {
        if (volume.isEmpty()) return LiveMeshFrame(revision = revision)

        fun isPreviewSurface(cell: TsdfCell?): Boolean = cell != null &&
            cell.weight >= 0.58f &&
            abs(cell.tsdf) <= 0.62f &&
            (cell.viewCount >= 2 || cell.weight >= 1.35f)

        val surfaceEntries = volume.entries.filter { isPreviewSurface(it.value) }
        if (surfaceEntries.size < 24) {
            return LiveMeshFrame(revision = revision, sourceCellCount = surfaceEntries.size)
        }

        val stride = ceil(surfaceEntries.size / MAX_SOURCE_CELLS.toFloat()).toInt().coerceAtLeast(1)
        val out = FloatArray(MAX_TRIANGLES * 9)
        var triangleCount = 0
        var outIndex = 0

        fun vertex(x: Float, y: Float, z: Float) {
            out[outIndex++] = x
            out[outIndex++] = y
            out[outIndex++] = z
        }

        fun quad(
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float
        ) {
            if (triangleCount + 2 > MAX_TRIANGLES) return
            vertex(ax, ay, az); vertex(bx, by, bz); vertex(cx, cy, cz)
            vertex(ax, ay, az); vertex(cx, cy, cz); vertex(dx, dy, dz)
            triangleCount += 2
        }

        loop@ for (i in surfaceEntries.indices step stride) {
            val key = surfaceEntries[i].key
            val x0 = key.x * voxelSize
            val y0 = key.y * voxelSize
            val z0 = key.z * voxelSize
            val x1 = x0 + voxelSize
            val y1 = y0 + voxelSize
            val z1 = z0 + voxelSize

            for (face in directions.indices) {
                val d = directions[face]
                val neighbor = volume[VoxelKey(key.x + d[0], key.y + d[1], key.z + d[2])]
                if (isPreviewSurface(neighbor)) continue

                when (face) {
                    0 -> quad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1)
                    1 -> quad(x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0)
                    2 -> quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0)
                    3 -> quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1)
                    4 -> quad(x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1)
                    5 -> quad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0)
                }
                if (triangleCount >= MAX_TRIANGLES) break@loop
            }
        }

        return LiveMeshFrame(
            revision = revision,
            positions = out.copyOf(outIndex),
            triangleCount = triangleCount,
            sourceCellCount = surfaceEntries.size
        )
    }
}
