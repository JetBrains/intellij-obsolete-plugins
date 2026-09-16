package com.jetbrains.bigdatatools.wizard

import com.intellij.bigdatatools.coreUi.ui.comboBoxWithAutoUpdate
import com.intellij.bigdatatools.coreUi.util.messageOrDefault
import com.intellij.icons.AllIcons
import com.intellij.ide.JavaUiBundle
import com.intellij.ide.projectWizard.NewProjectWizardCollector.Maven.logArtifactIdChanged
import com.intellij.ide.projectWizard.NewProjectWizardCollector.Maven.logGroupIdChanged
import com.intellij.ide.starters.JavaStartersBundle
import com.intellij.ide.starters.shared.DEFAULT_MODULE_GROUP
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.util.equalsTo
import com.intellij.openapi.observable.util.trim
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.CHECK_ARTIFACT_ID
import com.intellij.openapi.ui.validation.CHECK_GROUP_ID
import com.intellij.openapi.ui.validation.CHECK_NON_EMPTY
import com.intellij.openapi.ui.validation.WHEN_PROPERTY_CHANGED
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.trimmedTextValidation
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.editableValueMatches
import com.intellij.util.lang.JavaVersion
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import org.jetbrains.annotations.Nls
import java.io.File

private const val BDIDE_PROJECT_WIZARD_NOTIFICATION_GROUP = "BDIDE_PROJECT_WIZARD_NOTIFICATION_GROUP"

class SparkProjectWizardStep(val parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
  private val groupIdProperty = propertyGraph.lazyProperty { DEFAULT_MODULE_GROUP }
  private val artifactIdProperty = propertyGraph.lazyProperty(::suggestArtifactIdByName)
  private val projectTypeProperty = propertyGraph.lazyProperty { SparkProjectType.entries.first() }
  private val languageTypeProperty = propertyGraph.lazyProperty { SparkProjectLanguageHandler.getAll().first() }
  val buildSystemProperty: GraphProperty<BuildSystemBasedStructureProviderFactory> = propertyGraph.lazyProperty {
    BuildSystemBasedStructureProviderFactory.getAll().first { languageTypeProperty.get().language in it.supportedLanguageType }
  }

  private var moduleJdk: Sdk? = null
  private var buildSystemComboBox: ComboBox<BuildSystemBasedStructureProviderFactory>? = null

  init {
    artifactIdProperty.dependsOn((parent as NewProjectWizardBaseStep).nameProperty, ::suggestArtifactIdByName)
  }


  private val errorHandler = { e: Exception ->
    Notifications.Bus.notify(
      Notification(
        BDIDE_PROJECT_WIZARD_NOTIFICATION_GROUP,
        WizardMessageBundle.message("bdide.project.wizard.error.title"),
        e.messageOrDefault(),
        NotificationType.ERROR
      )
    )
  }

  private val wizardPanels = SparkProjectLanguageHandler.getAll().associateWith {
    it.createWizardPanel()
  }

  override fun setupUI(builder: Panel) {
    val project = context.project
    val model = ProjectSdksModel()
    model.reset(project)

    lateinit var jdkComboBox: JdkComboBox

    jdkComboBox = JdkComboBox(
      project, model, Condition { it is JavaSdk }, null, Condition { it is JavaSdk }
    ) {
      if (it is JavaSdk && jdkComboBox.selectedJdk == null)
        jdkComboBox.selectedJdk = it
    }.apply {
      this.addActionListener {
        moduleJdk = this.selectedJdk
        context.projectJdk = this.selectedJdk
      }
    }
    if (jdkComboBox.selectedJdk == null && jdkComboBox.itemCount > 0)
      jdkComboBox.selectedJdk = jdkComboBox.model.getElementAt(0)?.jdk

    builder.apply {
      row(WizardMessageBundle.message("bdt.wizard.type.label")) {
        segmentedButton(SparkProjectType.entries) { this.text = it.title }.bind(projectTypeProperty)
      }
      row(WizardMessageBundle.message("bdt.wizard.lang.label")) {
        segmentedButton(SparkProjectLanguageHandler.getAll()) {
          this.text = it.presentableName
          this.icon = it.icon
        }.bind(languageTypeProperty)
      }
      row(JavaStartersBundle.message("title.project.build.system.label")) {
        comboBoxWithAutoUpdate(languageTypeProperty,
                               ItemWithIconCellRenderer()) {
          languageTypeProperty.get().createBuildSystemFactories()
        }
          .bindItem(buildSystemProperty)
          .resizableColumn().also {
            buildSystemComboBox = it.component
            buildSystemComboBox?.selectedIndex = 0
          }
      }

      row(JavaUiBundle.message("label.project.wizard.new.project.jdk")) {
        cell(jdkComboBox)
      }
      row {
        val component = SimpleColoredComponent()
        component.append(WizardMessageBundle.message("notification.spark.with.java.15.problems"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
        component.icon = AllIcons.General.Warning
        cell(component)
      }.visibleIf(jdkComboBox.editableValueMatches {
        val jdkComboBoxItem = (it as? JdkComboBox.JdkComboBoxItem)?.jdk ?: jdkComboBox.selectedJdk
        val versionString = jdkComboBoxItem?.versionString ?: return@editableValueMatches false
        val featureVersion = JavaVersion.tryParse(versionString)?.feature ?: return@editableValueMatches false
        featureVersion >= 15
      })

      wizardPanels.forEach {
        it.value?.initComponent(this, this@SparkProjectWizardStep)?.visibleIf(languageTypeProperty.equalsTo(it.key))
      }

      collapsibleGroup(WizardMessageBundle.message("border.title.advanced.settings")) {
        row {
          layout(RowLayout.LABEL_ALIGNED)
          label(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.group.id.label"))
            .applyToComponent { horizontalTextPosition = JBLabel.LEFT }
            .applyToComponent { icon = AllIcons.General.ContextHelp }
            .applyToComponent { toolTipText = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.group.id.help") }
          textField()
            .bindText(groupIdProperty.trim())
            .columns(COLUMNS_MEDIUM)
            .trimmedTextValidation(CHECK_NON_EMPTY, CHECK_GROUP_ID)
            .validationInfo { validateGroupId(groupIdProperty.get()) }
            .whenTextChangedFromUi { logGroupIdChanged() }
        }

        row {
          layout(RowLayout.LABEL_ALIGNED)
          label(ExternalSystemBundle.message("external.system.mavenized.structure.wizard.artifact.id.label"))
            .applyToComponent { horizontalTextPosition = JBLabel.LEFT }
            .applyToComponent { icon = AllIcons.General.ContextHelp }
            .applyToComponent {
              toolTipText = ExternalSystemBundle.message("external.system.mavenized.structure.wizard.artifact.id.help",
                                                         context.presentationName)
            }
          textField()
            .bindText(artifactIdProperty.trim())
            .columns(COLUMNS_MEDIUM)
            .trimmedTextValidation(CHECK_NON_EMPTY, CHECK_ARTIFACT_ID)
            .validationRequestor(WHEN_PROPERTY_CHANGED(artifactIdProperty))
            .whenTextChangedFromUi { logArtifactIdChanged() }
        }
      }.topGap(TopGap.MEDIUM)
    }
  }

  override fun setupProject(project: Project) {
    val provider = buildSystemProperty.get().createProvider(File(context.projectFileDirectory), errorHandler)
    val cePath = context.projectDirectory.toAbsolutePath().toCanonicalPath()

    runProviderUnderProgress(
      provider,
      WizardMessageBundle.message("bdide.project.wizard.importing.message"),
      project
    ) { pr, indicator ->
      val additionalParams = wizardPanels.filter { languageTypeProperty.get() == it.key }.values.flatMap {
        it?.getValues() ?: emptyList()
      }.toMap()

      provider.copyProjectTemplate(indicator, additionalParams,
                                   groupId = groupIdProperty.get(),
                                   artifactName = artifactIdProperty.get(),
                                   projectType = projectTypeProperty.get(),
                                   languageHandler = languageTypeProperty.get())

      pr.linkProject(project, cePath, indicator)
    }
  }

  private fun validationError(@NlsContexts.DialogMessage message: String) = ValidationInfo(message, null)

  private fun validateGroupId(text: String?): ValidationInfo? {
    return if (text.isNullOrEmpty())
      validationError(WizardMessageBundle.message("dialog.message.missing.group.id"))
    else
      null
  }

  private fun runProviderUnderProgress(selectedProvider: BuildSystemBasedStructureProvider,
                                       @Nls title: String,
                                       project: Project,
                                       action: (BuildSystemBasedStructureProvider, indicator: ProgressIndicator) -> Unit) {
    ProgressManager.getInstance().run(object : Task.Modal(project, title, false) {
      override fun run(indicator: ProgressIndicator) {
        action.invoke(selectedProvider, indicator)
      }
    })
  }

  private fun suggestArtifactIdByName() = (parent as NewProjectWizardBaseStep).name
}
