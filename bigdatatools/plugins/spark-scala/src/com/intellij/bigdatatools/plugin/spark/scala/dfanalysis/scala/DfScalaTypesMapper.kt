package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.scala

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypesMapper
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.ScalaTypeSystem
import org.jetbrains.plugins.scala.lang.psi.types.api.JavaArrayType
import org.jetbrains.plugins.scala.lang.psi.types.api.StdTypes
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable
import scala.Function0

class DfScalaTypesMapper(private val project: Project) : DfTypesMapper<ScType?>() {
  private val stdTypes = StdTypes.instance(project)

  override fun byteType(): ScType? = stdTypes.Byte()

  override fun shortType(): ScType? = stdTypes.Short()

  override fun integerType(): ScType? = stdTypes.Int()

  override fun longType(): ScType? = stdTypes.Long()

  override fun floatType(): ScType? = stdTypes.Float()

  override fun doubleType(): ScType? = stdTypes.Double()

  override fun decimalType(): ScType? = designateType("java.math.BigDecimal")

  override fun stringType(): ScType? = designateType("java.lang.String")

  override fun binaryType(): ScType = JavaArrayType(booleanType())

  override fun booleanType(): ScType = stdTypes.Boolean()

  override fun timestampType(): ScType? = designateType("java.time.Instant")

  override fun dateType(): ScType? = designateType("java.time.LocalDate")

  override fun bottomType(): ScType? = stdTypes.Nothing()

  override fun valType(): ScType? = stdTypes.AnyVal()

  private fun designateType(fqn: String): ScType? {
    val psiClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project)) ?: return null
    if (psiClass is Typeable) return (psiClass as Typeable).type()?.getOrElse (
      object : Function0<ScType?> { // incorrect type inferred if converted to lambda
        override fun apply(): ScType? = null
      }
    )

    val psiType = JavaPsiFacade.getInstance(project).elementFactory.createType(psiClass)
    return ScalaTypeSystem.instance(project).toScType(psiType, false, false)
  }
}