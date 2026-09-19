package com.nothatcher.sproutbook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.min

private val TreeAnchors = listOf(
    Offset(.50f,.18f), Offset(.37f,.24f), Offset(.63f,.25f), Offset(.27f,.33f),
    Offset(.48f,.32f), Offset(.72f,.34f), Offset(.18f,.44f), Offset(.36f,.43f),
    Offset(.58f,.42f), Offset(.82f,.45f), Offset(.27f,.54f), Offset(.48f,.53f),
    Offset(.70f,.54f), Offset(.15f,.59f), Offset(.85f,.60f), Offset(.37f,.64f),
    Offset(.61f,.64f), Offset(.24f,.71f), Offset(.76f,.71f), Offset(.50f,.73f),
    Offset(.33f,.77f), Offset(.66f,.77f), Offset(.42f,.16f), Offset(.58f,.16f)
)

@Composable
fun MemoryTree(
    memories: List<Memory>,
    arrangeMode: Boolean,
    onLeafTap: (Memory) -> Unit,
    onMoveMemory: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var selectedId by remember(arrangeMode) { mutableStateOf<String?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    fun anchorPx(index: Int): Offset {
        val a = TreeAnchors[index.mod(TreeAnchors.size)]
        return Offset(a.x * canvasSize.width, a.y * canvasSize.height)
    }

    fun leafPosition(memory: Memory, index: Int): Offset {
        val anchorIndex = if (memory.anchor >= 0) memory.anchor else index.mod(TreeAnchors.size)
        if (memory.id == draggingId && dragPosition != null) return dragPosition!!
        return anchorPx(anchorIndex)
    }

    fun findLeaf(position: Offset): Pair<Int, Memory>? {
        return memories.mapIndexed { i, m -> i to m }
            .minByOrNull { (i, m) -> (leafPosition(m, i) - position).getDistance() }
            ?.takeIf { (i, m) -> (leafPosition(m, i) - position).getDistance() <= 52f }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.08f)
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(memories, arrangeMode, canvasSize) {
                detectTapGestures { pos ->
                    val hit = findLeaf(pos)
                    if (hit != null) {
                        if (arrangeMode) selectedId = hit.second.id else onLeafTap(hit.second)
                    } else if (arrangeMode && selectedId != null) {
                        val nearest = TreeAnchors.indices.minByOrNull { i ->
                            (anchorPx(i) - pos).getDistance()
                        }
                        if (nearest != null && (anchorPx(nearest) - pos).getDistance() < 72f) {
                            onMoveMemory(selectedId!!, nearest)
                        }
                    }
                }
            }
            .pointerInput(memories, arrangeMode, canvasSize) {
                if (arrangeMode) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { pos ->
                            val hit = findLeaf(pos)
                            if (hit != null) {
                                selectedId = hit.second.id
                                draggingId = hit.second.id
                                dragPosition = pos
                            }
                        },
                        onDrag = { change, amount ->
                            if (draggingId != null) {
                                change.consume()
                                dragPosition = (dragPosition ?: change.position) + amount
                            }
                        },
                        onDragEnd = {
                            val id = draggingId
                            val pos = dragPosition
                            if (id != null && pos != null) {
                                val nearest = TreeAnchors.indices.minByOrNull { i ->
                                    (anchorPx(i) - pos).getDistance()
                                }
                                if (nearest != null) onMoveMemory(id, nearest)
                            }
                            draggingId = null
                            dragPosition = null
                        },
                        onDragCancel = {
                            draggingId = null
                            dragPosition = null
                        }
                    )
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val growth = (0.45f + memories.size * .025f).coerceAtMost(1f)

        drawOval(
            color = Color.Black.copy(alpha = .18f),
            topLeft = Offset(w*.30f, h*.88f),
            size = Size(w*.40f, h*.055f)
        )

        val canopy = listOf(
            Triple(.31f,.36f,.17f), Triple(.46f,.27f,.18f), Triple(.62f,.29f,.18f),
            Triple(.72f,.39f,.16f), Triple(.24f,.48f,.15f), Triple(.42f,.45f,.19f),
            Triple(.60f,.46f,.20f), Triple(.78f,.51f,.13f), Triple(.35f,.58f,.16f),
            Triple(.57f,.57f,.18f)
        )
        canopy.forEachIndexed { i, (x,y,r) ->
            val alpha = (.44f + growth*.42f).coerceAtMost(.9f)
            val base = if (i % 3 == 0) Color(0xFF28583A) else Color(0xFF1D472F)
            drawCircle(base.copy(alpha = alpha), radius = w*r*growth, center = Offset(w*x,h*y))
            drawCircle(
                Color(0xFF3C724A).copy(alpha = .18f),
                radius = w*r*.55f*growth,
                center = Offset(w*x-w*r*.18f,h*y-w*r*.16f)
            )
        }

        val trunk = Path().apply {
            moveTo(w*.44f,h*.90f)
            lineTo(w*.47f,h*.61f)
            lineTo(w*.485f,h*.39f)
            lineTo(w*.515f,h*.39f)
            lineTo(w*.53f,h*.61f)
            lineTo(w*.56f,h*.90f)
            close()
        }
        drawPath(trunk, Color(0xFF6C452B))

        val trunkHighlight = Path().apply {
            moveTo(w*.455f,h*.88f)
            lineTo(w*.485f,h*.60f)
            lineTo(w*.495f,h*.40f)
            lineTo(w*.505f,h*.40f)
            lineTo(w*.505f,h*.62f)
            lineTo(w*.49f,h*.88f)
            close()
        }
        drawPath(trunkHighlight, Color(0xFF9B6A43))

        val branches = listOf(
            Triple(Offset(.50f,.63f), Offset(.24f,.46f), 14f),
            Triple(Offset(.50f,.59f), Offset(.76f,.43f), 14f),
            Triple(Offset(.50f,.48f), Offset(.31f,.29f), 11f),
            Triple(Offset(.50f,.45f), Offset(.69f,.27f), 11f),
            Triple(Offset(.49f,.72f), Offset(.29f,.64f), 12f),
            Triple(Offset(.51f,.70f), Offset(.71f,.64f), 12f)
        )
        branches.forEach { (a,b,stroke) ->
            val ap = Offset(a.x*w,a.y*h)
            val bp = Offset(b.x*w,b.y*h)
            drawLine(Color(0xFF6B4329), ap, bp, strokeWidth = stroke)
            drawLine(Color(0xFF986540), ap-Offset(2f,2f), bp-Offset(2f,2f), strokeWidth = stroke*.26f)
        }

        TreeAnchors.forEachIndexed { i, _ ->
            val p = anchorPx(i)
            val source = Offset(
                x = w*.50f + (p.x-w*.50f)*.52f,
                y = min(p.y+38f, h*.72f)
            )
            drawLine(Color(0xFF5B3B27).copy(alpha=.76f), source, p, strokeWidth = 3.2f)
        }

        if (arrangeMode) {
            TreeAnchors.indices.forEach { i ->
                val p = anchorPx(i)
                drawCircle(Color(0xFFF0CB72).copy(alpha=.16f), radius=15f, center=p)
                drawCircle(Color(0xFFF0CB72).copy(alpha=.72f), radius=18f, center=p, style=Stroke(2.4f))
            }
        }

        memories.forEachIndexed { i, memory ->
            val p = leafPosition(memory, i)
            val selected = memory.id == selectedId
            val leafColor = when (memory.kind.lowercase()) {
                "first" -> Color(0xFFF0C36C)
                "milestone" -> Color(0xFF79B891)
                "funny" -> Color(0xFFD99AB9)
                "health" -> Color(0xFF7CAED2)
                "photo" -> Color(0xFFAA94D6)
                else -> Color(0xFF8FBC78)
            }
            val angle = ((i * 37) % 30 - 15).toFloat()
            val rx = if (selected) 30f else 25f
            val ry = if (selected) 20f else 17f

            withTransform({ rotate(angle, p) }) {
                val leaf = Path().apply {
                    moveTo(p.x-rx,p.y)
                    cubicTo(p.x-rx*.35f,p.y-ry*1.25f,p.x+rx*.35f,p.y-ry*1.25f,p.x+rx,p.y)
                    cubicTo(p.x+rx*.35f,p.y+ry*1.25f,p.x-rx*.35f,p.y+ry*1.25f,p.x-rx,p.y)
                    close()
                }
                drawPath(leaf, leafColor)
                drawLine(Color(0xFF27432A).copy(alpha=.68f), Offset(p.x-rx*.62f,p.y), Offset(p.x+rx*.62f,p.y), strokeWidth=1.8f)
                if (selected) {
                    drawOval(
                        color = SproutPrimary,
                        topLeft = Offset(p.x-rx-7f,p.y-ry-7f),
                        size = Size((rx+7f)*2,(ry+7f)*2),
                        style = Stroke(3f)
                    )
                }
            }
        }
    }
}
