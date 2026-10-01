package com.intellij.dataWrangler.annotations

import com.intellij.dataWrangler.executor.DataWranglerContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValuesProvider(val value: KClass<out PossibleValuesProvider<*>>)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntValuesProvider(vararg val value: Int)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FloatValuesProvider(vararg val value: Float)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StringValuesProvider(vararg val value: String)

abstract class PossibleValuesProvider<T: Any>(val targetType: KClass<T>) {
  abstract fun getValues(context: DataWranglerContext?): List<T>
}
