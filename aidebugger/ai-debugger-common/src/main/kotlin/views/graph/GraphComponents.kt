package com.intellij.aidebugger.common.views.graph


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.aidebugger.common.models.entities.SimpleGraphNode
import com.intellij.aidebugger.common.viewModels.SimpleGraphVM
import org.jetbrains.jewel.ui.component.Text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val nodeHeight = 40.dp
val nodeWidth = 80.dp

fun getNodeColor(type: String): Color {
    return when (type) {
        "schema" -> Color(0xFF8BC34A)
        "runnable" -> Color(0xFF2196F3)
        else -> Color.Gray
    }
}

fun Modifier.draggable(
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {},
    onDrag: (Offset) -> Unit
): Modifier = composed {
//    val state = rememberDraggableState()
    pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { onDragStarted() },
            onDragEnd = { onDragStopped() },
            onDrag = { change, dragAmount ->
                change.consume()
                onDrag(dragAmount)
            }
        )
    }
}

fun DrawScope.drawArrow(sourcePos: Offset, targetPos: Offset)
{
    val path = Path().apply {
        moveTo(sourcePos.x, sourcePos.y)
        lineTo(targetPos.x, targetPos.y)
    }

    // Draw edge
    val dashEffect = if (/*edge.conditional*/ false) {
        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    } else null

    drawPath(
        path = path,
        color = Color.Gray,
        style = Stroke(width = 2f, pathEffect = dashEffect)
    )

    // Draw arrow
    val angle = atan2(targetPos.y - sourcePos.y, targetPos.x - sourcePos.x)
    val arrowSize = 10f

    val p1 = Offset(
        x = targetPos.x - arrowSize * cos(angle - Math.PI / 6).toFloat(),
        y = targetPos.y - arrowSize * sin(angle - Math.PI / 6).toFloat()
    )
    val p2 = Offset(
        x = targetPos.x - arrowSize * cos(angle + Math.PI / 6).toFloat(),
        y = targetPos.y - arrowSize * sin(angle + Math.PI / 6).toFloat()
    )

    drawLine(
        color = Color.Gray,
        start = targetPos,
        end = p1,
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Gray,
        start = targetPos,
        end = p2,
        strokeWidth = 2f
    )
}

@Composable
fun graphNode(node: SimpleGraphNode, position: MutableState<Offset>, active: Boolean) {
    var position by position

    Box(
        modifier = Modifier
            .size(nodeWidth, nodeHeight)
            .offset(
                x = (position.x - nodeWidth.value / 2).dp,
                y = (position.y - nodeHeight.value / 2).dp
            )
            .clip(RoundedCornerShape(6.dp))
            .background(getNodeColor(node.type ?: "schema"))
            .draggable(
                onDragStarted = {  },
                onDragStopped = {  },
                onDrag = { dragAmount ->
                    // Convert dragAmount to dp and update node position
                    position = position.plus(dragAmount)
                }
            )
            .then(
                if (active) {
                    Modifier.border(2.dp, Color.Green, RoundedCornerShape(6.dp))
                } else Modifier
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = node.id,
//                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

//                    Text(
//                        text = node.type,
//                        fontSize = 9.sp,
//                        color = Color.White.copy(alpha = 0.8f),
//                        textAlign = TextAlign.Center
//                    )
        }
    }
}

@Composable
fun GraphView(viewModel: SimpleGraphVM, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val lastEvent by viewModel.lastEventName.collectAsState()

    val nodeHeightHalfPx = nodeHeight.value * density.density / 2
    val nodeHeightHalfOffset = Offset(0f, nodeHeightHalfPx)

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width(viewModel.canvasWidth)
                .height(viewModel.canvasHeight)
        ) {

            // Draw edges
            viewModel.graph.edges.forEach { edge ->
                val sourcePos by viewModel.nodePositions[edge.source] ?: return@forEach
                val targetPos by viewModel.nodePositions[edge.target] ?: return@forEach

                val sourcePosPx = Offset(sourcePos.x * density.density, sourcePos.y * density.density)
                val targetPosPx = Offset(targetPos.x * density.density, targetPos.y * density.density)

                if (sourcePosPx.y < targetPosPx.y) {
                    drawArrow(
                        sourcePosPx + nodeHeightHalfOffset,
                        targetPosPx - nodeHeightHalfOffset
                    )
                } else {
                    drawArrow(
                        sourcePosPx - nodeHeightHalfOffset,
                        targetPosPx + nodeHeightHalfOffset
                    )
                }
            }
        }

        // Draw nodes
        viewModel.graph.nodes.forEach { node ->
            val position = viewModel.nodePositions[node.id] ?: return@forEach
            graphNode(node, position, lastEvent == node.id)
        }
    }
}