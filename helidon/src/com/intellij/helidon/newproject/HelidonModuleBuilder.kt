// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.newproject

import com.intellij.helidon.HelidonIcons
import com.intellij.helidon.utils.HelidonBundle
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.starters.local.*
import com.intellij.ide.starters.local.wizard.StarterInitialStep
import com.intellij.ide.starters.shared.*
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.util.Key
import com.intellij.pom.java.LanguageLevel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.lang.JavaVersion
import javax.swing.Icon

internal val NEW_HELIDON_PROJECT_KEY: Key<Boolean> = Key.create("helidon.new.project")

internal class HelidonModuleBuilder : StarterModuleBuilder() {
  override fun getBuilderId(): String = "helidon"
  override fun getNodeIcon(): Icon = HelidonIcons.Helidon
  override fun getPresentableName(): String = HelidonBundle.HELIDON_LIBRARY
  override fun getDescription(): String = HelidonBundle.message("description.for.helidon.project.starter")
  override fun getHelpId(): String = "helidon.project"
  override fun getProjectTypes(): List<StarterProjectType> = listOf(MAVEN_PROJECT, GRADLE_PROJECT)

  override fun getLanguages(): List<StarterLanguage> {
    return listOf(
      JAVA_STARTER_LANGUAGE,
      KOTLIN_STARTER_LANGUAGE
    )
  }

  // Helidon 3 requires at least Java 17
  override fun getMinJavaVersion(): JavaVersion = LanguageLevel.JDK_17.toJavaVersion()

  override fun createWizardSteps(context: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
    return emptyArray()
  }

  override fun createOptionsStep(contextProvider: StarterContextProvider): StarterInitialStep {
    return HelidonStarterInitialStep(contextProvider)
  }

  override fun setupModule(module: Module) {
    // manually set, we do not show the second page with libraries
    starterContext.starter = starterContext.starterPack.starters.first()
    starterContext.starterDependencyConfig = loadDependencyConfig()[starterContext.starter?.id]

    if (starterContext.isCreatingNewProject) {
      module.project.putUserData(NEW_HELIDON_PROJECT_KEY, true)
    }

    super.setupModule(module)
  }

  override fun getStarterPack(): StarterPack {
    return StarterPack("helidon", listOf(
      Starter("helidon", "Helidon", getDependencyConfig("/starters/helidon.pom"), listOf())
    ))
  }

  override fun getAssets(starter: Starter): List<GeneratorAsset> {
    val ftManager = FileTemplateManager.getInstance(ProjectManager.getInstance().defaultProject)
    val standardAssetsProvider = StandardAssetsProvider()
    val assets = mutableListOf<GeneratorAsset>()

    if (starterContext.projectType == GRADLE_PROJECT) {
      assets.add(GeneratorTemplateFile("build.gradle", ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_BUILD_GRADLE)))
      assets.add(GeneratorTemplateFile("settings.gradle", ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_SETTINGS_GRADLE)))
      assets.add(GeneratorTemplateFile(standardAssetsProvider.gradleWrapperPropertiesLocation,
                                       ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_GRADLEW_PROPERTIES)))
      assets.addAll(standardAssetsProvider.getGradlewAssets())
      if (starterContext.isCreatingNewProject) {
        assets.addAll(standardAssetsProvider.getGradleIgnoreAssets())
      }
    }
    else if (starterContext.projectType == MAVEN_PROJECT) {
      assets.add(GeneratorTemplateFile("pom.xml", ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_POM_XML)))

      assets.add(GeneratorTemplateFile(standardAssetsProvider.mavenWrapperPropertiesLocation,
                                       ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_MVNW_PROPERTIES)))
      assets.addAll(standardAssetsProvider.getMvnwAssets())
      if (starterContext.isCreatingNewProject) {
        assets.addAll(standardAssetsProvider.getMavenIgnoreAssets())
      }
    }

    assets.add(GeneratorTemplateFile("src/main/resources/META-INF/beans.xml",
                                     ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_BEANS_XML)))
    assets.add(GeneratorTemplateFile("src/main/resources/META-INF/microprofile-config.properties",
                                     ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_MICROPROFILE_CONFIG)))
    assets.add(GeneratorTemplateFile("src/main/resources/application.yaml",
                                     ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_APPLICATION_YAML)))
    assets.add(GeneratorTemplateFile("src/main/resources/logging.properties",
                                     ftManager.getJ2eeTemplate(HelidonFileTemplateGroup.HELIDON_LOGGING_PROPERTIES)))

    val packagePath = getPackagePath(starterContext.group, starterContext.artifact)
    val samplesLanguage = starterContext.language.id
    val samplesExt = getSamplesExt(starterContext.language)

    assets.add(GeneratorTemplateFile("src/main/${samplesLanguage}/${packagePath}/HelloResource.${samplesExt}",
                                     ftManager.getJ2eeTemplate("helidon-HelloResource-${samplesLanguage}.${samplesExt}")))

    return assets
  }

  override fun getFilePathsToOpen(): List<String> {
    val files = mutableListOf<String>()
    if (starterContext.projectType == MAVEN_PROJECT) {
      files.add("pom.xml")
    }
    else if (starterContext.projectType == GRADLE_PROJECT) {
      files.add("build.gradle")
    }

    val packagePath = getPackagePath(starterContext.group, starterContext.artifact)
    val samplesLanguage = starterContext.language.id
    val samplesExt = getSamplesExt(starterContext.language)

    files.add("src/main/${samplesLanguage}/${packagePath}/HelloResource.${samplesExt}")

    return files
  }

  private class HelidonStarterInitialStep(contextProvider: StarterContextProvider) : StarterInitialStep(contextProvider) {
    override fun addFieldsAfter(layout: Panel) {
      layout.row {
        hyperLink(HelidonBundle.message("helidon.mp.overview"), "https://helidon.io/docs/v3/#/mp/introduction")
      }
    }
  }
}