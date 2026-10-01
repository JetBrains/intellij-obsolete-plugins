package com.intellij.aidebugger.evaluation.settings

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

object AIToolkitUIBundle {

    private const val BUNDLE: String = "messages.AIToolkitUIBundle"
    private val INSTANCE: DynamicBundle = DynamicBundle(AIToolkitUIBundle::class.java, BUNDLE)

    @Nls
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        INSTANCE.getMessage(key, *params)
}
