// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.codeInspection.*
import com.intellij.helidon.config.HelidonParametrizedConfigKey
import com.intellij.helidon.config.isHelidonConfigFile
import com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyReference
import com.intellij.microservices.jvm.config.MicroservicesConfigBundle
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils
import com.intellij.microservices.jvm.config.yaml.ShowDuplicateKeysQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.yaml.psi.*

internal class HelidonYamlConfigInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    if (!hasHelidonLibrary(holder.project) ||
        !isHelidonConfigFile(holder.file)) {
      return PsiElementVisitor.EMPTY_VISITOR
    }

    return super.buildVisitor(holder, isOnTheFly, session)
  }

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    if (file !is YAMLFile) return null

    val holder = ProblemsHolder(manager, file, isOnTheFly)
    val yamlDocuments = file.documents
    for (document in yamlDocuments) {
      val duplicates = MultiMap<String, YAMLKeyValue>()
      document.acceptChildren(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
          super.visitElement(element)
          if (element !is YAMLKeyValue || element.key == null) return

          // only check key if scalar/no value given
          val valueElement = element.value
          if (valueElement != null &&
              valueElement !is YAMLScalar &&
              valueElement !is YAMLSequence) {
            return
          }
          if (valueElement != null) {
            if (valueElement is YAMLScalar) {
              MicroservicesConfigUtils.highlightValueReferences(valueElement, holder)
            }
            else {
              val sequence = valueElement as YAMLSequence
              for (item in sequence.items) {
                val itemValue = item.value as? YAMLScalar ?: continue
                MicroservicesConfigUtils.highlightValueReferences(itemValue, holder)
              }
            }
          }

          // todo skip List<T> items until ConfigKeyPath references are implemented
          if (PsiTreeUtil.getContextOfType(element, YAMLSequenceItem::class.java) != null) {
            return
          }
          val configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(element)
          if (configKey == null) return  // cannot check anything if key unknown

          if (!configKey.isAccessType(*MetaConfigKey.AccessType.MAP_GROUP)) {
            duplicates.putValue(configKey.name, element)
          }
        }
      })
      for ((configKey, values) in duplicates.entrySet()) {
        if (values.size == 1 || HelidonParametrizedConfigKey.getParametrizedConfigKey(configKey) != null) continue

        val showDuplicatesFix = ShowDuplicateKeysQuickFix(configKey, values)
        for (keyValue in values) {
          holder.registerProblem(keyValue!!,
                                 MicroservicesConfigBundle.message("config.duplicate.key", configKey),
                                 showDuplicatesFix)
        }
      }
    }
    return holder.resultsArray
  }
}