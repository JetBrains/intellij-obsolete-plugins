package com.jetbrains.spark.submit.run.common.ui

import com.intellij.bigdatatools.coreUi.settings.getValidationInfo
import com.intellij.bigdatatools.coreUi.settings.withNonEmptyValidator
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.ex.FileLookup
import com.intellij.openapi.fileChooser.ex.FileLookup.Finder
import com.intellij.openapi.fileChooser.ex.FileLookup.LookupFile
import com.intellij.openapi.fileChooser.ex.FileTextFieldImpl
import com.intellij.openapi.fileChooser.ex.LocalFsFinder
import com.intellij.openapi.fileChooser.ex.LocalFsFinder.FileChooserFilter
import com.intellij.openapi.fileChooser.impl.FileChooserFactoryImpl
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.TextAccessor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.DslComponentProperty
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorOption
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import javax.swing.text.JTextComponent

class TextFieldFileSelector(
  private val fileSelectorType: FileSelectorType,
  filePathSerializer: FilePathSerializer,
  fileSelectorContext: FileSelectorContextImpl,
  val defaultText: String = "",
) : FileSelector(filePathSerializer, fileSelectorContext) {

  override val component: TextFieldWithBrowseButton =
    object : TextFieldWithBrowseButton(JBTextField().also { it.emptyText.text = defaultText }, { innerSelectFile() }) {
      init {
        putClientProperty(DslComponentProperty.INTERACTIVE_COMPONENT, textField)
      }
      override fun installPathCompletion(fileChooserDescriptor: FileChooserDescriptor?, parent: Disposable?) {
        val application = ApplicationManager.getApplication()
        if (application == null || application.isUnitTestMode || application.isHeadlessEnvironment) return
        val finder = LocalFsFinder()
        val filter = FileChooserFilter(fileChooserDescriptor, true)

        class MyLookupFile(val wrapped: LookupFile) : LookupFile by wrapped {
          override fun getAbsolutePath() = "${FileType.UPLOAD.scheme}${wrapped.absolutePath}"
          override fun getChildren(filter: FileLookup.LookupFilter?) = wrapped.getChildren(filter).map { MyLookupFile(it) }
          override fun getParent() = wrapped.parent?.let { MyLookupFile(it) }
        }
        object : FileTextFieldImpl(childComponent, object : Finder {
          override fun find(path: String): LookupFile? {
            return finder.find(path.removePrefix(FileType.UPLOAD.scheme))?.let { MyLookupFile(it) }
          }

          override fun normalize(path: String): String {
            return "${FileType.UPLOAD.scheme}${finder.normalize(path.removePrefix(FileType.UPLOAD.scheme))}"
          }

          override fun getSeparator(): String {
            return finder.separator
          }
        }, FileLookup.LookupFilter { file -> filter.isAccepted(file) }, FileChooserFactoryImpl.getMacroMap(), parent) {
          init {
            myAutopopup = true
          }
        }
      }
    }

  override val textAccessor: TextAccessor
    get() = component
  override val interactiveComponent: JTextComponent
    get() = component.textField

  protected fun innerSelectFile() {
    val fileSelectorOptions = FileSelectorOption.getForType(fileSelectorType)
    if (fileSelectorOptions.size == 1) {
      selectFile(fileSelectorOptions.single())
    }
    else {
      val actions = ArrayList<AnAction>()
      fileSelectorOptions.forEach { fileTypeAction ->
        fileTypeAction.isAvailable(fileSelectorContext.project)
        actions.add(object : AnAction(fileTypeAction.title, fileTypeAction.tooltip, fileTypeAction.icon) {
          override fun actionPerformed(e: AnActionEvent) {
            selectFile(fileTypeAction)
          }
        })
      }

      val popupMenu = ActionManager.getInstance().createActionPopupMenu("SparkSubmitFileSelector", DefaultActionGroup(actions))
      popupMenu.component.show(component, component.width, component.height)
      val menuOrigin = component.locationOnScreen
      popupMenu.component.setLocation(menuOrigin.x + component.width - popupMenu.component.preferredSize.width,
                                      menuOrigin.y + component.height)
    }
  }

}

fun FileSelector.withNonEmptyValidator(uiDisposable: Disposable): FileSelector {
  interactiveComponent.withNonEmptyValidator(uiDisposable)
  return this
}

fun FileSelector.getFileSelectorValidationInfo() = interactiveComponent.getValidationInfo()
