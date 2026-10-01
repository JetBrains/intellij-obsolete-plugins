package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.annotations.DWParameterType
import com.intellij.dataWrangler.annotations.DWTableColumn
import com.intellij.dataWrangler.annotations.PossibleValuesProvider
import com.intellij.dataWrangler.annotations.ValuesProvider
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.jupyterPython.engine.PandasDataFrameType
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@DWParameterType(extends = DWTableColumn::class)
@ValuesProvider(NumericTableColumnsProvider::class)
annotation class DWTableNumericColumn

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@DWParameterType(extends = DWTableColumn::class)
@ValuesProvider(StringTableColumnsProvider::class)
annotation class DWTableStringColumn

internal class NumericTableColumnsProvider : PossibleValuesProvider<String>(String::class) {
  override fun getValues(context: DataWranglerContext?): List<String> {
    if (context !is PythonDataWranglerContext) return emptyList()
    return context.getColumns()
      .filter { it.type == PandasDataFrameType.FLOAT || it.type == PandasDataFrameType.INT }
      .map { it.name }
  }
}

internal class StringTableColumnsProvider : PossibleValuesProvider<String>(String::class) {
  override fun getValues(context: DataWranglerContext?): List<String> {
    if (context !is PythonDataWranglerContext) return emptyList()
    return context.getColumns().filter { it.type == PandasDataFrameType.STRING }.map { it.name }
  }
}