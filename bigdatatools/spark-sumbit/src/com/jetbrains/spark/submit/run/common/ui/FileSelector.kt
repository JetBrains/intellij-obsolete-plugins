package com.jetbrains.spark.submit.run.common.ui

import com.intellij.internal.statistic.eventLog.events.BaseEventId
import com.intellij.internal.statistic.eventLog.events.EventId3
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.TextAccessor
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.delegate.Delegate
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtStatisticUtils
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.StatisticInfoProvider
import com.jetbrains.bigdatatools.common.ui.chooser.FileChooserUtil
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorOption
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.model.initBeforeTask
import org.jetbrains.annotations.Nls
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JComponent
import javax.swing.text.JTextComponent

abstract class FileSelector(
  private val filePathSerializer: FilePathSerializer,
  protected val fileSelectorContext: FileSelectorContextImpl
) : StatisticInfoProvider {

  init {
    fileSelectorContext.setSelector(this)
  }

  var selectedArtifactInfo: SelectedArtifactInfo? = null

  private val actionDelegate = Delegate<FileSelectorOption, Unit>()

  abstract val component: JComponent
  abstract val interactiveComponent: JTextComponent
  protected abstract val textAccessor: TextAccessor

  var path: FilePath
    get() {
      return filePathSerializer.fromText(textAccessor.text)
    }
    set(value) {
      textAccessor.text = filePathSerializer.toText(value)
    }

  @get:NlsContexts.Tooltip
  open var toolTipText: String
    get() = interactiveComponent.toolTipText
    set(@NlsContexts.Tooltip value) {
      interactiveComponent.toolTipText = value
    }

  protected fun selectFile(fileTypeAction: FileSelectorOption) {
    actionDelegate.notify(fileTypeAction)

    fileTypeAction.select(fileSelectorContext) { selectedInfo ->
      ThreadingAssertions.assertEventDispatchThread()
      if (fileSelectorContext.hasBeforeTasks) {
        initBeforeTask(selectedInfo, component)
      }
      this.path = selectedInfo.filePath
      this.selectedArtifactInfo = selectedInfo
    }
  }

  override fun attachCollector(eventId: BaseEventId, index: AtomicInteger, type: BdtConnectionType) {
    BdtStatisticUtils.attachCollector(interactiveComponent, eventId, index, type)
  }

  override fun attachActionCollector(eventId: BaseEventId, index: AtomicInteger, type: BdtConnectionType) {
    @Suppress("UNCHECKED_CAST")
    val eventId3 = eventId as EventId3<Int, BdtConnectionType, Class<*>>
    actionDelegate.plusAssign {
      eventId3.log(index.incrementAndGet(), type, it.javaClass)
    }
  }

  companion object {
    fun openLocalFile(@Nls(capitalization = Nls.Capitalization.Title) fileChooserTitle: String,
                      project: Project,
                      prevSelectedPath: String? = null) =
      FileChooserUtil.selectSingleFile(project, prevSelectedPath, fileChooserTitle)
  }
}