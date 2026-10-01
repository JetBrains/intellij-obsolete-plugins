package com.intellij.dataWrangler.impl.operations

import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWCollectionItemType
import com.intellij.dataWrangler.annotations.DWParameterType
import com.intellij.dataWrangler.annotations.DWVariantType
import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.annotations.DisplayNameProvider
import com.intellij.dataWrangler.annotations.FloatValuesProvider
import com.intellij.dataWrangler.annotations.IntValuesProvider
import com.intellij.dataWrangler.annotations.ItemDisplayNameProvider
import com.intellij.dataWrangler.annotations.PossibleValuesProvider
import com.intellij.dataWrangler.annotations.StringValuesProvider
import com.intellij.dataWrangler.annotations.ValueDisplayNameProvider
import com.intellij.dataWrangler.annotations.ValuesProvider
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWCollectionTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWPrimitiveTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc.Field
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWVariantTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypeSource.DWAnonymousDefinition
import com.intellij.dataWrangler.impl.operations.DWTypeSource.DWClassDefinition
import com.intellij.dataWrangler.impl.operations.DWTypeSource.DWFakeDefinition
import com.intellij.dataWrangler.impl.operations.DWTypeSource.DWTypeDefinition
import com.intellij.dataWrangler.operations.FieldType
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.database.util.common.castTo
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.containers.FactoryMap
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

object DWTypes {
  fun <V: Any> create(member: KProperty1<*, V>): DWTypeDesc<V> {
    return create(DWAnonymousDefinition(member))
  }
  fun <V: Any> create(kClass: KClass<V>): DWTypeDesc<V> {
    return create(DWClassDefinition(kClass))
  }
  fun <V: Any> createFake(targetType: KClass<V>, annotations: List<Annotation>): DWTypeDesc<V> {
    return create(DWFakeDefinition(targetType, annotations))
  }

  fun <V: Any> createFakeStruct(kClass: KClass<V>, fields: List<Field<V, *>>): DWStructTypeDesc<V> {
    return DWStructTypeDesc(DWClassDefinition(kClass), null, fields, null)
  }
}

private val typesCache: Map<KClass<out Annotation>, DWTypeDesc<*>> = FactoryMap.create {
  create<Any>(DWTypeDefinition(it))
}

private fun <V: Any> create(source: DWTypeSource<V>): DWTypeDesc<V> {
  val builder = DWTypeBuilder(source)
  source.annotations.forEach { annotation ->
    builder.processAnnotation(annotation)
  }
  return builder.build()
}

private class DWTypeBuilder<V: Any>(val source: DWTypeSource<V>) {
  var defaultName: String? = null
  var displayName: DisplayNameProvider<*>? = null
  var possibleValues: PossibleValuesProvider<*>? = null
  var collectionItemType: DWTypeDesc<*>? = null
  var variants: List<DWVariantTypeDesc.Item<*>>? = null
  var parent: DWTypeDesc<*>? = null
  var hasTypeAnnotation = false

  fun processAnnotation(annotation: Annotation) {
    when {
      annotation is ValueDisplayNameProvider ->
        displayName = annotation.value.createInstance()
      annotation is ValuesProvider ->
        possibleValues = annotation.value.createInstance()
      annotation is IntValuesProvider ->
        possibleValues = FixedPossibleValuesProvider(annotation.value.asList(), Int::class)
      annotation is FloatValuesProvider ->
        possibleValues = FixedPossibleValuesProvider(annotation.value.asList(), Float::class)
      annotation is StringValuesProvider ->
        possibleValues = FixedPossibleValuesProvider(annotation.value.asList(), String::class)
      annotation is DWCollectionItemType ->
        collectionItemType = typesCache[annotation.value]
      annotation is DWVariantType ->
        variants = annotation.value.map { DWVariantTypeDesc.Item(DWTypes.create(it.value) as DWStructTypeDesc, it.name) }
      annotation is CommandParameterName ->
        defaultName = annotation.value
      annotation is DWParameterType -> {
        hasTypeAnnotation = true
        annotation.extends.takeIf { it.isDWType() }?.let {
          parent = typesCache[it]
        }
      }
      annotation.annotationClass.isDWType() ->
        parent = typesCache[annotation.annotationClass]
    }
  }

  private fun validateValues() {
    if (!hasTypeAnnotation && source.defType != null) {
      throw AssertionError("$source does not have ${DWParameterType::class.simpleName} annotation")
    }
  }

  fun build(): DWTypeDesc<V> {
    validateValues()
    parent?.let { inheritValues(it) }
    detectValues()
    return create()
  }

  private fun create(): DWTypeDesc<V> {
    finalValidate()
    val itemType = collectionItemType
    if (itemType != null) {
      return createCollection(itemType)
    }
    if (variants != null) {
      return createVariants(variants)
    }
    val fields = getStructFields()
    if (fields != null) {
      return createStruct(fields)
    }
    return createPrimitive()
  }

  @Suppress("UNCHECKED_CAST")
  private fun createVariants(variants: List<DWVariantTypeDesc.Item<*>>?): DWVariantTypeDesc<V> {
    return DWVariantTypeDesc(source, defaultName, variants as List<DWVariantTypeDesc.Item<V>>, parent)
  }

  private fun finalValidate() {
    if (parent is DWCollectionTypeDesc<*, *> && collectionItemType == null) {
      throw AssertionError("$source inherits collection type $parent but has no item type")
    }
    val possibleValues = possibleValues
    val displayName = displayName
    if (collectionItemType != null) {
      if (possibleValues != null) {
        throw AssertionError("$source inherits collection type but has ${ValuesProvider::class.simpleName}")
      }
      if (displayName != null) {
        throw AssertionError("$source inherits collection type but has ${ValueDisplayNameProvider::class.simpleName}")
      }
    }
    val targetType = source.targetType
    val pPrimitive = parent as? DWPrimitiveTypeDesc<*>
    validateInheritedTypes(possibleValues, pPrimitive?.possibleValues, targetType) { this.targetType }
    validateInheritedTypes(displayName, pPrimitive?.displayName, targetType) { this.targetType }
    val anyTarget = targetType ?: possibleValues?.targetType
    if (anyTarget != null) {
      if (collectionItemType != null && !anyTarget.isSubclassOf(Collection::class)) {
        throw AssertionError("$source inherits collection type but applied to $targetType")
      }
      if (collectionItemType == null && anyTarget.isSubclassOf(Collection::class)) {
        throw AssertionError("$source does not inherit collection type but applied to collection $targetType")
      }
      variants?.forEach {
        if (it.desc.targetType?.isSubclassOf(anyTarget) == false) {
          throw AssertionError("$source variant type is not subclass of $anyTarget (got ${it.desc.targetType})")
        }
      }
    }
    if (collectionItemType != null && variants != null) {
      throw AssertionError("$source matches both collection and variant type")
    }
  }

  private inline fun <T: Any> validateInheritedTypes(x: T?, parentX: T?, targetType: KClass<*>?, getType: T.() -> KClass<*>) {
    if (x != null) {
      if (targetType != null && !x.getType().isAssignableFrom(targetType)) {
        throw AssertionError("$source uses $displayName that is not applicable")
      }
      if (parentX != null && !x.getType().isAssignableFrom(parentX.getType())) {
        throw AssertionError("$source uses $displayName and inherits ${parent?.source} that uses ${parentX} - they are not compatible")
      }
    }
  }

  private fun KClass<*>.isAssignableFrom(other: KClass<*>): Boolean =
    java.isAssignableFrom(other.java)

  private fun createCollection(itemType: DWTypeDesc<*>): DWTypeDesc<V> {
    @Suppress("UNCHECKED_CAST")
    return DWCollectionTypeDesc(source as DWTypeSource<Collection<Any>>, defaultName, itemType as DWTypeDesc<Any>, parent) as DWTypeDesc<V>
  }

  private fun createPrimitive(): DWTypeDesc<V> {
    @Suppress("UNCHECKED_CAST")
    return DWPrimitiveTypeDesc(
      source, defaultName,
      possibleValues as PossibleValuesProvider<V>?,
      displayName as DisplayNameProvider<V>?,
      parent
    )
  }

  private fun createStruct(fields: List<Field<V, *>>): DWTypeDesc<V> {
    return DWStructTypeDesc(
      source, defaultName, fields, parent
    )
  }

  private fun getStructFields(): List<Field<V, *>>? =
    source.targetType?.takeIf { !isPrimitiveType(it) }?.run {
      val order = HashMap<String, Int>()
      primaryConstructor?.parameters?.forEachIndexed { index, parameter -> order[parameter.name ?: ""] = index }
      memberProperties.mapNotNull { it as? KMutableProperty1<V, *> }
        .sortedBy { order[it.name] ?: Int.MAX_VALUE }
        .map { createField<Any>(it) }
    }

  private fun isPrimitiveType(klass: KClass<V>): Boolean =
    klass.javaPrimitiveType != null || klass == String::class || klass.isSubclassOf(Enum::class)

  @Suppress("UNCHECKED_CAST")
  private fun <V2: Any> createField(property: KMutableProperty1<V, *>): Field<V, V2> =
    DWStructTypeDesc.createField(property as KMutableProperty1<V, V2>)


  private fun detectValues() {
    val targetType = source.targetType
    if (possibleValues == null && targetType != null) {
      possibleValues = detectPossibleValuesProvider(targetType)
    }
    if (displayName == null && targetType != null) {
      displayName = detectDisplayNameProvider(targetType)
    }
  }

  private fun inheritValues(parent: DWTypeDesc<*>) {
    if (defaultName == null) {
      defaultName = parent.defaultName
    }
    if (possibleValues == null && parent is DWPrimitiveTypeDesc) {
      possibleValues = parent.possibleValues
    }
    if (collectionItemType == null && parent is DWCollectionTypeDesc<*, *>) {
      collectionItemType = parent.itemType
    }
    if (variants == null && parent is DWVariantTypeDesc<*>) {
      variants = parent.variants
    }
  }

  private fun <V : Any> detectPossibleValuesProvider(
    targetType: KClass<V>,
  ): PossibleValuesProvider<V>? {
    if (targetType.isSubclassOf(Enum::class)) {
      return EnumPossibleValuesProvider<V>(targetType)
    }
    return null
  }
}

private fun <V : Any> detectDisplayNameProvider(
  targetType: KClass<V>,
): DisplayNameProvider<V>? {
  if (targetType.isSubclassOf(DisplayName::class)) {
    @Suppress("UNCHECKED_CAST")
    return ItemDisplayNameProvider(targetType as KClass<DisplayName>) as DisplayNameProvider<V>
  }
  if (targetType.isSubclassOf(Enum::class)) {
    @Suppress("UNCHECKED_CAST")
    return EnumDisplayNameProvider(targetType as KClass<Enum<*>>) as DisplayNameProvider<V>
  }
  return null
}

private class EnumPossibleValuesProvider<V: Any>(clazz: KClass<V>): PossibleValuesProvider<V>(clazz) {
  override fun getValues(context: DataWranglerContext?): List<V> {
    return targetType.java.enumConstants.toList()
  }
}

private class EnumDisplayNameProvider<V: Enum<*>>(clazz: KClass<V>): DisplayNameProvider<V>(clazz) {
  override fun getDisplayName(context: DataWranglerContext, value: V): String {
    return value.name
  }
}

private fun KClass<out Annotation>.isDWType(): Boolean {
  return annotations.find { it is DWParameterType } != null
}
private class FixedPossibleValuesProvider<V: Any>(val values: List<V>, elementClass: KClass<V>): PossibleValuesProvider<V>(elementClass) {
  override fun getValues(context: DataWranglerContext?): List<V> = values
}

internal sealed interface DWTypeSource<V: Any> {
  val defType: KClass<out Annotation>?
  val annotations: List<Annotation>
  val targetType: KClass<V>?

  class DWFakeDefinition<V: Any>(override val targetType: KClass<V>, override val annotations: List<Annotation>): DWTypeSource<V> {
    override val defType: KClass<out Annotation>? get() = null
    override fun toString(): String {
      return "fake ${targetType.simpleName}"
    }
  }
  class DWTypeDefinition(override val defType: KClass<out Annotation>): DWTypeSource<Any> {
    override val annotations: List<Annotation> get() = defType.annotations
    override val targetType: KClass<Any>? get() = null
    override fun toString(): String {
      return "$defType"
    }
  }
  class DWAnonymousDefinition<V: Any>(private val member: KProperty1<*, V>): DWTypeSource<V> {
    override val annotations: List<Annotation> get() = member.annotations
    override val defType: KClass<out Annotation>? get() = null
    @Suppress("UNCHECKED_CAST")
    override val targetType: KClass<V> get() = member.returnType.classifier as KClass<V>
    override fun toString(): String {
      return member.name
    }
  }
  class DWClassDefinition<V: Any>(private val kClass: KClass<V>): DWTypeSource<V> {
    override val annotations: List<Annotation> get() = kClass.annotations
    override val defType: KClass<out Annotation>? get() = null
    @Suppress("UNCHECKED_CAST")
    override val targetType: KClass<V> get() = kClass
    override fun toString(): String {
      return "$kClass"
    }
  }
}
sealed class DWTypeDesc<V: Any>(internal val source: DWTypeSource<V>, @NlsContexts.Label val defaultName: String?, val parent: DWTypeDesc<*>?) {
  val defType: KClass<out Annotation>? get() = source.defType
  val anyDefType: KClass<out Annotation>? get() = defType ?: parent?.anyDefType
  open val targetType: KClass<V>? get() = source.targetType ?: possibleValues?.targetType
  val annotations: List<Annotation> get() = source.annotations
  abstract val possibleValues: PossibleValuesProvider<V>?

  class DWStructTypeDesc<V: Any> internal constructor(
    source: DWTypeSource<V>, @NlsContexts.Label defaultName: String?,
    val fields: List<Field<V, *>>,
    parent: DWTypeDesc<*>?
  ) : DWTypeDesc<V>(source, defaultName, parent) {
    override val possibleValues: PossibleValuesProvider<V>? get() = null
    override val targetType: KClass<V>
      get() = super.targetType!!

    override fun toString(): String {
      return "struct ${source} {${fields.joinToString()}}"
    }

    data class Field<P: Any, V: Any> (
      val id: String,
      val desc: DWTypeDesc<V>,
      val accessor: FieldAccessor<P, V>
    ) {
      override fun toString(): String {
        return "$id: $desc"
      }
    }

    companion object {
      fun <V: Any, V2: Any> createField(property: KMutableProperty1<V, V2>): Field<V, V2> =
        Field(property.name, DWTypes.create(property as KProperty1<V, V2>), PropertyAccessor(property))
    }


    interface FieldAccessor<P: Any, V: Any> {
      val type: KClass<V>
      fun set(instance: P, value: V)
      fun get(instance: P): V
    }

    class PropertyAccessor<V: Any, V2: Any>(val property: KMutableProperty1<V, V2>): FieldAccessor<V, V2> {
      @Suppress("UNCHECKED_CAST")
      override val type: KClass<V2>
        get() = property.returnType.classifier as KClass<V2>

      override fun set(instance: V, value: V2) {
        property.set(instance, value)
      }

      override fun get(instance: V): V2 {
        return property.get(instance)
      }
    }
  }

  class DWVariantTypeDesc<V: Any> internal constructor(
    source: DWTypeSource<V>, @NlsContexts.Label defaultName: String?,
    val variants: List<Item<out V>>,
    parent: DWTypeDesc<*>?
  ) : DWTypeDesc<V>(source, defaultName, parent) {
    override val possibleValues: PossibleValuesProvider<V>? get() = null

    override fun toString(): String {
      return "variants $source(${variants.joinToString()})"
    }

    class Item<V: Any> internal constructor(
      val desc: DWStructTypeDesc<V>, @NlsContexts.Label val label: String?
    ) {
      val type: KClass<V> get() = desc.targetType
      val id: String get() = type.simpleName!!
    }
  }
  class DWPrimitiveTypeDesc<V: Any> internal constructor(
    source: DWTypeSource<V>, @NlsContexts.Label defaultName: String?,
    override val possibleValues: PossibleValuesProvider<V>?,
    val displayName: DisplayNameProvider<V>?,
    parent: DWTypeDesc<*>?
  ) : DWTypeDesc<V>(source, defaultName, parent) {

    override fun toString(): String {
      defType?.let { return "def ${it.simpleName}" }
      annotations.find { it.annotationClass.isDWType() }?.annotationClass?.let {
        return "anno ${it.simpleName}"
      }
      if (source is DWFakeDefinition) {
        return "$source${parent?.let { ": $it" } ?: ""}"
      }
      return "no type anno $source"
    }
  }
  class DWCollectionTypeDesc<V: Any, C: Collection<V>> internal constructor(source: DWTypeSource<C>, @NlsContexts.Label defaultName: String?, val itemType: DWTypeDesc<V>, parent: DWTypeDesc<*>?)
    : DWTypeDesc<C>(source, defaultName, parent) {
    override val possibleValues: PossibleValuesProvider<C>? get() = null

    override fun toString(): String {
      return "coll ${targetType?.simpleName} ($itemType)"
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun <V: Any, C: Collection<V>> DWTypeDesc<C>.asCollectionType(): DWCollectionTypeDesc<V, C> =
  this as DWCollectionTypeDesc<V, C>

fun DWTypeDesc<*>.inherits(type: KClass<out Annotation>): Boolean {
  return inherits(typesCache[type]!!)
}
fun DWTypeDesc<*>.inherits(dwType: DWTypeDesc<*>): Boolean {
  return dwType == this || parent?.inherits(dwType) ?: false
}

fun <P : Any> MetaStruct<P>.setDefaultsFromPossibleValues(instance: P, context: DataWranglerContext?) {
  fun <V: Any> FieldType<P, V>.resetDefault() {
    if (this is FieldTypeImpl<V, P>) {
      this.desc.possibleValues?.getValues(context)?.firstOrNull()?.let {
        set(instance, it)
      }
    }
  }
  for (field in getFields().values) {
    field.resetDefault()
  }
}

fun <T: Any> DWTypeDesc<*>.findAnnotation(clazz: KClass<T>): T? {
  return annotations.find { clazz.isInstance(it) }?.castTo(clazz) ?: parent?.findAnnotation(clazz)
}