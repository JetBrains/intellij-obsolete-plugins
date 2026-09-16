package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.SimpleTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkDataFrameCreateSource
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.scala.SparkScalaMessagesBundle
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.CheckingTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfGenericTypeCheckError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.scala.DfScalaTypesMapper
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.types.ScParameterizedType
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable

class DfScalaCollectionTypeSourceProvider : DfAbstractMethodBasedTypeProvider() {
  companion object {
    private const val METHOD_NAME = "toDF"
    private const val BASE_PARAMETER_NAME = "colNames"
    private val BASE_CLASS_NAMES = setOf("Dataset", "DatasetHolder")
  }

  override fun getTypeSource(psiElement: PsiElement): DfTypeSource? {
    if (!checkMethodCallExpr(psiElement, setOf(METHOD_NAME), BASE_CLASS_NAMES)) return null
    if (psiElement.firstChild !is Typeable) return null

    val matched = foldStringVararg(psiElement.parent as ScMethodCall, BASE_PARAMETER_NAME)
    if (matched.isEmpty()) return null

    val maybeType = (psiElement.firstChild as Typeable).type()
    if (maybeType.isLeft) return null

    val tsDescription = SparkScalaMessagesBundle.message(
      "df.method.call.ts.description",
      DfComputingUtil.trimTextForDescription(psiElement.lastChild.text)
    )

    val result = convertType(maybeType.right().get(), psiElement.project, matched) ?: return null
    val baseSource = SimpleTypeSource(DfTypeSchema(result), tsDescription)

    SparkStatisticScala.logSchema(psiElement, SparkDataFrameCreateSource.EXPLICIT_SCHEMA)
    return if (matched.size == result.size)
      baseSource
    else
      CheckingTypeSource(
        baseSource,
        listOf(
          DfGenericTypeCheckError(
            null,
            SparkScalaMessagesBundle.message("df.seq.type.source.header.count.error", matched.size, result.size)
          )
        )
      )
  }

  private fun convertType(tpe: ScType, project: Project, matched: List<String>): LinkedHashMap<String, DfColumnType>? {
    val scalaMapper = DfScalaTypesMapper(project)

    when (tpe) {
      is ScParameterizedType -> {
        // We get a type like Dataset[Seq[(X,Y,Z)]], so we need first to extract Seq[(X,Y,Z)], then extract a Tuple type from it, and
        // finally extract and map every type included into the Tuple type. But if underlying type is not a tuple type, we assume,
        // that it is a single column schema
        val collectionTypes = tpe.typeArguments()
        if (collectionTypes.isEmpty) return null
        if (collectionTypes.head() !is ScParameterizedType) {
          val singleType = scalaMapper.convertTo(collectionTypes.head()) ?: return null
          val singleName = matched.firstOrNull() ?: return null

          return DfComputingUtil.linkedHashMapOf(Pair(singleName, singleType))
        }

        val ttypes = (collectionTypes.head() as ScParameterizedType).typeArguments()
        val i1 = matched.iterator()
        val i2 = ttypes.iterator()

        val result = LinkedHashMap<String, DfColumnType>()

        while (i1.hasNext() && i2.hasNext()) {
          val columnType = scalaMapper.convertTo(i2.next())
          if (columnType != null) result[i1.next()] = columnType
        }

        return result
      }
      is ScDesignatorType -> {
        //handling cases DataSet[Row]
        val maybeDataSetType = tpe.designatorSingletonType()
        if (maybeDataSetType.isEmpty) return null
        val maybeRowType = (maybeDataSetType.get() as? ScParameterizedType)?.typeArguments()?.headOption() ?: return null
        if (maybeRowType.isEmpty) return null

        val element = ((maybeRowType.get() as? ScDesignatorType)?.element() as? ScClass) ?: return null
        if (!element.isCase) return null

        val maybeConstructor = element.constructor()
        if (maybeConstructor.isEmpty) return null

        val i1 = matched.iterator()
        val i2 = maybeConstructor.get().valueParameters().iterator()

        val result = LinkedHashMap<String, DfColumnType>()

        while (i1.hasNext() && i2.hasNext()) {
          val maybeType = i2.next().type()
          result[i1.next()] = if (maybeType.isLeft) UnknownType else scalaMapper.convertTo(maybeType.right().get()) ?: UnknownType
        }

        return result
      }
      else -> return null
    }
  }
} // ((maybeType.right().get() as ScDesignatorType).designatorSingletonType().get() as ScParameterizedType).typeArguments().head() = X in DataSet[X]
//(((maybeType.right().get() as ScDesignatorType).designatorSingletonType().get() as ScParameterizedType).typeArguments().head() as ScDesignatorType).element()