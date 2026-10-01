package com.intellij.dbt.run

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtIcons
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.util.NotNullLazyValue


class DbtRunConfigurationType :
  ConfigurationTypeBase(ID,
                        DbtBundle.message("dbt.display.name"),
                        DbtBundle.message("dbt.run.configuration.description"),
                        NotNullLazyValue.createValue { DbtIcons.Dbt }
  ) {

  private val myFactory = DbtRunConfigurationFactory(this)

  init {
    addFactory(DbtRunConfigurationFactory(this))
  }

  fun getFactory(): DbtRunConfigurationFactory = myFactory

  companion object {
    const val ID = "DbtRunConfiguration"

    fun getInstance(): DbtRunConfigurationType {
      return runConfigurationType<DbtRunConfigurationType>()
    }
  }
}