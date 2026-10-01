package com.intellij.dataWrangler.annotations

import com.intellij.openapi.util.NlsContexts

/**
 * A user-visible label for a specific field in the data class within the parameters panel.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommandParameterName(@NlsContexts.Label val value: String)