package com.intellij.bigdatatools.visualization.inlays.style

import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.fields.IntegerField
import com.jetbrains.bigdatatools.common.settings.wrappers.CheckBoxWrapper
import com.jetbrains.bigdatatools.common.settings.wrappers.ComponentWrapper
import com.jetbrains.bigdatatools.common.settings.wrappers.TextFieldWrapper
import javax.swing.JComponent

class InlaysConfigurationPanel {

  private val panel: JComponent

  //private val cellHighlightMode = ComboBoxWrapper(ComboBox(HighlightMode.values()),
  //                                                { InlaysConfig.instance.cellHighlightMode },
  //                                                { value -> InlaysConfig.instance.cellHighlightMode = value }).apply {
  //  label = JLabel(VisMessagesBundle.message("style.highlightSelectedCell"))
  //}

  //private val outputHighlightMode = ComboBoxWrapper(ComboBox(HighlightMode.values()),
  //                                                  { InlaysConfig.instance.outputHighlightMode },
  //                                                  { value -> InlaysConfig.instance.outputHighlightMode = value }).apply {
  //  label = JLabel(VisMessagesBundle.message("style.highlightOutput"))
  //}

  //private val drawSeparatorLine = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("style.drawSeparatorLine")),
  //                                                { InlaysConfig.instance.drawSeparatorLine },
  //                                                { value -> InlaysConfig.instance.drawSeparatorLine = value })

  private val showBottomInfo = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("style.showBottomInfo")),
                                               { InlaysConfig.getInstance().showBottomInfo },
                                               { value -> InlaysConfig.getInstance().showBottomInfo = value }).apply {
    component.toolTipText = VisMessagesBundle.message("style.showBottomInfo.hint")
  }

  //private val inlayGutterCollapsible = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("style.outputCollapsibleFromGutter")),
  //                                                     { InlaysConfig.instance.inlayGutterCollapsible },
  //                                                     { value -> InlaysConfig.instance.inlayGutterCollapsible = value })

  //private val outputLayout = ComboBoxWrapper(ComboBox(InlaysOutputLayout.values()),
  //                                           { InlaysConfig.instance.outputLayout },
  //                                           { value -> InlaysConfig.instance.outputLayout = value }).apply {
  //  label = JLabel(VisMessagesBundle.message("style.outputLayout"))
  //}

  //private val transparentOutput = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("style.outputTransparent")),
  //                                                { InlaysConfig.instance.transparentOutput },
  //                                                { value -> InlaysConfig.instance.transparentOutput = value })
  //
  //private val transparentGutter = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("style.gutterTransparent")),
  //                                                { InlaysConfig.instance.transparentGutter },
  //                                                { value -> InlaysConfig.instance.transparentGutter = value })
  //
  //private val cellToolbar = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("style.enableCellToolbar")),
  //                                          { InlaysConfig.instance.cellToolbar },
  //                                          { value -> InlaysConfig.instance.cellToolbar = value })
  //
  //private val cellToolbarStyle = ComboBoxWrapper(ComboBox(InlaysToolbarStyle.values()),
  //                                               { InlaysConfig.instance.cellToolbarStyle },
  //                                               { value -> InlaysConfig.instance.cellToolbarStyle = value }).apply {
  //  label = JLabel(VisMessagesBundle.message("style.cellToolbarStyle"))
  //}
  //
  //private val cellToolbarPosition = ComboBoxWrapper(ComboBox(OutputToolbarPosition.values()),
  //                                                  { InlaysConfig.instance.outputToolbarPosition },
  //                                                  { value -> InlaysConfig.instance.outputToolbarPosition = value }).apply {
  //  label = JLabel(VisMessagesBundle.message("style.cellToolbarPosition"))
  //}
  //
  //private val outputToolbarStyle = ComboBoxWrapper(ComboBox(InlaysToolbarStyle.values()),
  //                                                 { InlaysConfig.instance.outputToolbarStyle },
  //                                                 { value -> InlaysConfig.instance.outputToolbarStyle = value }).apply {
  //  label = JLabel(VisMessagesBundle.message("style.outputToolbarStyle"))
  //}

  private val convertTextToTable = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("behaviour.convertTextToTable")),
                                                   { InlaysConfig.getInstance().convertTextToTable },
                                                   { value -> InlaysConfig.getInstance().convertTextToTable = value }).apply {
    component.toolTipText = VisMessagesBundle.message("behaviour.convertTextToTable.hint")
  }

  private val specialDoubleConvertion = CheckBoxWrapper(JBCheckBox(VisMessagesBundle.message("behaviour.specialDoubleConvertion")),
                                                        { InlaysConfig.getInstance().specialDoubleConvertion },
                                                        { value -> InlaysConfig.getInstance().specialDoubleConvertion = value }).apply {
    component.toolTipText = VisMessagesBundle.message("behaviour.specialDoubleConvertion.hint")
  }

  private val specialDoubleConvertionLength = TextFieldWrapper(IntegerField(null, 1, Int.MAX_VALUE),
                                                               { InlaysConfig.getInstance().specialDoubleConvertionLength.toString() },
                                                               { value ->
                                                                 InlaysConfig.getInstance().specialDoubleConvertionLength = value.toIntOrNull()
                                                                                                                            ?: 16
                                                               }).apply {
    component.toolTipText = VisMessagesBundle.message("behaviour.specialDoubleConvertionLength.hint")
  }

  //private val browserStrategy = ComboBoxWrapper(ComboBox(BrowserStrategy.values()),
  //                                              { InlaysConfig.instance.browserStrategy },
  //                                              { value -> InlaysConfig.instance.browserStrategy = value }).apply {
  //  label = JLabel(VisMessagesBundle.message("style.browserStrategy"))
  //}

  //private val componentsList = arrayOf(cellHighlightMode,
  //                                     outputHighlightMode,
  //                                     drawSeparatorLine,
  //                                     showBottomInfo,
  //                                     inlayGutterCollapsible,
  //                                     outputLayout,
  //                                     transparentOutput,
  //                                     transparentGutter,
  //                                     cellToolbar,
  //                                     cellToolbarStyle,
  //                                     outputToolbarStyle,
  //                                     cellToolbarPosition,
  //                                     convertTextToTable,
  //                                     browserStrategy,
  //                                     editorSoftWraps)

  private val componentsList = arrayOf(showBottomInfo,
                                       convertTextToTable,
                                       specialDoubleConvertion,
                                       specialDoubleConvertionLength)

  init {
    panel = MigPanel().apply {
      row(showBottomInfo)
      row(convertTextToTable)
      add(MigPanel().apply {
        add(specialDoubleConvertion.component)
        add(specialDoubleConvertionLength.component)
      }, UiUtil.pushXSpanXWrap)
    }

    specialDoubleConvertion.component.addActionListener {
      updateSpecialDoubleConvertionLengthState()
    }

    updateSpecialDoubleConvertionLengthState()
  }

  private fun MigPanel.row(component: ComponentWrapper) {
    val label = component.label
    if (label == null) {
      add(component.component, UiUtil.spanXWrap)
    }
    else {
      shortRow(label, component.component)
    }
  }

  private fun updateSpecialDoubleConvertionLengthState() {
    specialDoubleConvertionLength.component.isEnabled = specialDoubleConvertion.component.isSelected
  }

  fun getComponent() = panel
  fun reset() = componentsList.forEach { it.reset() }
  fun apply() = componentsList.forEach {
    it.apply()
    InlaysConfig.getInstance().notifyChange()
  }

  fun isModified() = componentsList.find { it.isModified() } != null
}