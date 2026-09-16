package com.intellij.bigdatatools.zeppelin.drivers.actions

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.util.RfsFileUtil
import org.jetbrains.annotations.Nls

fun askConfirmation(project: Project?, @Nls message: String): Boolean = invokeAndWaitIfNeeded {
  val result = Messages.showYesNoDialog(project,
                                        message,
                                        ZepMessagesBundle.message("action.confirmation"),
                                        Messages.getQuestionIcon())
  return@invokeAndWaitIfNeeded result == Messages.YES
}

fun checkNotebookName(notebookName: String) = when {
  notebookName.contains(":") -> RfsFileUtil.Fail(ZepMessagesBundle.message("note.name.validation.invalid.symbols"))
  notebookName.isEmpty() -> RfsFileUtil.Fail(ZepMessagesBundle.message("note.name.validation.empty"))
  else -> RfsFileUtil.Success
}

internal fun suggestNewNoteName(rootName: ZeppelinRfsPath, driver: ZeppelinDriver): ZeppelinRfsPath {
  var index = 1
  val basicName = "Untitled Note "
  val fileSystem = driver.fileSystem
  while (index < 100 && fileSystem.getNoteInfoByPath(rootName.child(basicName + index.toString(), false)) != null) {
    index++
  }
  return rootName.child(basicName + index.toString(), false)
}


internal open class RelativeNoteNameValidator(val driver: Driver,
                                              private val dirRfsPath: RfsPath) : InputValidatorEx {
  override fun getErrorText(inputString: String) = when {
    inputString.isEmpty() -> ZepMessagesBundle.message("note.name.validation.empty")
    inputString.contains(Regex(";")) -> ZepMessagesBundle.message("note.name.validation.invalid.symbols")
    inputString.contains("..") -> ZepMessagesBundle.message("note.name.validation.double.dot")
    inputString.startsWith("/") -> ZepMessagesBundle.message("note.name.validation.start.with.slash")
    inputString.endsWith("/") -> ZepMessagesBundle.message("note.name.validation.end.with.slash")
    inputString.contains("//") -> ZepMessagesBundle.message("note.name.validation.double.slash")
    inputString.takeLastWhile { it != '/' }.length > 255 -> ZepMessagesBundle.message("note.name.validation.double.slash")
    isRelativePathExists(inputString) -> ZepMessagesBundle.message("note.name.validation.note.exists")
    else -> null
  }

  private fun isRelativePathExists(inputString: String) = try {
    (driver as ZeppelinDriver).fileSystem.noteExists(dirRfsPath.addRelative(inputString, isDirectory = false))
  }
  catch (t: Throwable) {
    false
  }

  override fun checkInput(inputString: String): Boolean = getErrorText(inputString) == null

  override fun canClose(inputString: String): Boolean = getErrorText(inputString) == null
}