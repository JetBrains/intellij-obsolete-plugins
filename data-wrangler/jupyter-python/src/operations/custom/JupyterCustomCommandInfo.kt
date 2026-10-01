package com.intellij.dataWrangler.jupyterPython.operations.custom

import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc.Field
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import kotlin.reflect.KClass

data class JupyterCustomCommandInfo(val file: VirtualFile, val func: FunctionInfo) {
  data class FunctionInfo(
    val id: String,
    val displayName: @Nls String?,
    val description: @Nls String,
    val group: @NonNls String,
    val label: @Nls String?,
    val details: @Nls String?,
    val args: List<ArgInfo>
  )
  data class ArgInfo(
    val id: String,
    val displayName: @Nls String?,
    val type: String?,
    val def: String?
  ) {
    fun asField(): Field<JupyterCustomCommandParameters, *> =
      type.parseType(displayName, def).buildField(id)
  }
}

internal fun <T: Any> buildFieldForValue(id: String, v: T): Field<JupyterCustomCommandParameters, T> =
  buildTypeForValue(v).buildField(id)

private fun <T: Any> buildTypeForValue(v: T): FakeTypeBuilder<T> = FakeTypeBuilder(v).apply {
  val ct = tryCast<Collection<*>>()
  if (ct != null && v is Collection<*>) {
    val itemTypeDesc = buildItemTypeDesc<Collection<*>>(v)
    ct.parseCollectionType(itemTypeDesc, null)
    ct.setItemDesc(itemTypeDesc.buildDesc())
    //ct.setDefault(null.parseCollectionDef(ct.targetType, Any::class))
  }
  else {
    setDefault(null.parseSimpleDef(targetType))
  }
}

private fun <C : Collection<*>> buildItemTypeDesc(v: C): FakeTypeBuilder<out Any> {
  val item = v.firstOrNull()
  return item?.let { buildTypeForValue(item) } ?: FakeTypeBuilder(String::class)
}

private fun String?.parseType(displayName: String?, def: String?): FakeTypeBuilder<*> = FakeTypeBuilder(Any::class).apply {
  val idx = this@parseType?.indexOf("(")?.takeIf { it != -1 }
  displayName?.let { setDisplayName(displayName) }
  if (idx != null) {
    val main = substring(0, idx).trim()
    val args = substring(idx + 1, length - 1).trim()
    parseTypeImpl(main, args, def)
  }
  else {
    parseTypeImpl(this@parseType, null, def)
  }
}

private fun FakeTypeBuilder<*>.parseTypeImpl(
  text: String?,
  args: String?,
  def: String?
) {
  when (text) {
    "list" -> setTargetType(List::class).parseCollectionType(args?.parseType(null, null), def)
    "set" -> setTargetType(Set::class).parseCollectionType(args?.parseType(null, null), def)
    "column" -> setTypeAnno(DWTableColumn()).setTargetType(String::class).parseSimpleType(args?.parseStrings(), def)
    "string" -> setTargetType(String::class).parseSimpleType(args?.parseStrings(), def)
    "int" -> setTargetType(Int::class).parseSimpleType(args?.parseStrings(), def)
    "float" -> setTargetType(Float::class).parseSimpleType(args?.parseStrings(), def)
    else -> setTargetType(String::class).parseSimpleType(args?.parseStrings(), def)
  }
}

private fun <C: Collection<*>> FakeTypeBuilder<C>.parseCollectionType(
  item: FakeTypeBuilder<*>?,
  def: String?
) {
  val itemDesc = item?.buildDesc()
  val dv = def.parseCollectionDef(targetType, itemDesc?.targetType ?: String::class)
  setItemDesc(itemDesc)
  setDefault(dv)
}

private fun String.parseStrings(): List<String> = split(',').map { it.trim() }

private fun <T: Any> FakeTypeBuilder<T>.parseSimpleType(
  possibleValues: List<String>?,
  def: String?
) {
  possibleValues?.let { vs ->
    tryCast<Int>()?.setPossibleValues(vs.mapNotNull { it.toIntOrNull() }.toIntArray())
    tryCast<Float>()?.setPossibleValues(vs.mapNotNull { it.toFloatOrNull() }.toFloatArray())
    tryCast<String>()?.setPossibleValues(vs.toTypedArray())
  }
  setDefault(def.parseSimpleDef(targetType))
}

@Suppress("UNCHECKED_CAST")
private fun <C: Collection<*>, T: Any> String?.parseCollectionDef(colClazz: KClass<C>, itemClazz: KClass<T>): C {
  val values = if (this != null)
    substring(1, lastIndex).splitToSequence(',').map { it.trim().parseSimpleDef(itemClazz, true) }
  else
    emptySequence()
  return values.let { v ->
    when (colClazz) {
      List::class -> v.toList()
      Set::class -> v.toSet()
      else -> throw AssertionError("Unsupported type")
    } as C
  }
}
@Suppress("UNCHECKED_CAST")
private fun <T: Any> String?.parseSimpleDef(clazz: KClass<T>, quoted: Boolean = false): T =
  when (clazz) {
    Int::class -> this?.toIntOrNull() ?: 0
    Float::class -> this?.toFloatOrNull() ?: 0.0f
    Double::class -> this?.toDoubleOrNull() ?: 0.0
    String::class -> if (quoted && this != null) StringUtil.unescapeStringCharacters(substring(1, lastIndex)) else this ?: ""
    else -> throw AssertionError("Unsupported type $clazz")
  } as T
