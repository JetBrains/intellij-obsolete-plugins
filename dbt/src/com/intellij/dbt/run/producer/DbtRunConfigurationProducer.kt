package com.intellij.dbt.run.producer

import com.intellij.database.console.runConfiguration.DatabaseScriptRunConfiguration
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.run.DBT_COMMAND_ARGUMENT_SELECT
import com.intellij.dbt.run.DbtRunConfiguration
import com.intellij.dbt.run.DbtRunConfigurationType
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.jinja.template.psi.impl.DjangoTemplateFileImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.sql.SqlFileType
import com.intellij.sql.psi.SqlLanguage

abstract class DbtBaseRunConfigurationProducer(private val dbtCommand: DbtCommand) : LazyRunConfigurationProducer<DbtRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory = DbtRunConfigurationType.getInstance().getFactory()

  override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
    return self.configuration is DbtRunConfiguration && other.configuration is DatabaseScriptRunConfiguration
  }

  override fun isConfigurationFromContext(configuration: DbtRunConfiguration, context: ConfigurationContext): Boolean {
    if (context.module == null || !DbtUtils.isDbtModule(context.module)) {
      return false
    }
    val file = context.location?.virtualFile ?: return false
    if (configuration.configurationModule.module != context.module) {
      return false
    }
    if (!configuration.isForFile(file)) {
      return false
    }
    if (configuration.getDbtCommand() == dbtCommand) {
      return true
    }
    if (configuration.getDbtCommand() == DbtCommand.BUILD && dbtCommand == DbtCommand.RUN) {
      // See DS-6184 Changes after editing Run configuration don't get applied
      return true
    }
    return false
  }

  abstract fun acceptLocation(file: PsiFile, module: Module): Boolean

  override fun setupConfigurationFromContext(configuration: DbtRunConfiguration,
                                             context: ConfigurationContext,
                                             sourceElement: Ref<PsiElement>): Boolean {
    var file = sourceElement.get().containingFile ?: return false
    val module = context.module ?: return false
    if (!acceptLocation(file, module)) {
      return false
    }
    if (file is DjangoTemplateFileImpl) {
      val sqlDialect = file.viewProvider.languages.firstOrNull { it.isKindOf(SqlLanguage.INSTANCE) } ?: return false
      file = file.viewProvider.getPsi(sqlDialect) ?: return false
    }
    if (file.fileType != SqlFileType.INSTANCE) return false
    configuration.name = dbtCommand.commandName + ": " + file.name
    configuration.setModule(context.module)
    configuration.setDbtCommand(dbtCommand)
    configuration.setDbtArguments(listOf(DBT_COMMAND_ARGUMENT_SELECT, file.name))
    return true
  }
}

class DbtRunRunConfigurationProducer : DbtBaseRunConfigurationProducer(DbtCommand.RUN) {
  override fun acceptLocation(file: PsiFile, module: Module) = DbtUtils.isUnderModelsDirectory(file.virtualFile, module)
}

class DbtTestRunConfigurationProducer : DbtBaseRunConfigurationProducer(DbtCommand.TEST) {
  override fun acceptLocation(file: PsiFile, module: Module) = DbtUtils.isUnderModelsDirectory(file.virtualFile, module) ||
                                                               DbtUtils.isUnderTestDirectory(file.virtualFile, module)
}

class DbtShowRunConfigurationProducer : DbtBaseRunConfigurationProducer(DbtCommand.SHOW) {
  override fun acceptLocation(file: PsiFile, module: Module) = DbtUtils.isUnderModelsDirectory(file.virtualFile, module)
}