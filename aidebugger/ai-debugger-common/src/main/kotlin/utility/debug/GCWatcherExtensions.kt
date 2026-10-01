package com.intellij.aidebugger.common.utility.debug

/**
 * Extension function to easily watch an object for GC
 */
fun <T : Any> T.watchGC(description: String = ""): T {
    GCWatcher.getInstance().watch(this, description)
    return this
}

/**
 * Extension function to watch an object with auto-generated description
 */
inline fun <reified T : Any> T.watchGC(): T {
    val description = "${T::class.java.simpleName}@${System.identityHashCode(this).toString(16)}"
    GCWatcher.getInstance().watch(this, description)
    return this
}