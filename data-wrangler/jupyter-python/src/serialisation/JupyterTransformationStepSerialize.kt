package com.intellij.dataWrangler.jupyterPython.serialisation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.operations.DWTypeDesc
import com.intellij.dataWrangler.impl.operations.MetaStructImpl
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.engine.getCommandFactoryById
import com.intellij.dataWrangler.jupyterPython.operations.createMissingJupyterTransformationStep
import com.intellij.dataWrangler.jupyterPython.serialisation.SerializableTransformationStep.Companion.COMMAND_FACTORY_FIELD
import com.intellij.dataWrangler.jupyterPython.serialisation.SerializableTransformationStep.Companion.COMMAND_PARAMS_FIELD
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.diagnostic.thisLogger
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

private val logger = fileLogger()

@JsonDeserialize(using = TransformationStepDeserializer::class)
@JsonSerialize(using = TransformationStepSerializer::class)
data class SerializableTransformationStep(
  val factoryId: String,
  val params: Map<String, Any>,
) {
  companion object {
    const val COMMAND_FACTORY_FIELD: String = "command_factory_class"
    const val COMMAND_PARAMS_FIELD: String = "command_params"

    fun <P : Any, C : DataWranglerContext> createSerializableTransformationStep(step: TransformationStep<P, C>): SerializableTransformationStep {
      val params = mutableMapOf<String, Any>()
      step.params.dumpFields((step.factory.parametersMetaType as MetaStructImpl<P>).desc) { id, v -> params[id] = v }
      return SerializableTransformationStep(step.factory.id, params)
    }

    private fun <P : Any> P.dumpFields(desc: DWTypeDesc.DWStructTypeDesc<P>, consumer: (id: String, v: Any) -> Unit) {
      desc.fields.forEach {
        dumpField(it, consumer)
      }
    }

    private fun <P : Any, V: Any> P.dumpField(field: DWTypeDesc.DWStructTypeDesc.Field<P, V>, consumer: (String, Any) -> Unit) {
      val desc = field.desc
      if (desc is DWTypeDesc.DWStructTypeDesc) {
        val obj = field.accessor.get(this)
        obj.dumpFields(desc) { id, v -> consumer("${field.id}.$id", v) }
      }
      else if (desc is DWTypeDesc.DWVariantTypeDesc) {
        val obj = field.accessor.get(this)
        val variant = desc.variants.find { it.type == obj::class } ?: desc.variants.first()
        consumer(field.id + "#", variant.id)
        @Suppress("UNCHECKED_CAST")
        obj.dumpFields(variant.desc as DWTypeDesc.DWStructTypeDesc<V>) { id, v -> consumer("${field.id}.$id", v) }
      }
      else {
        consumer(field.id, field.accessor.get(this))
      }
    }
  }

  @JsonIgnore
  fun toTransformationStep(): TransformationStep<*, *> {
    val migrated = migrate()
    return toTransformationStep(migrated)
  }
}

private fun toTransformationStep(step: SerializableTransformationStep): TransformationStep<*, *> {
  val factory = getCommandFactoryById(step.factoryId)
  if (factory == null) {
    return createMissingJupyterTransformationStep(step.factoryId, step.params)
  }
  return toTransformationStep(factory, step)
}

private fun <T: Any> toTransformationStep(factory: CommandFactory<T, PythonDataWranglerContext>, step: SerializableTransformationStep): TransformationStep<T, PythonDataWranglerContext> {
  val mt = factory.parametersMetaType
  val params = mt.getNewInstance()
  params.setFields((mt as MetaStructImpl<T>).desc) { step.params[it] }
  return TransformationStep(factory, params)
}

private fun <P: Any, T: Any> P.setField(field: DWTypeDesc.DWStructTypeDesc.Field<P, T>, params: (String) -> Any?) {
  val desc = field.desc
  when (desc) {
    is DWTypeDesc.DWStructTypeDesc -> {
      val obj = field.accessor.get(this)
      obj.setFields(desc) { params("${field.id}.$it") }
    }
    is DWTypeDesc.DWVariantTypeDesc -> {
      val variantId = params(field.id + "#")
      val variant = if (variantId == null) desc.variants.first()
      else desc.variants.find { it.id == variantId } ?: throw IllegalArgumentException("Invalid variant id: $variantId")
      setVariantFields(variant, field, params)
    }
    else -> {
      var v = params(field.id) ?: return
      v = desc.patchDeserializedValue(v)
      field.accessor.set(this, v as T)
    }
  }
}

private fun <P : Any, T : Any> P.setVariantFields(
  variant: DWTypeDesc.DWVariantTypeDesc.Item<T>,
  field: DWTypeDesc.DWStructTypeDesc.Field<P, in T>,
  params: (String) -> Any?,
) {
  val obj = variant.type.createInstance()
  field.accessor.set(this, obj)
  obj.setFields(variant.desc) { params("${field.id}.$it") }
}


fun <P : Any> P.setFields(desc: DWTypeDesc.DWStructTypeDesc<P>, params: (String) -> Any?) {
  desc.fields.forEach {
    try {
      setField(it, params)
    }
    catch (cce: ClassCastException) {
      logger.warn(cce)
    }
  }
}

@Suppress("UNCHECKED_CAST")
private fun <V: Any> DWTypeDesc<V>.patchDeserializedValue(value: Any): V = when (this) {
  is DWTypeDesc.DWCollectionTypeDesc<*, *> -> patchDeserializedValueC(value)
  is DWTypeDesc.DWPrimitiveTypeDesc<V> -> patchDeserializedValueP(value)
  is DWTypeDesc.DWStructTypeDesc<V>,
  is DWTypeDesc.DWVariantTypeDesc<V>
     -> throw AssertionError("Should not happen")
} as V

@Suppress("UNCHECKED_CAST")
private fun <V: Any> DWTypeDesc.DWPrimitiveTypeDesc<V>.patchDeserializedValueP(value: Any): V = targetType.let { targetType ->
  when {
    targetType == null || targetType.isInstance(value) -> value
    targetType.isSubclassOf(Number::class) && value is Number ->
      value.toNumber(targetType as KClass<out Number>)
    targetType.isSubclassOf(Enum::class) && value is String ->
      (targetType as KClass<out Enum<*>>).java.enumConstants.find { it.name == value }!!
    else -> throw AssertionError("Can not convert $value to $this")
  }
} as V

private fun <V: Any, C: Collection<V>> DWTypeDesc.DWCollectionTypeDesc<V, C>.patchDeserializedValueC(value: Any): C =
  when (targetType) {
    null -> value
    List::class -> (value as Collection<*>).map { itemType.patchDeserializedValue(it!!) }
    Set::class -> (value as Collection<*>).mapTo(mutableSetOf()) { itemType.patchDeserializedValue(it!!) }
    else -> throw AssertionError("Can not convert $value to $this")
  } as C

@Suppress("UNCHECKED_CAST")
private fun <T: Number> Number.toNumber(clazz: KClass<T>): T =
  when (clazz) {
    Int::class -> toInt()
    Long::class -> toLong()
    Byte::class -> toByte()
    Short::class -> toShort()
    Float::class -> toFloat()
    Double::class -> toDouble()
    else -> throw AssertionError("Unsupported type $clazz")
  } as T

class TransformationStepSerializer : JsonSerializer<SerializableTransformationStep>() {
  override fun serialize(
    value: SerializableTransformationStep,
    gen: JsonGenerator,
    serializers: SerializerProvider,
  ) {
    gen.writeStartObject()
    gen.writeStringField(COMMAND_FACTORY_FIELD, value.factoryId)
    //compatibility
    gen.writeObjectFieldStart(COMMAND_PARAMS_FIELD)
    gen.writeObjectField("props", value.params)
    gen.writeEndObject()

    gen.writeEndObject()
  }
}

class TransformationStepDeserializer : JsonDeserializer<SerializableTransformationStep>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SerializableTransformationStep {
    val node = p.codec.readTree<JsonNode>(p)

    val commandIdNode = node.get(COMMAND_FACTORY_FIELD) ?: run {
      thisLogger().debug("Cannot find '$COMMAND_FACTORY_FIELD' field")
      throw IllegalArgumentException("Cannot find '$COMMAND_FACTORY_FIELD' field")
    }
    val factoryId = commandIdNode.asText()
    val paramNode = node.get(COMMAND_PARAMS_FIELD) ?: run {
      thisLogger().debug("Cannot find '$COMMAND_PARAMS_FIELD' field")
      throw IllegalArgumentException("Cannot find '$COMMAND_PARAMS_FIELD' field")
    }
    val props = paramNode.get("props")
    val param = if (props == null) mutableMapOf() else props.traverse(p.codec).readValueAs(Map::class.java) as Map<String, Any>

    return SerializableTransformationStep(factoryId, param)
  }
}