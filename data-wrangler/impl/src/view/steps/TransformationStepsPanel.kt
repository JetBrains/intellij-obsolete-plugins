package com.intellij.dataWrangler.impl.view.steps

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.ui.DataWranglerUiSession
import com.intellij.dataWrangler.impl.view.DWMainPanel
import com.intellij.dataWrangler.impl.view.DWProperties
import com.intellij.dataWrangler.impl.view.transformation.ParameterViewEvents
import com.intellij.dataWrangler.impl.view.transformation.TransformationsPanel
import com.intellij.dataWrangler.impl.view.transformation.TreeViewEvents
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.CollectionListModel
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.asSafely
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.launchOnShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants

private const val idDWSteps = "DataWrangler.Transformations.Popup"

internal class TransformationStepsPanel<C : DataWranglerContext>(
  private val session: DataWranglerUiSession<C>,
  private val transformationPanel: TransformationsPanel<C>,
) : JBPanel<TransformationStepsPanel<C>>(BorderLayout()), UiDataProvider {

  private val panel = DWMainPanel.createPanel(PROPERTIES.panelTitle, PROPERTIES.toolbarPlaceName,
                                              ActionManager.getInstance().getAction("DataWrangler.Steps.Export") as ActionGroup)

  private val stepsListModel: CollectionListModel<TransformationStep<*, out DataWranglerContext>> = CollectionListModel()
  private val stepsListView: JList<TransformationStep<*, out DataWranglerContext>> = JBList(stepsListModel).apply {
    selectionMode = ListSelectionModel.SINGLE_SELECTION
  }
  private val scrollPane = JBScrollPane(stepsListView, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply {
    border = JBUI.Borders.empty(UIUtil.getListCellPadding())
  }

  init {
    stepsListView.isFocusable = true
    border = JBUI.Borders.empty(UIUtil.getListCellPadding())
    panel.add(createList())
    add(panel, BorderLayout.NORTH)
  }

  override fun uiDataSnapshot(sink: DataSink) {
    if (stepsListView.selectedIndex == -1) return
    sink[SELECTED_DW_STEP] = stepsListModel.getElementAt(stepsListView.selectedIndex)
  }

  private fun createList(): DialogPanel {
    stepsListView.cellRenderer = ListCellRenderer { _, step, index, isSelected, isFocus ->
      val command = step?.createCommand()
      val comp = TransformationStepItemComponent(command?.getCommandLabel() ?: "", index, command?.getDescription() ?: "", true)
      // Uncomment after backend logic
      comp.setSelected(isSelected && isFocus)
      JBUI.Panels.simplePanel(comp)
        .withBorder(JBUI.Borders.empty(7, 0))
    }

    stepsListView.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (e.isPopupTrigger) {
          val index = stepsListView.locationToIndex(e.point)
          if (index != -1) {
            stepsListView.selectedIndex = index
          }
        }
      }
    })

    stepsListView.addListSelectionListener { event ->
      if (event.valueIsAdjusting) return@addListSelectionListener
      event.source.asSafely<JBList<TransformationStep<*, C>>>()?.let { list ->
        val step = list.selectedValue
        transformationPanel.showParameterPreview(step)
      }
    }

    PopupHandler.installPopupMenu(stepsListView, idDWSteps, idDWSteps)

    val panel = panel {
      add(scrollPane)
    }
    panel.launchOnShow(this::class.java.name) {
      session.backendSession.getTransformationStepsManager().apply {
        launch {
          getEventFlow().collect { stepsList ->
            stepsListModel.replaceAll(stepsList)
          }
        }
        getErrorsFlow().collect { throwable ->
          withContext(Dispatchers.EDT) {
            showError(throwable)
          }
        }
      }
    }

    panel.launchOnShow(this::class.java.name + "_" + transformationPanel.treeEventFlow::class.java.name) {
      transformationPanel.treeEventFlow.collect { event ->
        when (event) {
          is TreeViewEvents.CommandPickStarted<*> -> {
            stepsListView.clearSelection()
          }
        }
      }
    }

    panel.launchOnShow(this::class.java.name + "_" + transformationPanel.parameterEventFlow::class.java.name) {
      transformationPanel.parameterEventFlow.collect { event ->
        when (event) {
          is ParameterViewEvents.ParameterEditFinished<C> -> {
            session.backendSession.runTransformation(event.step)
          }
        }
      }
    }
    return panel
  }

  private fun showError(throwable: Throwable) {
    JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
      HtmlChunk.text(throwable.message ?: "Error").toString(),
      MessageType.ERROR,
      null
    )
      .setFadeoutTime(5000)
      .createBalloon()
      .showInCenterOf(panel)
  }

  companion object {
    val PROPERTIES = DWProperties.STEPS_PANEL_INFO

    val SELECTED_DW_STEP: DataKey<TransformationStep<*, out DataWranglerContext>> = DataKey.create<TransformationStep<*, *>>("DW_SELECTED_ELEMENT")
  }
}


class AddActionGroupPopup : DefaultActionGroup(), DumbAware {
  init {
    templatePresentation.apply {
      putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
      putClientProperty(ActionUtil.USE_SMALL_FONT_IN_TOOLBAR, true)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}