package com.intellij.bigdatatools.plugin.spark.assistance.statistic

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.getOrCreateUserDataUnsafe
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

object SparkStatisticCollector : CounterUsagesCollector() {
  private val group = EventLogGroup("bigdatatools.spark.analyze", 3)

  val schemaComputedEvent = group.registerEvent("dataframe.is.calculated", EventFields.LanguageById, EventFields.Enum<SparkDataFrameCreateSource>("source"))
  val inlayShownEvent = group.registerEvent("inlay.is.shown", EventFields.LanguageById, EventFields.Boolean("isConstant"), EventFields.Boolean("withSchema"))
  val inspectionShownEvent = group.registerEvent("inspection.is.shown", EventFields.LanguageById, EventFields.Enum<SparkDataFrameInspection>("kind"))
  val columnInCompletion = group.registerEvent("add.columns.to.completion", EventFields.LanguageById, EventFields.Count, EventFields.Boolean("isPartial"))

  override fun getGroup(): EventLogGroup = group
}

data object SparkStatisticScala : SparkStatisticHelper("Scala", SparkStatisticCollector)
data object SparkStatisticPython : SparkStatisticHelper("Python", SparkStatisticCollector)

sealed class SparkStatisticHelper(private val language: String, private val collector: SparkStatisticCollector) {

  private val PsiElement.document
    get() = PsiDocumentManager.getInstance(project).getDocument(containingFile)

  private fun <T> registerIfNotRegistered(psiElement: PsiElement, key: Key<Set<T>>, source: T): Unit? {
    val document = psiElement.document
                   ?: return null
    val alreadyRegistered = document.getOrCreateUserDataUnsafe(key) { emptySet() }
    if (source in alreadyRegistered)
      return null
    document.putUserData(key, alreadyRegistered + source)
    return Unit
  }

  private val initTriggeredKey = Key<Set<SparkDataFrameCreateSource>>("stat.bigdatatools.spark.analyze.triggered.dataframe.is.calculated")
  private val inspectionTriggerredKey = Key<Set<Pair<String?, SparkDataFrameInspection>>>("stat.bigdatatools.spark.analyze.triggered.inspection")
  private val inlayTriggerredKey = Key<Set<Pair<Boolean, Boolean>>>("stat.bigdatatools.spark.analyze.triggered.inlay")

  fun logSchema(psiElement: PsiElement, source: SparkDataFrameCreateSource) {
    registerIfNotRegistered(psiElement, initTriggeredKey, source) ?: return
    collector.schemaComputedEvent.log(language, source)
  }

  fun logInspection(psiElement: PsiElement, columnName: String?, inspectionKind: SparkDataFrameInspection) {
    registerIfNotRegistered(psiElement, inspectionTriggerredKey, columnName to inspectionKind) ?: return
    collector.inspectionShownEvent.log(language, inspectionKind)
  }

  fun logCompletion(count: Int, isPartial: Boolean) {
    collector.columnInCompletion.log(language, count, isPartial)
  }

  fun logInlay(psiElement: PsiElement, isCompileTimeConstant: Boolean, withSchema: Boolean) {
    registerIfNotRegistered(psiElement, inlayTriggerredKey, isCompileTimeConstant to withSchema) ?: return
    collector.inlayShownEvent.log(language, isCompileTimeConstant, withSchema)
  }
}