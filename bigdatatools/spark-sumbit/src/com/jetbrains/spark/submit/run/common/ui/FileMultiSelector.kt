package com.jetbrains.spark.submit.run.common.ui

import com.intellij.bigdatatools.coreUi.settings.withValidator
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.icons.AllIcons
import com.intellij.internal.statistic.eventLog.events.BaseEventId
import com.intellij.internal.statistic.eventLog.events.EventId3
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.enteredTextSatisfies
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.delegate.Delegate
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtStatisticUtils
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.StatisticInfoProvider
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.model.FileSelectorOption
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.SelectedArtifactInfo
import com.jetbrains.spark.submit.model.inferSelectedArtifactInfo
import com.jetbrains.spark.submit.model.initBeforeTask
import java.util.concurrent.atomic.AtomicInteger

open class FileSelectorContextImpl(
  override val project: Project,
  override val dialogTitle: @DialogTitle String
) : FileSelectorContext {
  override val hasBeforeTasks: Boolean get() = true
  private var prevSelectedGetter: (() -> List<SelectedArtifactInfo>)? = null
  internal fun setMultiSelector(multiSelector: FileMultiSelector) {
    check(prevSelectedGetter == null)
    prevSelectedGetter = {
      multiSelector.files.asReversed().map { it.inferSelectedArtifactInfo() }
    }
  }
  internal fun setSelector(selector: FileSelector) {
    check(prevSelectedGetter == null)
    prevSelectedGetter = {
      listOf(selector.selectedArtifactInfo ?: selector.path.inferSelectedArtifactInfo())
    }
  }
  override val prevSelected: List<SelectedArtifactInfo> get() = checkNotNull(prevSelectedGetter).invoke()
}

class FileMultiSelector(
  private val fileSelectorType: FileSelectorType,
  private val filePathSerializer: FilePathSerializer,
  @NlsContexts.Label internal val labelText: String,
  private val fileSelectorContext: FileSelectorContextImpl
) : StatisticInfoProvider {

  internal val textField = ExpandableTextField()

  private val actionDelegate = Delegate<FileSelectorOption, Unit>()

  init {
    fileSelectorContext.setMultiSelector(this)
    textField.addExtension(ExtendableTextComponent.Extension.create(
      AllIcons.General.InlineAdd, AllIcons.General.InlineAddHover, null) {

      val fileSelectorOptions = FileSelectorOption.getForType(fileSelectorType)
      if (fileSelectorOptions.size == 1) {
        addFile(fileSelectorOptions.single())
      }
      else {

        val actions = ArrayList<AnAction>()

        fileSelectorOptions.forEach { fileType ->
          actions.add(DumbAwareAction.create(fileType.title, fileType.icon) {
            addFile(fileType)
          })
        }

        val popupMenu = ActionManager.getInstance().createActionPopupMenu("FilesMultiSelector", DefaultActionGroup(actions))

        popupMenu.component.show(textField, textField.width, textField.height)
        val menuOrigin = textField.locationOnScreen
        popupMenu.component.setLocation(menuOrigin.x + textField.width - popupMenu.component.preferredSize.width,
                                        menuOrigin.y + textField.height)
      }
    })
  }

  var files: List<FilePath>
    get() = ParametersListUtil.parse(textField.text).mapNotNull {
      filePathSerializer.fromText(it)
    }
    set(value) {
      textField.text = ParametersListUtil.join(value.map { filePathSerializer.toText(it) })
    }

  private fun addFile(fileTypeAction: FileSelectorOption) {
    actionDelegate.notify(fileTypeAction)

    fileTypeAction.select(fileSelectorContext) { selectedInfo ->
      ThreadingAssertions.assertEventDispatchThread()
      if (fileSelectorContext.hasBeforeTasks) {
        initBeforeTask(selectedInfo, this.textField)
      }
      this.files += selectedInfo.filePath
    }
  }

  var toolTipText: @NlsContexts.Tooltip String
    get() = textField.toolTipText
    set(value) {
      textField.toolTipText = value
    }

  fun withValidator(uiDisposable: Disposable, validate: (String) -> String?): FileMultiSelector {
    textField.withValidator(uiDisposable, validate)
    return this
  }

  fun isEmpty(): ComponentPredicate {
    return textField.enteredTextSatisfies { it.isEmpty() }
  }

  override fun attachCollector(eventId: BaseEventId, index: AtomicInteger, type: BdtConnectionType) {
    BdtStatisticUtils.attachCollector(textField, eventId, index, type)
  }

  override fun attachActionCollector(eventId: BaseEventId, index: AtomicInteger, type: BdtConnectionType) {
    @Suppress("UNCHECKED_CAST")
    val eventId3 = eventId as EventId3<Int, BdtConnectionType, Class<*>>
    actionDelegate.plusAssign {
      eventId3.log(index.incrementAndGet(), type, it.javaClass)
    }
  }
}

fun Panel.row(it: FileMultiSelector): Row {
  return row(it.labelText, it.textField)
}

fun MigBlock.row(it: FileMultiSelector) {
  row(it.labelText, it.textField)
}