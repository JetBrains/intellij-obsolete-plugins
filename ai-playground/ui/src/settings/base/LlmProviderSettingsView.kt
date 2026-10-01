package com.intellij.aiplayground.ui.settings.base

import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.view.bind
import com.intellij.aiplayground.ui.settings.base.LlmProviderSettingsViewModel.ConnectionState
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.FilterComponent
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.speedSearch.FilteringListModel
import com.intellij.util.ui.ThreeStateCheckBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.ListCellRenderer
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

open class LlmProviderSettingsView<VM : LlmProviderSettingsViewModel<out LlmProviderSettings<*>>, S : LlmProviderSettingsView.Settings>(
  parentScope: CoroutineScope,
  protected val viewModel: VM,
  protected val settings: S,
) : JComponent(), Disposable {

  protected val coroutineScope: CoroutineScope = parentScope.childScope("LlmSettingsView")

  init {
    data class ModelItem(val id: String, val label: String) { override fun toString(): String = label }
    val originalModel = DefaultListModel<ModelItem>()
    val filteringModel = FilteringListModel(originalModel)
    val list = object : JBList<ModelItem>(filteringModel) {
      override fun getPreferredScrollableViewportSize(): Dimension {
        val rows = if (visibleRowCount > 0) visibleRowCount else 10
        val fm = getFontMetrics(font)
        val cellH = if (fixedCellHeight > 0) fixedCellHeight else fm.height + 4
        val height = (cellH * rows).coerceAtLeast(0)
        return Dimension(0, height)
      }
    }

    val scrollPane = JBScrollPane(list)
    val loadingPanel = JBLoadingPanel(BorderLayout(), this)
    loadingPanel.add(scrollPane, BorderLayout.CENTER)
    coroutineScope.launch {
      loadingPanel.startLoading()
    }

    var currentDisabled: Set<String> = emptySet()

    coroutineScope.launch {
      viewModel.availableModels.collectLatest { models ->
        val items = models.map { ModelItem(it.id.id, "${it.displayName} (${it.id.id})") }
        filteringModel.replaceAll(items)
        list.repaint()
        loadingPanel.stopLoading()
      }
    }

    coroutineScope.launch {
      viewModel.disabledModels.collectLatest { disabled ->
        currentDisabled = disabled
        list.repaint()
      }
    }

    val filterField = object : FilterComponent("LLM_MODELS_FILTER_IN_AI_PLAYGROUND", 10) {
      override fun filter() {
        filteringModel.setFilter { item -> item.label.contains(filter, ignoreCase = true) }
      }
    }
    (filterField.textEditor as? com.intellij.ui.components.JBTextField)
      ?.emptyText
      ?.setText(AIPlaygroundUIBundle.message("models.filter.placeholder"))

    list.cellRenderer = ListCellRenderer<Any?> { _, value, _, isSelected, _ ->
      val checkBox = JCheckBox()
      val item = value as? ModelItem
      checkBox.text = item?.label ?: ""
      val enabled = item != null && !currentDisabled.contains(item.id)
      checkBox.isSelected = enabled
      checkBox.isOpaque = false
      panel {
        row {
          cell(checkBox)
        }
      }
    }

    list.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val index = list.locationToIndex(e.point)
        if (index >= 0) {
          val item = filteringModel.getElementAt(index)
          val isEnabled = !currentDisabled.contains(item.id)
          if (isEnabled) viewModel.disableModels(listOf(item.id)) else viewModel.enableModels(listOf(item.id))
        }
      }
    })

    list.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_SPACE) {
          val index = list.selectedIndex
          if (index >= 0) {
            val item = filteringModel.getElementAt(index)
            val isEnabled = !currentDisabled.contains(item.id)
            if (isEnabled) viewModel.disableModels(listOf(item.id)) else viewModel.enableModels(listOf(item.id))
            e.consume()
          }
        }
      }
    })


    layout = BorderLayout()
    add(panel {
      row(AIPlaygroundUIBundle.message("name")) {
        textField().applyToComponent {
          document.whenTextChanged {
            viewModel.updateName(it)
          }
          bind(coroutineScope, viewModel.settings.map { it.displayName ?: "" })
        }.align(AlignX.FILL)
      }
      contributeToForm()
      row {
        cell()
        link(AIPlaygroundUIBundle.message("link.label.test.connection")) {
          viewModel.testConnection()
        }
        icon(AllIcons.Empty).applyToComponent {
          coroutineScope.launch {
            viewModel.connected.collect { connected ->
              isVisible = connected != ConnectionState.UNKNOWN
              when (connected) {
                ConnectionState.UNKNOWN -> {
                }
                ConnectionState.CONNECTING -> {
                  text = AIPlaygroundUIBundle.message("connecting")
                  icon = AnimatedIcon.Default()
                }
                ConnectionState.CONNECTED -> {
                  text = AIPlaygroundUIBundle.message("connected")
                  icon = AllIcons.General.GreenCheckmark
                }
                ConnectionState.FAILED -> {
                  text = AIPlaygroundUIBundle.message("failed.to.connect")
                  icon = AllIcons.General.Warning
                }
              }
            }
          }
        }
      }
      separator()
      row {
        threeStateCheckBox(AIPlaygroundUIBundle.message("checkbox.enable.all")).applyToComponent {
          isOpaque = false
          isThirdStateEnabled = true

          fun visibleIds(): List<String> = (0 until filteringModel.size).map { filteringModel.getElementAt(it).id }

          fun updateTriState() {
            val total = filteringModel.size
            isEnabled = total > 0
            if (total == 0) {
              setState(ThreeStateCheckBox.State.DONT_CARE)
              return
            }
            val ids = visibleIds()
            val enabledCount = ids.count { !currentDisabled.contains(it) }
            val state = when (enabledCount) {
              0 -> ThreeStateCheckBox.State.NOT_SELECTED
              total -> ThreeStateCheckBox.State.SELECTED
              else -> ThreeStateCheckBox.State.DONT_CARE
            }
            setState(state)
          }

          addActionListener {
            val ids = visibleIds()
            when (state) {
              ThreeStateCheckBox.State.SELECTED -> {
                viewModel.enableModels(ids)
              }
              ThreeStateCheckBox.State.NOT_SELECTED -> {
                viewModel.disableModels(ids)
              }
              ThreeStateCheckBox.State.DONT_CARE -> {
                viewModel.enableModels(ids)
              }
            }
          }

          // Recalculate state when data changes
          coroutineScope.launch {
            viewModel.disabledModels.collectLatest {
              updateTriState()
            }
          }
          coroutineScope.launch {
            viewModel.availableModels.collectLatest {
              updateTriState()
            }
          }
          filteringModel.addListDataListener(object : ListDataListener {
            override fun contentsChanged(e: ListDataEvent?) { updateTriState() }
            override fun intervalAdded(e: ListDataEvent?) { updateTriState() }
            override fun intervalRemoved(e: ListDataEvent?) { updateTriState() }
          })

          // Initialize
          updateTriState()
        }
      }
      row(AIPlaygroundUIBundle.message("label.models")) { }
      row { cell(filterField).align(AlignX.FILL) }
      row { cell(loadingPanel).align(AlignX.FILL).align(AlignY.FILL).resizableColumn() }.resizableRow()
    }, BorderLayout.CENTER)
  }

  override fun dispose() {
    coroutineScope.cancel()
  }

  protected open fun Panel.contributeToForm() {
  }

  open class Settings(val dialog: Boolean)

}