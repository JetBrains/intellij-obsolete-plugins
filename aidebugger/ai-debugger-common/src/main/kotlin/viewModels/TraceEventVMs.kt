package com.intellij.aidebugger.common.viewModels

import com.intellij.aidebugger.common.AiDebuggerPlugin.scriptsPath
import com.intellij.aidebugger.common.models.entities.EventStackFrame
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import com.intellij.aidebugger.common.models.entities.TraceLlmTool
import kotlinx.coroutines.flow.StateFlow

open class EventVM: ViewModelBase

class ExceptionEventVM(
    val name: String,
    val exception: String,
    val stackTrace: List<EventStackFrame>,
) : EventVM()

class TraceEventVM(
    val name: String,
    val eventType: EventType,
    val framework: Framework,
    val children: List<EventVM>,
    val niceVms: List<ViewModelBase>,
    val widgets: List<ViewModelBase>? = null,
    val rawJson: String? = null,
    val tools: List<TraceLlmTool>? = null,
    val finished: Boolean,
    val defaultExpanded: Boolean,
) : EventVM()

class EventsGroupVM(
    override val threadId: String,
    override val title: String,
    override val isActive: Boolean,
    val stackTrace: List<EventStackFrame>,
    graph: SimpleGraph,
    lastEventName: StateFlow<String?>
) : EventVM(), SessionThreadVM {
    val graphVM = SimpleGraphVM(graph, lastEventName)
}


class DataMapVM(
    val data: Map<String, Any>
) : ViewModelBase

class DataListVM(
    val data: List<Any>
) : ViewModelBase

class DataStringVM(
    val data: String
) : ViewModelBase

class DataNullVM : ViewModelBase

fun codeLineFromStack(stackTrace: List<String>): String? {
    return stackTrace
        .lastOrNull { !it.contains(scriptsPath) }
        ?.lines()
        ?.firstOrNull()
        ?.trim()
}

fun dataToVM(data: Any?): ViewModelBase {
    return when (data) {
        null -> DataNullVM()
        is Map<*, *> -> DataMapVM(
            data.map { it.key.toString() to dataToVM(it.value) }.toMap()
        )
        is List<*> -> DataListVM(
            data.map { dataToVM(it) }.toList()
        )
        is String -> DataStringVM(data)
        is Number -> DataStringVM(data.toString())
        is Boolean -> DataStringVM(data.toString())
        else -> DataNullVM()
    }
}