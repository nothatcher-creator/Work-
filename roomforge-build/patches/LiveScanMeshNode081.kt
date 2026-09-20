package com.roomforge.scanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.roomforge.scanner.scanner.LiveDepthPreviewMesher
import com.roomforge.scanner.scanner.LiveMeshFrame
import io.github.sceneview.SceneScope
import io.github.sceneview.node.MeshNode as MeshNodeImpl
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

private class SafeLiveMeshGpuBuffers(engine: Engine) {
    private val maxVertices = LiveDepthPreviewMesher.MAX_TOTAL_TRIANGLES * 3
    private val maxFloats = maxVertices * 3
    private val retained = ArrayDeque<ByteBuffer>()

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
            val bytes = ByteBuffer.allocateDirect(maxVertices * Int.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            val ints = bytes.asIntBuffer()
            for (i in 0 until maxVertices) ints.put(i)
            ints.flip()
            index.setBuffer(engine, ints)
            retain(bytes)
        }

    init {
        // Never expose Filament to an uninitialized vertex buffer. The initial geometry is
        // degenerate at the world origin until the first real depth patch is uploaded.
        val bytes = ByteBuffer.allocateDirect(maxFloats * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        val floats = bytes.asFloatBuffer()
        repeat(maxFloats) { floats.put(0f) }
        floats.flip()
        vertexBuffer.setBufferAt(engine, 0, floats, 0, maxFloats)
        retain(bytes)
    }

    private fun retain(buffer: ByteBuffer) {
        retained.addLast(buffer)
        while (retained.size > 4) retained.removeFirst()
    }

    fun upload(engine: Engine, positions: FloatArray): Int {
        var vertexCount = min(positions.size / 3, maxVertices)
        vertexCount -= vertexCount % 3
        if (vertexCount <= 0) return 0

        val floatCount = vertexCount * 3
        val bytes = ByteBuffer.allocateDirect(floatCount * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        val floats = bytes.asFloatBuffer()
        floats.put(positions, 0, floatCount)
        floats.flip()
        vertexBuffer.setBufferAt(engine, 0, floats, 0, floatCount)
        retain(bytes)
        return vertexCount
    }
}

@Composable
fun SceneScope.LiveScanMeshNode(frame: LiveMeshFrame) {
    val buffers = remember(engine) { SafeLiveMeshGpuBuffers(engine) }
    val material = remember(materialLoader) {
        materialLoader.createUnlitColorInstance(Color(0.08f, 0.78f, 1.0f, 0.52f))
    }
    val nodeHolder = remember { arrayOfNulls<MeshNodeImpl>(1) }
    val uploadedRevision = remember { longArrayOf(Long.MIN_VALUE) }

    MeshNode(
        primitiveType = RenderableManager.PrimitiveType.TRIANGLES,
        vertexBuffer = buffers.vertexBuffer,
        indexBuffer = buffers.indexBuffer,
        materialInstance = material,
        apply = {
            isTouchable = false
            isHittable = false
            nodeHolder[0] = this
        }
    )

    SideEffect {
        if (uploadedRevision[0] != frame.revision) {
            val vertexCount = buffers.upload(engine, frame.positions)
            val node = nodeHolder[0]
            if (vertexCount > 0 && node != null && node.renderableInstance != 0) {
                engine.renderableManager.setGeometryAt(
                    node.renderableInstance,
                    0,
                    RenderableManager.PrimitiveType.TRIANGLES,
                    buffers.vertexBuffer,
                    buffers.indexBuffer,
                    0,
                    vertexCount
                )
            }
            uploadedRevision[0] = frame.revision
        }
    }
}
