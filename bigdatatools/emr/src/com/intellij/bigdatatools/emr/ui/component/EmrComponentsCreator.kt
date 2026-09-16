package com.intellij.bigdatatools.emr.ui.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.jetbrains.spark.submit.run.common.ui.SparkSubmitConfigurationEditor
import software.amazon.awssdk.services.emr.model.ActionOnFailure
import java.util.Locale
import javax.swing.JList

object EmrComponentsCreator {
  fun createActionOnFailureField(): ComboBox<ActionOnFailure> {
    return ComboBox(ActionOnFailure.knownValues().toTypedArray(), SparkSubmitConfigurationEditor.MINIMUM_COMBO_BOX_WIDTH).apply {
      selectedItem = ActionOnFailure.CONTINUE
      setRenderer(object : SimpleListCellRenderer<ActionOnFailure>() {
        override fun customize(list: JList<out ActionOnFailure>,
                               value: ActionOnFailure,
                               index: Int,
                               selected: Boolean,
                               hasFocus: Boolean) {
          @Suppress("HardCodedStringLiteral")
          text = value.name.lowercase().replace("_", " ").replaceFirstChar {
            if (it.isLowerCase())
              it.titlecase(Locale.getDefault())
            else
              it.toString()
          }
        }
      })
    }
  }
}