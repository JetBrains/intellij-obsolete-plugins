package com.intellij.aidebugger.python.viewModels

internal val MESSAGE_LIST_KEYS = listOf(
    "messages",
    "prompts",
)
internal val MESSAGE_KEYS = listOf(
    "input",
    "inputs",
    "output",
    "outputs",
)

@Suppress("UNCHECKED_CAST")
fun getLastMessage(data: Any?): Any? = when (data) {
    is Map<*, *> -> {
        val mapData = data as? Map<String, Any> ?: return null
        findLastMessage(mapData)
    }
    is List<*> -> {
        data.firstNotNullOfOrNull { getLastMessage(it) }
    }
    is String -> data // TODO is it ok? there are other usages other than nice view
    else -> null
}

fun findLastMessage(output: Map<String, Any>): Any? {
    output.forEach { (key, value) ->
        if (key.lowercase() in MESSAGE_LIST_KEYS && value is List<*>) {
            return value.lastOrNull()
        }
        else if (key.lowercase() in MESSAGE_KEYS) {
            return value
        }
    }
    return null
}

