package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInterpreterSettingsListener
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInterpreterSettingsManager
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryAuth
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryProxy
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryProxyProtocol
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.credentialStore.OneTimeString
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.sql.indexOf
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.jetbrains.bigdatatools.common.ui.addSearchExtension
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import com.jetbrains.bigdatatools.common.util.invokeLater
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultListSelectionModel
import javax.swing.JComponent
import javax.swing.JPanel

open class RepositorySettingsDialog(val manager: ZeppelinInterpreterSettingsManager) : BaseSettingsDialog, ZeppelinInterpreterSettingsListener, Disposable {

  companion object {
    private val REPOSITORY_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9-_]+\$")
    private const val LIST_SPLITTER_PROPORTION_KEY = "zeppelin.notebook.repository.settings.splitter.proportion"
  }

  private val repositoryId = JBTextField(15)
  private val repositoryUrl = JBTextField(15)

  private val repositoryLogin = JBTextField(15)
  private val repositoryPassword = JBPasswordField()

  private val httpProxy = JBRadioButton(ZepMessagesBundle.message("repository.settings.proxy.http"))
  private val httpsProxy = JBRadioButton(ZepMessagesBundle.message("repository.settings.proxy.https"))
  private val proxyHost = JBTextField(15)
  private val proxyPort = IntegerField()
  private val proxyLogin = JBTextField(15)
  private val proxyPassword = JBPasswordField()
  private var selectedRepository: Repository? = null

  private val emptyPanel = JBPanelWithEmptyText().withEmptyText(ZepMessagesBundle.message("repository.settings.empty.text"))

  private val listModel = FilteredListModel<Repository> { it.id }

  private val list = JBList(listModel).apply {
    cellRenderer = CustomListCellRenderer<Repository> { "   ${it.id}" }
    autoscrolls = true
    selectionMode = DefaultListSelectionModel.SINGLE_SELECTION
  }

  private val dataPanel = panel {
    row(ZepMessagesBundle.message("repository.settings.field.id")) { cell(repositoryId).align(Align.FILL).resizableColumn() }
    row(ZepMessagesBundle.message("repository.settings.field.url")) { cell(repositoryUrl).align(Align.FILL).resizableColumn() }
    row(ZepMessagesBundle.message("repository.settings.field.login")) { cell(repositoryLogin).align(Align.FILL).resizableColumn() }
    row(ZepMessagesBundle.message("repository.settings.field.password")) { cell(repositoryPassword).align(Align.FILL).resizableColumn() }

    @Suppress("DialogTitleCapitalization") // There was (optional) suffix which we do not want to capitalize.
    collapsibleGroup(ZepMessagesBundle.message("repository.settings.proxyTitle"), false) {
      buttonsGroup { row { cell(httpProxy); cell(httpsProxy) } }
      row(ZepMessagesBundle.message("repository.settings.field.proxy.host")) { cell(proxyHost).align(Align.FILL).resizableColumn() }
      row(ZepMessagesBundle.message("repository.settings.field.proxy.port")) { cell(proxyPort) }
      row(ZepMessagesBundle.message("repository.settings.field.proxy.login")) { cell(proxyLogin).align(Align.FILL).resizableColumn() }
      row(ZepMessagesBundle.message("repository.settings.field.proxy.password")) { cell(proxyPassword).align(Align.FILL).resizableColumn() }
    }
  }

  private val detailsPanel = JPanel(BorderLayout()).apply {
    border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
    add(emptyPanel, BorderLayout.CENTER)
  }

  val component = OnePixelSplitter(false, LIST_SPLITTER_PROPORTION_KEY, 0.3f).apply {
    dividerPositionStrategy = Splitter.DividerPositionStrategy.KEEP_FIRST_SIZE
    secondComponent = detailsPanel
    firstComponent = createList(manager.getRepos().map { it.copy() })
  }

  private fun createList(elements: List<Repository>): JComponent {
    listModel.data = elements

    val scrollPane = ScrollPaneFactory.createScrollPane(list, true)

    list.addListSelectionListener { e ->
      if (e.valueIsAdjusting) return@addListSelectionListener

      // Updating current record
      val localSelectedRepository = selectedRepository
      if (localSelectedRepository != null) {
        val indexOfFound = listModel.data.indexOf { it.id == localSelectedRepository.id }
        if (indexOfFound != -1) {
          listModel.replaceElement(getDetails(), indexOfFound)
        }
      }
      selectedRepository = list.selectedValue

      showDetails(list.selectedValue)
    }

    if (listModel.size != 0) {
      list.selectedIndex = 0
    }

    val horizontalPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      add(ToolbarUtils.createActionToolbar(this, "BDTZeppelinRepositorySettings", createActions(), true).component)
      add(createFilterText())
    }

    return JPanel(BorderLayout()).apply {
      add(horizontalPanel, BorderLayout.NORTH)
      add(scrollPane, BorderLayout.CENTER)
      minimumSize = Dimension(JBUIScale.scale(150), minimumSize.height)
    }
  }

  init {
    manager.addListener(this)
    httpsProxy.isSelected = true
    proxyPort.withPortValidator(this)
    repositoryUrl.withNotEmptyValidator(this, "repository.settings.validation.url.empty")
    setIdValidator()
  }

  private fun setIdValidator() {
    ComponentValidator(this)
      .withValidator(Supplier {
        when {
          repositoryId.text.isBlank() -> {
            ValidationInfo(ZepMessagesBundle.message("repository.settings.validation.id.empty"), repositoryId)
          }
          listModel.data.find { it != list.selectedValue && it.id == repositoryId.text } != null -> {
            ValidationInfo(ZepMessagesBundle.message("repository.settings.validation.id.existing", repositoryId.text), repositoryId)
          }
          !REPOSITORY_NAME_PATTERN.matcher(repositoryId.text).find() -> {
            ValidationInfo(ZepMessagesBundle.message("repository.settings.validation.id.invalid"), repositoryId)
          }
          else -> {
            null
          }
        }
      })
      .andRegisterOnDocumentListener(repositoryId)
      .installOn(repositoryId)
  }

  override fun dispose() {
    manager.removeListener(this)
  }

  override fun updateRepositories(repositories: List<Repository>, requestException: Throwable?) {
    invokeLater {
      val selectedRepository = list.selectedValue

      listModel.data = repositories.map { it.copy() }

      if (selectedRepository != null) {
        val found = listModel.filteredData.find { it.id == selectedRepository.id }
        found?.let { list.setSelectedValue(found, true) }
      }

      if (list.selectedIndex == -1 && !list.isEmpty) {
        list.selectedIndex = 0
      }
    }
  }

  private fun getDetails(): Repository {
    val authentication = if (repositoryLogin.text.isNullOrBlank() && repositoryPassword.password.isEmpty()) {
      RepositoryAuth()
    }
    else {
      RepositoryAuth(username = repositoryLogin.text, password = OneTimeString(repositoryPassword.password))
    }

    val proxyAuth = if (proxyLogin.text.isNullOrBlank() && proxyPassword.password.isEmpty()) {
      RepositoryAuth()
    }
    else {
      RepositoryAuth(username = proxyLogin.text, password = OneTimeString(proxyPassword.password))
    }

    val proxy = if (proxyHost.text.isNullOrBlank()) null
    else RepositoryProxy(type = if (httpProxy.isSelected) RepositoryProxyProtocol.HTTP else RepositoryProxyProtocol.HTTPS,
                         host = proxyHost.text,
                         port = if (proxyPort.text.isNullOrBlank()) -1 else proxyPort.text.toIntOrNull() ?: -1,
                         auth = proxyAuth)

    return Repository(id = repositoryId.text,
                      url = repositoryUrl.text,
                      authentication = authentication,
                      proxy = proxy)
  }

  override fun isModified(): Boolean {
    return FilteredListModel<Repository> { it.id }.apply { data = manager.getRepos().map { it.copy() } }.data != listModel.data
  }

  override fun apply(): Boolean {
    executeOnPooledThread {
      var hasChanges = false
      listModel.data.forEach { repository ->
        try {
          val old = manager.getRepos().find { it.id == repository.id }
          if (old == repository) {
            return@forEach
          }
          if (old == null) {
            manager.addRepository(repository)
            hasChanges = true
          }
          else {
            // We do not have update here.
            manager.removeRepository(repository)
            manager.addRepository(repository)
            hasChanges = true
          }
        }
        catch (e: Exception) {
          BaseSettingsDialog.showErrorMessage(ZepMessagesBundle.message("interpreter.settings.error.addOrUpdate", repository.id), e)
        }
      }

      // Processing deleted.
      manager.getRepos().filter { old -> listModel.data.find { it.id == old.id } == null }.forEach {
        manager.removeRepository(it)
        hasChanges = true
      }

      if (hasChanges) {
        manager.refreshAsync()
      }
    }
    return true
  }

  private fun getSuitableNewRepositoryName(): String {
    var name = "Unnamed"
    for (i in 1..100) {
      if (listModel.data.find { it.id == name } == null) {
        return name
      }

      name = "Unnamed_${i}"
    }

    return "Unnamed"
  }

  private fun createActions(): List<AnAction> {
    val refreshAction = DumbAwareAction.create(ZepMessagesBundle.message("repository.settings.action.refresh"), AllIcons.Actions.Refresh) {
      executeOnPooledThread {
        try {
          manager.refreshAsync()
        }
        catch (e: Exception) {
          BaseSettingsDialog.showErrorMessage(ZepMessagesBundle.message("repository.settings.error.refresh"), e)
        }
      }
    }

    val addAction = DumbAwareAction.create(ZepMessagesBundle.message("repository.settings.action.add"), AllIcons.General.Add) {
      val repository = Repository(id = getSuitableNewRepositoryName(), url = "")
      listModel.data += repository
      list.setSelectedValue(repository, true)

      if (!repositoryId.text.isNullOrBlank()) {
        repositoryId.select(0, repositoryId.text.length)
      }
      repositoryId.requestFocus()
    }

    val removeAction = DumbAwareAction.create(ZepMessagesBundle.message("repository.settings.action.remove"), AllIcons.General.Remove) {
      val contextComponent = it.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JComponent ?: component

      val selectedRepository = list.selectedValue

      if (selectedRepository == null) {
        Messages.showInfoMessage(ZepMessagesBundle.message("repository.settings.remove.nothing.message"),
                                 ZepMessagesBundle.message("repository.settings.remove.title"))
      }
      else {
        val res = MessageDialogBuilder.yesNo(ZepMessagesBundle.message("repository.settings.remove.title"),
                                             ZepMessagesBundle.message("repository.settings.remove.message", selectedRepository.id))
          .ask(contextComponent)

        if (res) {
          listModel.data -= selectedRepository
        }
      }
    }

    return listOf(refreshAction, addAction, removeAction)
  }

  private fun createFilterText(): JBTextField {
    return ExtendableTextField(15).apply {
      addSearchExtension()
      doOnChange {
        listModel.filter = { repository -> if (text.isBlank()) true else repository.id.lowercase().contains(text.lowercase()) }
      }
    }
  }

  private fun clearDetails() {
    repositoryId.text = ""
    repositoryUrl.text = ""
    repositoryLogin.text = ""
    repositoryPassword.text = ""
    clearProxy()
  }

  private fun clearProxy() {
    proxyHost.text = ""
    proxyPort.text = ""
    proxyLogin.text = ""
    proxyPassword.text = ""
  }

  private fun showDetails(settings: Repository?) {
    detailsPanel.removeAll()

    if (settings == null) {
      detailsPanel.add(emptyPanel, BorderLayout.CENTER)
      clearDetails()
      detailsPanel.revalidate()
      detailsPanel.repaint()
      return
    }

    detailsPanel.add(dataPanel, BorderLayout.CENTER)

    repositoryId.text = settings.id
    repositoryUrl.text = settings.url

    repositoryLogin.text = settings.authentication?.username ?: ""
    repositoryPassword.text = settings.authentication?.password?.toString() ?: ""

    if (settings.proxy != null) {

      if (settings.proxy.type == RepositoryProxyProtocol.HTTP) {
        httpProxy.isSelected = true
      }
      else {
        httpsProxy.isSelected = true
      }

      proxyHost.text = settings.proxy.host
      proxyPort.text = if (settings.proxy.port == -1) "" else settings.proxy.port.toString()

      proxyLogin.text = settings.proxy.auth?.username ?: ""
      proxyPassword.text = settings.proxy.auth?.password?.toString() ?: ""
    }
    else {
      clearProxy()
    }

    detailsPanel.revalidate()
    detailsPanel.repaint()
  }
}