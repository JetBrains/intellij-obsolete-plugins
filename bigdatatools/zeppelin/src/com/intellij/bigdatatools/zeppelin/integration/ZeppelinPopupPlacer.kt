package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.bigdatatools.zeppelin.notebook.parser.ZeppelinFileViewProvider
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.JList
import javax.swing.ListSelectionModel

class ZeppelinPopupPlacer(private val placers: Collection<ZeppelinAutoImportPlacer>) : ZeppelinAutoImportPlacer {
  override fun isAccepted(refsContainer: PsiElement): Boolean = placers.any { it.isAccepted(refsContainer) }

  override fun findAnchor(refsContainer: PsiElement, callback: (PsiElement?) -> Unit) {
    val file = refsContainer.containingFile
    val options = placers.filter { it.isAccepted(refsContainer) }

    if (options.isEmpty()) return callback(null)

    val isScala = (file.viewProvider as? ZeppelinFileViewProvider)?.let { ZeppelinAutoImportUtil.startsWithScala(it) } ?: false
    if (options.size == 1 || !isScala) return options[0].findAnchor(refsContainer, callback)

    @Suppress("DialogTitleCapitalization")
    val listPopup = JBPopupFactory.getInstance().createPopupChooserBuilder(options)
      .setModalContext(true)
      .setSelectedValue(options[0], false)
      .setSelectionMode(ListSelectionModel.SINGLE_SELECTION).setItemChosenCallback {
        it.findAnchor(refsContainer, callback)
      }
      .setRenderer(MySimpleListRenderer())
      .setTitle(ZepMessagesBundle.message("popup.placer.title"))
      .createPopup()

    listPopup.content.requestFocusInWindow()

    FileEditorManager.getInstance(file.project).selectedEditor?.let { fileEditor ->
      if (fileEditor.file == file.virtualFile) {
        (fileEditor as? TextEditor)?.editor?.let {
          listPopup.showInBestPositionFor(it)
          return
        }
      }
    }

    listPopup.showCenteredInCurrentWindow(file.project)
  }

  override fun getName() = ZepMessagesBundle.message("popup.placer.select.cell")

  private class MySimpleListRenderer : SimpleListCellRenderer<ZeppelinAutoImportPlacer>() {
    override fun customize(list: JList<out ZeppelinAutoImportPlacer>,
                           value: ZeppelinAutoImportPlacer?,
                           index: Int,
                           selected: Boolean,
                           hasFocus: Boolean) {
      value?.let {
        icon = it.getIcon()
        text = it.getName()
      }
    }
  }
}
