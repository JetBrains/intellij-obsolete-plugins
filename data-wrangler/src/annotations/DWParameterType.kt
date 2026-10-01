package com.intellij.dataWrangler.annotations

import kotlin.reflect.KClass

/**
 * Creates a structured hierarchy within annotation classes.
 * Look [DWTableColumn] and its subtype `com.intellij.dataWrangler.jupyterPython.operations.DWTableNumericColumn`
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DWParameterType(val extends: KClass<out Annotation> = Annotation::class)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DWCollectionItemType(val value: KClass<out Annotation>)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DWVariantType(vararg val value: DWVariantTypeItem)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DWVariantTypeItem(val value: KClass<*>, val name: String)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@DWParameterType
annotation class DWEmptyType
