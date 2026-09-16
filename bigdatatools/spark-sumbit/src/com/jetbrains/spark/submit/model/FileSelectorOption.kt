package com.jetbrains.spark.submit.model

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsContexts
import java.util.function.Consumer
import javax.swing.Icon

class FileSelectorType(val id: String) {
  companion object {
    val CLUSTER_JAR = FileSelectorType("file.selector.artifact-jar")
    val LOCAL_JAR = FileSelectorType("file.selector.local-jar")
    val LOCAL_FILE = FileSelectorType("file.selector.local-file")
    val LOCAL_DIRECTORY = FileSelectorType("file.selector.local-directory")
    val SSH_JAR = FileSelectorType("file.selector.ssh-jar")
    val SSH_FILE = FileSelectorType("file.selector.ssh-file")
    val SSH_DIRECTORY = FileSelectorType("file.selector.ssh-directory")
  }
}

interface FileSelectorOption {
  val title: @NlsActions.ActionText String
  val tooltip: @NlsContexts.Tooltip String
  val icon: Icon

  val applicableForTypes: List<FileSelectorType>
  fun isAvailable(project: Project): Boolean
  fun select(context: FileSelectorContext, consumer: Consumer<SelectedArtifactInfo>)

  companion object {
    private val EP_NAME = ExtensionPointName.create<FileSelectorOption>("com.intellij.bigdatatools.file.selector.option")
    fun getForType(fileSelectorType: FileSelectorType) = EP_NAME.extensionList.filter { fileSelectorType in it.applicableForTypes }
  }
}

interface FileSelectorContext {
  val project: Project
  val prevSelected: List<SelectedArtifactInfo>
  val dialogTitle: @NlsContexts.DialogTitle String
  val hasBeforeTasks: Boolean
}

abstract class FileSelectorOptionBase(
  private val fileType: FileType,
  @NlsActions.ActionText override val title: String,
  @NlsContexts.Tooltip override val tooltip: String,
  override val icon: Icon
) : FileSelectorOption {
  override fun isAvailable(project: Project): Boolean = true

  protected val FileSelectorContext.prevSelectedPath: String?
    get() = prevSelected.firstOrNull {
      it.filePath.type == fileType
    }?.filePath?.getAsSelectedPath()

  protected inline fun <reified T : SelectedArtifactInfo> FileSelectorContext.prevSelectedArtifactInfo(): T? {
    return prevSelected.filterIsInstance<T>().firstOrNull()
  }
}

abstract class FileSelectorReturningOption(
  fileType: FileType,
  @NlsActions.ActionText title: String,
  @NlsContexts.Tooltip tooltip: String,
  icon: Icon
) : FileSelectorOptionBase(fileType, title, tooltip, icon) {
  abstract fun select(context: FileSelectorContext): SelectedArtifactInfo?
  override fun select(context: FileSelectorContext, consumer: Consumer<SelectedArtifactInfo>) {
    select(context)?.let {
      consumer.accept(it)
    }
  }
}