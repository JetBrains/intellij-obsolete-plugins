/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.spark.submit.run.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.ui.TextAccessor
import com.jetbrains.spark.submit.run.common.ui.SparkSubmitConfigurationEditor

class MasterComboBox : ComboBox<String>(arrayOf("local", "yarn", "spark://", "mesos://", "k8s://"),
                                        SparkSubmitConfigurationEditor.MINIMUM_COMBO_BOX_WIDTH), TextAccessor {
  private val accessor = object : TextComponentAccessor<ComboBox<String>> {
    override fun getText(component: ComboBox<String>): String = component.selectedItem?.toString() ?: ""

    override fun setText(component: ComboBox<String>, text: String) {
      component.selectedItem = text
    }
  }

  init {
    setEditable(true)
  }

  override fun getText(): String = accessor.getText(this)
  override fun setText(text: String): Unit = accessor.setText(this, text)
}