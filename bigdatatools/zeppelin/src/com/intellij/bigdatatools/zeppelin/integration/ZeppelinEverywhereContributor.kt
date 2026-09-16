package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.ide.util.gotoByName.ChooseByNameItemProvider
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNameViewModel
import com.intellij.ide.util.gotoByName.DefaultChooseByNameItemProvider
import com.intellij.ide.util.gotoByName.DisposableGotoModelWithPersistentFilter
import com.intellij.ide.util.gotoByName.FilteringGotoByModel
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import javax.swing.JLabel
import javax.swing.ListCellRenderer

class ZeppelinEverywhereContributor(private val project: Project) : SearchEverywhereContributor<NotebookNavigationItem> {
  companion object {
    const val CONTRIBUTOR_ID = "Bdi.Zeppelin.Notebooks"
  }

  override fun getSearchProviderId(): String = CONTRIBUTOR_ID

  override fun getGroupName(): String = ZepMessagesBundle.message("zeppelin.search.tab.name")

  override fun getSortWeight(): Int = 0

  override fun showInFindResults(): Boolean = true

  override fun fetchElements(pattern: String, progressIndicator: ProgressIndicator, consumer: Processor<in NotebookNavigationItem>) {
    if (!isEmptyPatternSupported && pattern.isEmpty()) return

    val fetchRunnable = Runnable {
      val model: FilteringGotoByModel<*> = NotebookGotoModel(project)

      if (!progressIndicator.isCanceled) {
        val popup = ChooseByNamePopup.createPopup(project, model, null)
        try {
          val provider = popup.provider

          provider.filterElements(popup as ChooseByNameViewModel, pattern, true, progressIndicator) { element: Any? ->
            (element as? NotebookNavigationItem)?.let { consumer.process(it) } ?: false
          }
        }
        finally {
          Disposer.dispose(popup)
        }
      }
    }


    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode && application.isDispatchThread) {
      fetchRunnable.run()
    }
    else {
      ProgressIndicatorUtils.yieldToPendingWriteActions()
      ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(fetchRunnable, progressIndicator)
    }
  }

  override fun processSelectedItem(selected: NotebookNavigationItem, modifiers: Int, searchText: String): Boolean {
    selected.navigate(requestFocus = true)
    return true
  }

  override fun getElementsRenderer(): ListCellRenderer<in NotebookNavigationItem> = NotebookNavigationItemCellRenderer()

  override fun isShownInSeparateTab(): Boolean = true

  private class NotebookNavigationItemCellRenderer : PsiElementListCellRenderer<NotebookNavigationItem>() {
    override fun getElementText(element: NotebookNavigationItem?): String = element?.presentation?.presentableText ?: ""

    override fun getContainerText(element: NotebookNavigationItem?, name: String?): String? = element?.presentation?.locationString

    override fun getIconFlags(): Int = 0
  }
}

class NotebookGotoModel(project: Project) : DisposableGotoModelWithPersistentFilter<NotebookNavigationItem>(
  project, listOf(ZeppelinNotebookNameContributor())
) {
  override fun getPromptText(): String = ZepMessagesBundle.message("zeppelin.search.tab.prompt")
  override fun getNotInMessage(): String = ""
  override fun getNotFoundMessage(): String = ZepMessagesBundle.message("zeppelin.search.tab.not.found")
  override fun getCheckBoxName(): String? = null
  override fun loadInitialCheckBoxState(): Boolean = false
  override fun saveInitialCheckBoxState(state: Boolean) { }

  override fun getSeparators(): Array<String> = arrayOf("/", "@", "#/notebook", "http", "https")

  override fun getFullName(element: Any): String? = (element as? NotebookNavigationItem)?.getFullNoteName() // presentation?.locationString

  override fun willOpenEditor(): Boolean = true

  override fun filterValueFor(item: NavigationItem?): NotebookNavigationItem? = null

  override fun getItemProvider(context: PsiElement?): ChooseByNameItemProvider = IncludeEverythingByNameProvider(context)

  private class IncludeEverythingByNameProvider(context: PsiElement?) : DefaultChooseByNameItemProvider(context) {

    override fun filterElementsWithWeights(base: ChooseByNameViewModel,
                                           parameters: FindSymbolParameters,
                                           indicator: ProgressIndicator,
                                           consumer: Processor<in FoundItemDescriptor<*>>): Boolean {
      return super.filterElementsWithWeights(base,  parameters.withScope(IncludeEverythingScope()), indicator, consumer)
    }
  }

  private class IncludeEverythingScope : GlobalSearchScope() {
    override fun contains(file: VirtualFile): Boolean = true
    override fun isSearchInModuleContent(aModule: Module): Boolean = true
    override fun isSearchInLibraries(): Boolean = false
  }
}

class ZeppelinDummyContributor  : SearchEverywhereContributor<NotebookNavigationItem> {
  override fun getSearchProviderId(): String = "Bdi.Zeppelin.Dummy"
  override fun getGroupName(): String = ""
  override fun getSortWeight(): Int = 0
  override fun showInFindResults(): Boolean = false
  override fun fetchElements(pattern: String, progressIndicator: ProgressIndicator, consumer: Processor<in NotebookNavigationItem>) { }
  override fun processSelectedItem(selected: NotebookNavigationItem, modifiers: Int, searchText: String): Boolean = false
  override fun getElementsRenderer(): ListCellRenderer<in NotebookNavigationItem> = ListCellRenderer { _, _, _, _, _ -> JLabel("") }
}

class ZeppelinEverywhereContributorFactory : SearchEverywhereContributorFactory<NotebookNavigationItem> {
  override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<NotebookNavigationItem> =
    if (DriverManager.getDrivers(initEvent.project).any { it is ZeppelinDriver }) ZeppelinEverywhereContributor(initEvent.project!!)
    else ZeppelinDummyContributor()
}