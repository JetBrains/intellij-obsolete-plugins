package com.intellij.aidebugger.koog

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

object AiDebuggerKoogBundle {
    private const val BUNDLE = "messages.AiDebuggerKoogBundle"

    private val INSTANCE: DynamicBundle =
      DynamicBundle(AiDebuggerKoogBundle::class.java, BUNDLE)

    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return INSTANCE.getMessage(key, *params)
    }
}