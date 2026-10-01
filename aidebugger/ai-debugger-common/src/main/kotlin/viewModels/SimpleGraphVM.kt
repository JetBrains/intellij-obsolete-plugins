package com.intellij.aidebugger.common.viewModels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import kotlinx.coroutines.flow.StateFlow

class SimpleGraphVM(
    val graph: SimpleGraph,
    val lastEventName: StateFlow<String?>
): ViewModelBase {
    val levelMap = mutableMapOf<String, Int>()
    val nodePositions = mutableStateMapOf<String, MutableState<Offset>>()

    val canvasWidth = 400.dp
    val canvasHeight = 400.dp

    init {
        computeLevels("__start__", 0)

        // Group nodes by level
        val nodesByLevel = levelMap.entries.groupBy({ it.value }, { it.key })
        val maxLevel = nodesByLevel.keys.maxOrNull() ?: 0

        // Position nodes based on their level
        nodesByLevel.forEach { (level, nodeIds) ->
            val nodeCount = nodeIds.size
            nodeIds.forEachIndexed { index, nodeId ->
                val horizontalGap = canvasWidth.value / (nodeCount + 1)
                val verticalGap = canvasHeight.value / (maxLevel + 1)

                nodePositions[nodeId] = mutableStateOf(
                    Offset(
                        x = horizontalGap * (index + 1),
                        y = verticalGap * (level + 0.5f)
                    )
                )
            }
        }
    }


    private fun computeLevels(nodeId: String, level: Int) {
        if (levelMap.containsKey(nodeId) && levelMap[nodeId]!! <= level) return
        levelMap[nodeId] = level

        graph.edges.filter { it.source == nodeId }.forEach { edge ->
            computeLevels(edge.target, level + 1)
        }
    }
}