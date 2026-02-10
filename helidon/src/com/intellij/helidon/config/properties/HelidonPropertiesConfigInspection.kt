// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.properties

import com.intellij.codeInspection.*
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonBundle
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.microservices.jvm.config.properties.IndexAccessTextProcessor
import com.intellij.microservices.jvm.config.ConfigKeyPathReference
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

internal class HelidonPropertiesConfigInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    if (!hasHelidonLibrary(holder.project) ||
        !isHelidonConfigFile(holder.file)) {
      return PsiElementVisitor.EMPTY_VISITOR
    }

    return super.buildVisitor(holder, isOnTheFly, session)
  }

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    if (file !is PropertiesFile) return null

    val holder = ProblemsHolder(manager, file, isOnTheFly)
    for (property in file.properties) {
      ProgressManager.checkCanceled()
      if (property !is PropertyImpl) continue

      val propertyKey = HelidonPropertiesUtils.getPropertyKey(property) ?: continue
      val configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(propertyKey)
      if (configKey == null) {
        // do not highlight unresolved keys
        // cannot check value if key (type) unknown
        continue
      }

      highlightIndexAccessExpressions(holder, propertyKey, configKey)

      // resolve Map key/property path expressions
      if (MetaConfigKey.MAP_OR_INDEXED_WITHOUT_KEY_HINTS_CONDITION.value(configKey)) {
        for (reference in propertyKey.references) {
          if (reference !is ConfigKeyPathReference || reference.isSoft) {
            continue
          }
          if (reference.resolve() == null) {
            holder.registerProblem(reference,
                                   ProblemsHolder.unresolvedReferenceMessage(reference),
                                   ProblemHighlightType.ERROR)
          }
        }
      }

      // check value reference
      val valueElement: PropertyValueImpl = HelidonPropertiesUtils.getPropertyValue(property) ?: continue
      MicroservicesConfigUtils.highlightValueReferences(valueElement, holder)
    }
    return holder.resultsArray
  }

  private fun highlightIndexAccessExpressions(holder: ProblemsHolder,
                                              propertyKey: PropertyKeyImpl,
                                              configKey: MetaConfigKey) {
    object : IndexAccessTextProcessor(propertyKey.text, configKey) {
      override fun onMissingClosingBracket(startIdx: Int) {
        holder.registerProblem(propertyKey, HelidonBundle.message("helidon.inspections.config.properties.missing.closing.bracket"),
                               ProblemHighlightType.ERROR, TextRange.from(startIdx, 1))
      }

      override fun onMissingIndexValue(startIdx: Int) {
        holder.registerProblem(propertyKey, HelidonBundle.message("helidon.inspections.config.properties.missing.index.value"),
                               ProblemHighlightType.ERROR, TextRange.from(startIdx, 2))
      }

      override fun onBracket(startIdx: Int) {}
      override fun onIndexValue(indexValueRange: TextRange) {}
      override fun onIndexValueNotInteger(indexValueRange: TextRange) {
        holder.registerProblem(propertyKey, HelidonBundle.message("helidon.inspections.config.properties.non.integer.index"),
                               ProblemHighlightType.ERROR, indexValueRange)
      }
    }.process()
  }
}