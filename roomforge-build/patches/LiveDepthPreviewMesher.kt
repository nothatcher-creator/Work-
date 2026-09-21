package com.roomforge.scanner.scanner

import android.opengl.Matrix
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

data class LiveDepthPatchKey(
    val x: Int,
    val y: Int,
    val z: Int,
    val sector: Int
)

data class LiveDepthPatch(
    val key: LiveDepthPatchKey,
    val positions: FloatArray,
    val triangleCount: Int
)

/**
 * Crash-safe instant AR preview.
 *
 * This does not touch the TSDF map. It triangulates a reduced dense-depth grid directly in
 * world space and keeps a small set of nearby/view-direction patches alive. The full TSDF
 * continues independently for the final saved reconstruction.
 */
object LiveDepthPreviewMesher {
    const val MAX_TRIANGLES_PER_PATCH = 1_100
    const val MAX_TOTAL_TRIANGLES = 8_000
    const val MAX_PATCHES = 28
    private const val PATCH_METERS = 0.65f

    fun keyFor(packet: DepthPacket): LiveDepthPatchKey {
        val m = packet.cameraToWorld
        val px = if (m.size > 12) m[12] else 0f
        val py = if (m.size > 13) m[13] else 0f
        val pz = if (m.size > 14) m[14] else 0f
        return LiveDepthPatchKey(
            floor(px / PATCH_METERS).toInt(),
            floor(py / PATCH_METERS).toInt(),
            floor(pz / PATCH_METERS).toInt(),
            packet.viewSector.coerceIn(0, 23)
        )
    }

    fun build(packet: DepthPacket): LiveDepthPatch {
        val key = keyFor(packet)
        if (packet.width < 4 || packet.height < 4 || packet.depthBytes.isEmpty()) {
            return LiveDepthPatch(key, FloatArray(0), 0)
        }

        val targetVertices = 2_400f
        val step = ceil(sqrt((packet.width * packet.height) / targetVertices)).toInt().coerceIn(2, 8)
        val out = FloatArray(MAX_TRIANGLES_PER_PATCH * 9)
        var outIndex = 0
        var triangles = 0

        fun depthMeters(x: Int, y: Int): Float {
            if (x !in 0 until packet.width || y !in 0 until packet.height) return 0f
            val offset = y * packet.depthRowStride + x * packet.depthPixelStride
            if (offset < 0 || offset + 1 >= packet.depthBytes.size) return 0f
            val mm = (packet.depthBytes[offset].toInt() and 0xff) or
                ((packet.depthBytes[offset + 1].toInt() and 0xff) shl 8)
            if (mm <= 0) return 0f
            val d = mm / 1000f
            return if (d in 0.28f..8.0f) d else 0f
        }

        fun worldPoint(x: Int, y: Int, depth: Float, outPoint: FloatArray): Boolean {
            if (depth <= 0f || packet.fx <= 1e-5f || packet.fy <= 1e-5f) return false
            val camera = floatArrayOf(
                depth * (x - packet.cx) / packet.fx,
                depth * (packet.cy - y) / packet.fy,
                -depth,
                1f
            )
            Matrix.multiplyMV(outPoint, 0, packet.cameraToWorld, 0, camera, 0)
            return outPoint[3].isFinite()
        }

        fun triangle(
            ax: Int, ay: Int, ad: Float,
            bx: Int, by: Int, bd: Float,
            cx: Int, cy: Int, cd: Float
        ) {
            if (triangles >= MAX_TRIANGLES_PER_PATCH) return
            if (ad <= 0f || bd <= 0f || cd <= 0f) return

            val minD = minOf(ad, bd, cd)
            val maxD = maxOf(ad, bd, cd)
            val discontinuityLimit = max(0.16f, minD * 0.085f)
            if (maxD - minD > discontinuityLimit) return

            val a = FloatArray(4)
            val b = FloatArray(4)
            val c = FloatArray(4)
            if (!worldPoint(ax, ay, ad, a) || !worldPoint(bx, by, bd, b) || !worldPoint(cx, cy, cd, c)) return

            out[outIndex++] = a[0]; out[outIndex++] = a[1]; out[outIndex++] = a[2]
            out[outIndex++] = b[0]; out[outIndex++] = b[1]; out[outIndex++] = b[2]
            out[outIndex++] = c[0]; out[outIndex++] = c[1]; out[outIndex++] = c[2]
            triangles++
        }

        loop@ for (y in 0 until packet.height - step step step) {
            for (x in 0 until packet.width - step step step) {
                val d00 = depthMeters(x, y)
                val d10 = depthMeters(x + step, y)
                val d01 = depthMeters(x, y + step)
                val d11 = depthMeters(x + step, y + step)

                triangle(x, y, d00, x + step, y, d10, x, y + step, d01)
                triangle(x + step, y, d10, x + step, y + step, d11, x, y + step, d01)

                if (triangles >= MAX_TRIANGLES_PER_PATCH) break@loop
            }
        }

        return LiveDepthPatch(
            key = key,
            positions = out.copyOf(outIndex),
            triangleCount = triangles
        )
    }

    fun combine(patches: Collection<LiveDepthPatch>, revision: Long): LiveMeshFrame {
        if (patches.isEmpty()) return LiveMeshFrame(revision = revision)

        var triangleBudget = MAX_TOTAL_TRIANGLES
        var totalTriangles = 0
        for (patch in patches) {
            val keep = minOf(patch.triangleCount, triangleBudget)
            totalTriangles += keep
            triangleBudget -= keep
            if (triangleBudget <= 0) break
        }

        if (totalTriangles <= 0) {
            return LiveMeshFrame(revision = revision, sourceCellCount = patches.size)
        }

        val positions = FloatArray(totalTriangles * 9)
        var dst = 0
        triangleBudget = totalTriangles
        for (patch in patches) {
            if (triangleBudget <= 0) break
            val keep = minOf(patch.triangleCount, triangleBudget)
            val count = keep * 9
            patch.positions.copyInto(positions, destinationOffset = dst, startIndex = 0, endIndex = count)
            dst += count
            triangleBudget -= keep
        }

        return LiveMeshFrame(
            revision = revision,
            positions = if (dst == positions.size) positions else positions.copyOf(dst),
            triangleCount = dst / 9,
            sourceCellCount = patches.size
        )
    }
}
