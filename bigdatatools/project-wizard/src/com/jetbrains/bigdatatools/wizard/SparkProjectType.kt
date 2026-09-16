package com.jetbrains.bigdatatools.wizard

import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import org.jetbrains.annotations.Nls

enum class SparkProjectType(@Nls val title: String) {
  BATCH(WizardMessageBundle.message("bdt.wizard.type.batch")), STREAMING(WizardMessageBundle.message("bdt.wizard.type.streaming"))
}