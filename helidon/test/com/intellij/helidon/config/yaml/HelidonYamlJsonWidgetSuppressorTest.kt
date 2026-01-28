// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.helidon.HelidonHighlightingTestCase
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.jetbrains.jsonSchema.widget.JsonSchemaStatusWidgetFactory


class HelidonYamlJsonWidgetSuppressorTest : HelidonHighlightingTestCase() {
  fun testApplicationYaml() {
    myFixture.configureByText("application.yml", "")
    val widget = createJsonSchemaStatusWidget(project)
    disposeOnTearDown(widget)
    widget.updateInTests(true)
    assertFalse(widget.component.isVisible)
  }

  fun testNotConfigYamlFile() {
    myFixture.configureByText("not-application.yml", "")
    val widget = createJsonSchemaStatusWidget(project)
    disposeOnTearDown(widget)
    widget.updateInTests(true)
    assertTrue(widget.component.isVisible)
  }

  private fun createJsonSchemaStatusWidget(project: Project): EditorBasedStatusBarPopup {
    val widgetFactory: StatusBarWidgetFactory? =
      StatusBarWidgetFactory.EP_NAME.findExtension<JsonSchemaStatusWidgetFactory?>(JsonSchemaStatusWidgetFactory::class.java)
    assertNotNull(widgetFactory)
    return widgetFactory!!.createWidget(
      project,
      (project as ComponentManagerEx).getCoroutineScope()
    ) as EditorBasedStatusBarPopup
  }
}