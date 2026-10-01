package com.intellij.dbt.run

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.run.producer.DbtRunRunConfigurationProducer
import com.intellij.dbt.run.producer.DbtShowRunConfigurationProducer
import com.intellij.dbt.run.producer.DbtTestRunConfigurationProducer
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.sql.psi.SqlFile
import java.util.function.Function

class DbtRunLineMarkerContributor : RunLineMarkerContributor() {
  private val RUN_DBT_ELEMENT_TOOLTIP_PROVIDER: Function<PsiElement, String> = Function { it: PsiElement? ->
    DbtBundle.message("dbt.run.line.marker.tooltip")
  }

  override fun getInfo(element: PsiElement): Info? {
    val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
    if (DbtUtils.isDbtModule(module) && element is SqlFile) {
      val actions = mutableListOf<AnAction>()
      if (DbtRunRunConfigurationProducer().acceptLocation(element, module)) {
        actions.add(ActionManager.getInstance().getAction("dbtRunModelAction"))
      }
      if (DbtTestRunConfigurationProducer().acceptLocation(element, module)) {
        actions.add(ActionManager.getInstance().getAction("dbtTestModelAction"))
      }
      if (DbtShowRunConfigurationProducer().acceptLocation(element, module)) {
        actions.add(ActionManager.getInstance().getAction("dbtPreviewModelAction"))
      }

      if (actions.isNotEmpty()) {
        return Info(AllIcons.RunConfigurations.TestState.Run, actions.toTypedArray(), RUN_DBT_ELEMENT_TOOLTIP_PROVIDER)
      }
    }
    return null
  }
}
