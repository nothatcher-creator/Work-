package com.roomforge.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.roomforge.scanner.scanner.LiveMeshFrame
import com.roomforge.scanner.scanner.LiveTsdfPreviewMesher
import io.github.sceneview.SceneScope
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.min

private class LiveMeshGpuBuffers(engine: Engine) {
    private val maxVertices = LiveTsdfPreviewMesher.MAX_TRIANGLES * 3
    private val maxFloats = maxVertices * 3

    val vertexBuffer: VertexBuffer = VertexBuffer.Builder()
        .bufferCount(1)
        .vertexCount(maxVertices)
        .attribute(
            VertexBuffer.VertexAttribute.POSITION,
            0,
            VertexBuffer.AttributeType.FLOAT3,
            0,
            3 * Float.SIZE_BYTES
        )
        .build(engine)

    val indexBuffer: IndexBuffer = IndexBuffer.Builder()
        .indexCount(maxVertices)
        .bufferType(IndexBuffer.Builder.IndexType.UINT)
        .build(engine)
        .also { index ->
            index.setBuffer(
                engine,
                IntBuffer.allocate(maxVertices).apply {
                    for (i in 0 until maxVertices) put(i)
                    flip()
                }
            )
        }

    init {
        update(engine, FloatArray(0))
    }

    fun update(engine: Engine, positions: FloatArray) {
        val data = FloatArray(maxFloats)
        val count = min(positions.size, data.size)
        positions.copyInto(data, endIndex = count)
        vertexBuffer.setBufferAt(
            engine,
            0,
            FloatBuffer.wrap(data),
            0,
            data.size
        )
    }
}

/** Draws the current world-space TSDF preview directly in the ARCore session coordinate system. */
@Composable
fun SceneScope.LiveScanMeshNode(frame: LiveMeshFrame) {
    val buffers = remember(engine) { LiveMeshGpuBuffers(engine) }
    val material = remember(materialLoader) {
        materialLoader.createUnlitColorInstance(Color(0.08f, 0.78f, 1.0f, 0.40f))
    }

    val uploadedRevision = remember { longArrayOf(Long.MIN_VALUE) }
    SideEffect {
        if (uploadedRevision[0] != frame.revision) {
            buffers.update(engine, frame.positions)
            uploadedRevision[0] = frame.revision
        }
    }

    MeshNode(
        primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
        vertexBuffer = buffers.vertexBuffer,
        indexBuffer = buffers.indexBuffer,
        materialInstance = material
    )
}
