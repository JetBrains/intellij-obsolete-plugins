/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.feature.forge

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.ide.starters.local.StarterModuleBuilder
import com.intellij.ide.starters.remote.SERVER_APPLICATION_TYPES
import com.intellij.ide.starters.remote.SERVER_LANGUAGE_LEVELS_KEY
import com.intellij.ide.starters.remote.SERVER_LANGUAGE_LEVEL_KEY
import com.intellij.ide.starters.remote.WebStarterContext
import com.intellij.ide.starters.remote.WebStarterContextProvider
import com.intellij.ide.starters.remote.WebStarterDependency
import com.intellij.ide.starters.remote.WebStarterDependencyCategory
import com.intellij.ide.starters.remote.WebStarterFrameworkVersion
import com.intellij.ide.starters.remote.WebStarterModuleBuilder
import com.intellij.ide.starters.remote.WebStarterServerOptions
import com.intellij.ide.starters.remote.unzipSubfolder
import com.intellij.ide.starters.remote.wizard.WebStarterInitialStep
import com.intellij.ide.starters.shared.LibraryLink
import com.intellij.ide.starters.shared.LibraryLinkType
import com.intellij.ide.starters.shared.StarterAppType
import com.intellij.ide.starters.shared.StarterLanguage
import com.intellij.ide.starters.shared.StarterLanguageLevel
import com.intellij.ide.starters.shared.StarterProjectType
import com.intellij.ide.starters.shared.StarterTestRunner
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.Url
import com.intellij.util.Urls
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.GroovyMvcIcons
import java.io.File
import java.io.IOException
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.swing.Icon

internal class GrailsForgeModuleBuilder : WebStarterModuleBuilder() {
  private val SERVLET_KEY: Key<ServletType> = Key.create("grails.servlet.type")
  private val GORM_KEY: Key<GormType> = Key.create("grails.gorm.type")

  private val INVALID_ARTIFACT_SYMBOL_PATTERN: Regex = Regex("[^a-zA-Z0-9_.-]")

  override fun getDefaultServerUrl(): String = "https://latest.grails.org"

  override fun getBuilderId(): String = "grails-6"
  override fun getNodeIcon(): Icon = GroovyMvcIcons.Grails
  @Suppress("HardCodedStringLiteral")
  override fun getPresentableName(): String = "Grails Application Forge"
  @Suppress("HardCodedStringLiteral")
  override fun getDescription(): String = "Grails"
  override fun isShowProjectTypes(): Boolean = false
  override fun isReformatAfterCreation(project: Project): Boolean = false

  override fun getProjectTypes(): List<StarterProjectType> {
    return listOf(StarterProjectType("GRADLE", "Gradle"))
  }

  override fun getLanguages(): List<StarterLanguage> {
    return listOf(StarterLanguage("GROOVY", "Groovy", "Groovy"))
  }

  override fun getTestFrameworks(): List<StarterTestRunner> {
    return listOf(
      StarterTestRunner("SPOCK", "Spock"),
      StarterTestRunner("JUNIT", "JUnit")
    )
  }

  override fun getApplicationTypes(): List<StarterAppType> {
    return listOf(
      StarterAppType("WEB", "Web Application"),
      StarterAppType("REST-API", "Rest API"),
      StarterAppType("WEB_PLUGIN", "Web Plugin"),
      StarterAppType("PLUGIN", "Plugin")
    )
  }

  override fun getDefaultLanguageLevel(): StarterLanguageLevel = StarterLanguageLevel("JDK_11", "11", "11")

  override fun getLanguageLevels(): List<StarterLanguageLevel> {
    return listOf(
      StarterLanguageLevel("JDK_17", "17", "17"),
      StarterLanguageLevel("JDK_11", "11", "11")
    )
  }

  override fun getFilePathsToOpen(): List<String> = listOf("README.md")

  override fun loadServerOptions(serverUrl: String): WebStarterServerOptions {
    val json = loadJsonData(serverUrl.removeSuffix("/") + "/select-options")

    return handleOptionsJson(serverUrl, json)
  }

  private fun handleOptionsJson(serverUrl: String, optionsJson: JsonNode): WebStarterServerOptions {
    val typesNode = optionsJson.get("type")?.get("options") ?: throw IOException("Unable to read application types")

    val types = typesNode.map {
      val typeObject = it
      val id = typeObject.get("name").asText()
      val title = typeObject.get("title").asText()
      StarterAppType(id, title)
    }

    val descriptionLinkPattern = Pattern.compile(" \\((http[s]?://[^\\s]+)\\)")
    val dependencyCategories = types.flatMap {
      loadFeatures(serverUrl, it.id, descriptionLinkPattern)
    }

    val versionsJson = loadJsonData(serverUrl.removeSuffix("/") + "/versions")
    val version = versionsJson.get("versions")?.get("grails.version")?.asText()
                  ?: throw IOException("Unable to read Grails version")

    val options = WebStarterServerOptions(listOf(WebStarterFrameworkVersion(version, version, true)), dependencyCategories)
    options.putUserData(SERVER_APPLICATION_TYPES, types)

    val jdkConfigObject = optionsJson.get("jdkVersion")
    val jdkNodes = jdkConfigObject?.get("options")
    if (jdkNodes != null) {
      val languageLevels = jdkNodes.map { jdkObject ->
        val id = jdkObject.get("name").asText()
        val title = jdkObject.get("label").asText()
        StarterLanguageLevel(id, title, id.removePrefix("JDK_"))
      }
      options.putUserData(SERVER_LANGUAGE_LEVELS_KEY, languageLevels)

      val defaultOptionName = jdkConfigObject.get("defaultOption")?.get("name")?.asText()
      if (defaultOptionName != null) {
        val defaultLanguageLevel = languageLevels.find { it.id == defaultOptionName }
        options.putUserData(SERVER_LANGUAGE_LEVEL_KEY, defaultLanguageLevel)
      }
    }

    return options
  }

  private fun loadFeatures(serverUrl: String,
                           appTypeId: String,
                           descriptionLinkPattern: Pattern): Collection<WebStarterDependencyCategory> {
    val featuresRoot = loadJsonData(serverUrl.removeSuffix("/") + "/application-types/${appTypeId}/features")
    val categories = mutableMapOf<String, GrailsFeatureCategory>()

    for (featureElement in featuresRoot.get("features")) {
      val categoryName = featureElement.get("category").asText()
      val category = categories.getOrPut(categoryName) {
        GrailsFeatureCategory(appTypeId, categoryName)
      }

      val id = featureElement.get("name").asText()
      val title = featureElement.get("title").asText()
      var description = featureElement.get("description").asText()

      val matcher = descriptionLinkPattern.matcher(description)
      val links: List<LibraryLink> = if (matcher.find()) {
        description = description.substring(0, matcher.start()) + description.substring(matcher.end())
        listOf(LibraryLink(LibraryLinkType.WEBSITE, matcher.group(1)))
      }
      else {
        emptyList()
      }

      category.features.add(WebStarterDependency(id, title, description, links))
    }

    return categories.values
  }

  override fun composeGeneratorUrl(serverUrl: String, starterContext: WebStarterContext): Url {
    return composeEndpointUrl(starterContext, serverUrl)
  }

  private fun composeEndpointUrl(starterContext: WebStarterContext, serverUrl: String): Url {
    val basePackage = suggestBasePackage(starterContext.group, starterContext.artifact)
    var url = Urls.newFromEncoded(serverUrl.removeSuffix("/") + "/create/" + starterContext.applicationType!!.id + "/" + basePackage)

    val servletType = getServletType()
    val gormType = getGormType()

    url = url.addParameters(mapOf(
      "test" to starterContext.testFramework!!.id,
      "javaVersion" to starterContext.languageLevel!!.id,
      "servlet" to servletType.id,
      "gorm" to gormType.id
    ))

    if (starterContext.dependencies.isNotEmpty()) {
      val features = starterContext.dependencies.joinToString(",") { it.id }
      url = url.addParameters(mapOf("features" to features))
    }

    return url
  }

  private fun suggestBasePackage(group: String, artifact: String): String {
    val groupPrefix = group.lowercase().split(".")
      .joinToString(".") { StarterModuleBuilder.sanitizePackage(it) }

    return "$groupPrefix.${sanitizeArtifact(artifact)}"
  }

  private fun sanitizeArtifact(input: String): String {
    val fileName = FileUtil.sanitizeFileName(input, false)
    return fileName
      .replace(INVALID_ARTIFACT_SYMBOL_PATTERN, "_")
      .lowercase()
  }

  override fun extractGeneratorResult(tempZipFile: File, contentEntryDir: File) {
    unzipSubfolder(tempZipFile, contentEntryDir)
  }

  private fun setServletType(servletType: ServletType) {
    starterContext.putUserData(SERVLET_KEY, servletType)
  }

  private fun getServletType(): ServletType {
    return starterContext.getUserData(SERVLET_KEY) ?: ServletType.JETTY
  }

  private fun setGormType(gormType: GormType) {
    starterContext.putUserData(GORM_KEY, gormType)
  }

  private fun getGormType(): GormType {
    return starterContext.getUserData(GORM_KEY) ?: GormType.HIBERNATE
  }

  override fun createOptionsStep(contextProvider: WebStarterContextProvider): WebStarterInitialStep {
    return object : WebStarterInitialStep(contextProvider) {
      private val servletProperty: GraphProperty<ServletType> = propertyGraph.property(ServletType.TOMCAT)
      private val gormProperty: GraphProperty<GormType> = propertyGraph.property(GormType.HIBERNATE)

      override fun addFieldsAfter(layout: Panel) {
        layout.row(GrailsBundle.message("module.builder.servlet.type")) {
          segmentedButton(ServletType.entries.toList()) { text = it.messagePointer.get() }
            .bind(servletProperty)
        }.bottomGap(BottomGap.SMALL)

        setServletType(ServletType.JETTY)
        servletProperty.afterChange { setServletType(it) }

        layout.row(GrailsBundle.message("module.builder.gorm.type")) {
          segmentedButton(GormType.entries.toList()) { text = it.message }
            .bind(gormProperty)
        }.bottomGap(BottomGap.SMALL)

        setGormType(GormType.HIBERNATE)
        gormProperty.afterChange { pluginType ->
          setGormType(pluginType)
        }
      }
    }
  }

  private class GrailsFeatureCategory(
    private val appTypeId: String,
    title: String,
    val features: MutableList<WebStarterDependency> = mutableListOf()
  ) : WebStarterDependencyCategory(title, features) {
    override fun isAvailable(starterContext: WebStarterContext): Boolean {
      return starterContext.applicationType?.id == appTypeId
    }
  }

  private enum class ServletType(
    val id: String,
    val messagePointer: Supplier<String>
  ) {
    NONE("NONE", GrailsBundle.messagePointer("module.builder.servlet.type.none")),
    TOMCAT("TOMCAT", GrailsBundle.messagePointer("module.builder.servlet.type.tomcat")),
    JETTY("JETTY", GrailsBundle.messagePointer("module.builder.servlet.type.jetty")),
    UNDERTOW("UNDERTOW", GrailsBundle.messagePointer("module.builder.servlet.type.undertow"))
  }

  private enum class GormType(
    val id: String,
    val message: String
  ) {
    HIBERNATE("HIBERNATE", "Hibernate"),
    MONGODB("MONGODB", "MongoDB"),
    NEO4J("NEO4J", "Neo4j")
  }
}