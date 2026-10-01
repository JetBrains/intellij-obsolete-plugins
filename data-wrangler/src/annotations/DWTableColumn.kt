package com.intellij.dataWrangler.annotations

import com.intellij.dataWrangler.executor.DataWranglerContext

/**
 * Indicate that certain fields in the data class represent columns for enabling column selection highlighting.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@DWParameterType
@CommandParameterName("Column")
@ValuesProvider(TableColumnsProvider::class)
annotation class DWTableColumn

class TableColumnsProvider: PossibleValuesProvider<String>(String::class) {
  override fun getValues(context: DataWranglerContext?): List<String> {
    return context?.getColumnNames() ?: emptyList()
  }
}

