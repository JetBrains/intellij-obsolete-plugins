package com.intellij.python.huggingFace.cacheManager.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder.Companion.yesNo
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCardsUsageCollector
import com.intellij.python.huggingFace.HuggingFaceProBundle
import com.intellij.python.huggingFace.cacheManager.service.HfCacheEntryData
import com.intellij.python.huggingFace.cacheManager.service.HfCacheEnvUtils
import com.intellij.python.huggingFace.cacheManager.ui.core.HfCacheMaterialTable
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.util.ui.ListTableModel
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.border.CompoundBorder

class HfCacheToolbarFactory(
  private val project: Project,
  private val getSelectedItems: () -> List<HfCacheEntryData>,
  private val updateData: () -> Unit,
) {
  // todo: proper actions
  fun createToolbar(): ActionToolbar {
    // https://www.figma.com/design/NpIZPvqFVhctYmd8UB93iM/Hugging-Face?node-id=799-18605&t=MGvDgSYsigZ0QqSX-0
    // According to the concept:
    // todo: general/add -> add model
    // general/remove -> delete model
    // separator()
    // todo: general/paste -> copy model's code to clipboard (take from model choice)
    // nodes/folder -> open in finder
    // separator()
    // general/refresh
    val reloadAction = createReloadAction()
    val revealInFinderAction = createRevealInFinderAction()
    val clearCacheAction = createDeleteFromCacheAction()

    val actionGroup = DefaultActionGroup(reloadAction, revealInFinderAction, clearCacheAction)
    val toolbar = ActionManager.getInstance().createActionToolbar("HfToolwindow", actionGroup, false)
    return toolbar.apply {
      component.border = CompoundBorder(IdeBorderFactory.createBorder(SideBorder.RIGHT), component.border)
    }
  }

  private fun createReloadAction() = DumbAwareAction.create(
    HuggingFaceProBundle.message("action.title.refresh"),
    AllIcons.General.Refresh
  ) {
    updateData()
    HuggingFaceCardsUsageCollector.HF_CACHE_MANAGEMENT_TOOLWINDOW_UPDATE.log()
  }

  private fun createRevealInFinderAction(): DumbAwareAction {
    return object : DumbAwareAction(
      HuggingFaceProBundle.message("action.title.reveal.in.finder"),
      null,
      AllIcons.Nodes.Folder
    ) {
      override fun actionPerformed(e: AnActionEvent) {
        getSelectedItems().firstOrNull()?.let { RevealFileAction.openDirectory(File(it.path)) }
        HuggingFaceCardsUsageCollector.HF_CACHE_REVEAL_ITEM_IN_FILE_BROWSER.log()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getSelectedItems().isNotEmpty()
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }
  }

  private fun createDeleteFromCacheAction(): DumbAwareAction {
    return object : DumbAwareAction(
      HuggingFaceProBundle.message("action.title.remove.from.cache"),
      null,
      AllIcons.General.Delete
    ) {
      override fun actionPerformed(e: AnActionEvent) {
        val item = getSelectedItems().firstOrNull() ?: return
        val confirmed: Boolean = yesNo(
          HuggingFaceProBundle.message("action.remove.from.cache.alert.message", item.repoType.printName, item.repoId),
          HuggingFaceProBundle.message("action.remove.from.cache.alert.undo.warn")
        ).ask(project)
        HuggingFaceCardsUsageCollector.HF_CACHE_MANAGEMENT_DELETE_ITEM.log(
          HuggingFaceCardsUsageCollector.CacheManagementActionSource.TOOLBAR,
          HuggingFaceCardsUsageCollector.boolToDialogStatus(confirmed),
          HuggingFaceCardsUsageCollector.closestPowerOfTwo(item.sizeOnDisk)
        )

        if (confirmed) HfCacheEnvUtils.doDeleteItemAndUpdateTable(item) { updateData() }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getSelectedItems().isNotEmpty()
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }
  }
}

private interface HfPopupTableAction {
  fun extractItemFromContext(context: DataContext): HfCacheEntryData? {
    val panel = context.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? HfCacheMaterialTable ?: return null
    val selectedRow = panel.selectedRow
    if (selectedRow == -1) return null
    val tableModel = panel.model as? ListTableModel<*> ?: return null
    val item = tableModel.getRowValue(selectedRow) as? HfCacheEntryData ?: return null
    return item
  }
}

class HfRemoveFromCacheAction : DumbAwareAction(), HfPopupTableAction {
  // todo: merge with com.intellij.python.huggingFace.cacheManager.ui.HfCacheToolbarFactory.createDeleteFromCacheAction
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val context = e.dataContext
    val item = extractItemFromContext(context) ?: return
    val table = e.dataContext.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? HfCacheMaterialTable ?: return
    val confirmed: Boolean = yesNo(
      HuggingFaceProBundle.message("action.remove.from.cache.alert.message", item.repoType.printName, item.repoId),
      HuggingFaceProBundle.message("action.remove.from.cache.alert.undo.warn")
    ).ask(project)

    HuggingFaceCardsUsageCollector.HF_CACHE_MANAGEMENT_DELETE_ITEM.log(
      HuggingFaceCardsUsageCollector.CacheManagementActionSource.CONTEXT_MENU,
      HuggingFaceCardsUsageCollector.boolToDialogStatus(confirmed),
      HuggingFaceCardsUsageCollector.closestPowerOfTwo(item.sizeOnDisk)
    )

    if (confirmed) HfCacheEnvUtils.doDeleteItemAndUpdateTable(item) { table.updateTableData() }
  }
}

class HfCopyPathToClipboard : DumbAwareAction(), HfPopupTableAction {
  override fun actionPerformed(e: AnActionEvent) {
    val context = e.dataContext
    val selectedItem = extractItemFromContext(context) ?: return
    val clipboard = CopyPasteManager.getInstance()
    val stringSelection = StringSelection(selectedItem.path)
    clipboard.setContents(stringSelection)
    HuggingFaceCardsUsageCollector.HF_CACHE_ITEM_PATH_COPIED.log()
  }
}