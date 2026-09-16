package com.jetbrains.spark.submit.run.cluster.ui

import com.intellij.bigdatatools.coreUi.ui.components.BdtGroupRender
import com.intellij.icons.AllIcons
import com.intellij.internal.statistic.eventLog.events.BaseEventId
import com.intellij.internal.statistic.eventLog.events.EventId3
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.DslComponentProperty
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtStatisticUtils
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.StatisticInfoProvider
import com.jetbrains.bigdatatools.common.util.async.JobHandle
import com.jetbrains.spark.submit.run.cluster.AddConnectionOption
import com.jetbrains.spark.submit.run.cluster.LoadingRemoteTargetOption
import com.jetbrains.spark.submit.run.cluster.NotSelectedRemoteTargetOption
import com.jetbrains.spark.submit.run.cluster.RemoteTarget
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetOption
import com.jetbrains.spark.submit.run.cluster.RemoteTargetProvider
import com.jetbrains.spark.submit.run.cluster.SelectableRemoteTargetOption
import com.jetbrains.spark.submit.run.cluster.UnresolvedRemoteTargetOption
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JPanel

class ClustersComboBox(val project: Project, coroutineScope: CoroutineScope) : JPanel(BorderLayout()), StatisticInfoProvider {
  private var isTemplate: Boolean? = null
  fun setIsTemplate(value: Boolean) {
    check(isTemplate == null)
    isTemplate = value
    loadValues()
  }
  private var targets = listOf<RemoteTargetOption?>()
  private val addConnections
    get() = RemoteTargetProvider.getAllAddConnections(project)

  private val comboBoxModel = MutableCollectionComboBoxModel<RemoteTargetOption>()
  private val myComboBox = ComboBox(comboBoxModel)
  private val myComponent = ComponentWithBrowseButton<ComboBox<RemoteTargetOption>>(myComboBox) { loadValues() }


  var changeListener: ((RemoteTarget?) -> Unit)? = null
  var deselectListener: ((RemoteTargetOption?) -> Unit)? = null

  // This method can be invoked in component constructor,
  // so component can be not attached to window here and even when dispatching next EDT event
  private val loadValuesJob = JobHandle(coroutineScope.childScope(ModalityState.any().asContextElement()))

  private var prevSelected: RemoteTargetId? = null

  init {
    myComponent.setButtonIcon(AllIcons.General.InlineRefresh)
    add(myComponent, BorderLayout.CENTER)

    val groups = mutableListOf("" to addConnections)

    myComboBox.renderer = object : BdtGroupRender<RemoteTargetOption?>(myComboBox, groups) {
      override fun customize(item: SimpleColoredComponent, value: RemoteTargetOption?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
        super.customize(item, value, index, isSelected, cellHasFocus)

        if (value != null) {
          value.setupItem(item)
        }
        else {
          item.icon = null
          item.append(SparkMessagesBundle.message("cluster.status.no.target"), SimpleTextAttributes.ERROR_ATTRIBUTES)
        }
      }
    }

    putClientProperty(DslComponentProperty.VISUAL_PADDINGS, myComponent.getClientProperty(DslComponentProperty.VISUAL_PADDINGS))
    myComboBox.addItemListener {
      when (it.stateChange) {
        ItemEvent.DESELECTED -> {
          val item = myComboBox.item
          deselectListener?.invoke(item)
        }
        ItemEvent.SELECTED -> {
          when (val item = myComboBox.item) {
            null -> {}
            LoadingRemoteTargetOption -> {}
            NotSelectedRemoteTargetOption -> {
              prevSelected = null
              changeListener?.invoke(null)
            }
            is UnresolvedRemoteTargetOption -> {}
            is AddConnectionOption -> {
              setSelected(prevSelected)
              ApplicationManager.getApplication().invokeLater {
                if (item.onClick())
                  loadValues()
              }
            }
            is RemoteTarget -> {
              prevSelected = item.id
              changeListener?.invoke(item)
            }
          }
        }
      }
    }
  }

  fun getSelected(): RemoteTarget? = myComboBox.item as? RemoteTarget
  fun getSelectedOrPrevId(): RemoteTargetId? = getSelected()?.id ?: prevSelected
  fun isSelectedValid(): Boolean = getSelected() is RemoteTarget

  fun setSelected(targetId: RemoteTargetId?) {
    prevSelected = targetId
    val targetCluster = comboBoxModel.items.firstOrNull { it.id == targetId }
    comboBoxModel.selectedItem = targetCluster
  }

  @RequiresEdt
  fun loadValues(itemToSelect: RemoteTargetId? = null) {
    loadValuesJob.cancelAndLaunch {
      withContext(Dispatchers.EDT) {
        refreshList(listOf(LoadingRemoteTargetOption), RemoteTargetOption.LOADING_ID)
      }
      targets = RemoteTargetProvider.getAllTargets(project)
      withContext(Dispatchers.EDT) {
        val notSelectedOption = listOfNotNull(NotSelectedRemoteTargetOption.takeIf { isTemplate != false })
        refreshList(notSelectedOption + targets + addConnections, itemToSelect ?: prevSelected)
      }
    }
  }

  @RequiresEdt
  private fun refreshList(targets: List<RemoteTargetOption?>, itemToSelect: RemoteTargetId?) {
    val selectableTargets = targets.filterIsInstance<SelectableRemoteTargetOption>()
    val selected = selectableTargets.firstOrNull { it.id == itemToSelect }
                   ?: selectableTargets.firstOrNull().takeIf { prevSelected == null }
                   ?: prevSelected?.let { UnresolvedRemoteTargetOption(it.name ?: SparkMessagesBundle.message("cluster.status.no.target"), it) }
    comboBoxModel.update(listOfNotNull(selected.takeIf { it is UnresolvedRemoteTargetOption }) + targets)
    myComboBox.selectedItem = selected
    if (prevSelected != null && prevSelected != selected?.id && selected is RemoteTarget) {
      changeListener?.invoke(selected)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun attachCollector(eventId: BaseEventId, index: AtomicInteger, type: BdtConnectionType) {
    val event = eventId as EventId3<Int, BdtConnectionType, String>
    myComboBox.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        return@addItemListener
      }

      val item = myComboBox.item?.type ?: return@addItemListener
      event.log(index.incrementAndGet(), type, item.name)
    }
  }

  override fun attachActionCollector(eventId: BaseEventId, index: AtomicInteger, type: BdtConnectionType) {
    BdtStatisticUtils.attachActionCollector(myComponent, eventId, index, type)
  }
}
