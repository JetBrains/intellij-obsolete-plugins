package com.intellij.aiplayground.tests.gherkin

import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

abstract class GherkinTestBase : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {
  protected val fixture: CodeInsightTestFixture
    get() {
      return myFixture
    }
  private var steps = mutableMapOf<String, (List<String>) -> Unit>()
  private var backgroundSteps: List<String> = emptyList()
  private var lastStepType: String = "Given"
  private var executedSteps = mutableSetOf<String>()
  private var failedSteps = mutableSetOf<String>()
  private var pendingSteps = mutableSetOf<String>()
  private var skippedSteps = mutableSetOf<String>()

  protected lateinit var settingsManager: ApplicationSettingsManagerService
  protected lateinit var settingsState: PlaygroundSettings
  protected lateinit var llmServiceManager: LlmServiceManager
  protected lateinit var chatRepository: ChatRepository

  public override fun setUp() {
    println("Starting setUp()")
    super.setUp()

    // Initialize test application
    TestApplicationManager.getInstance()

    // Initialize services
    val app = ApplicationManager.getApplication()
    settingsManager = app.service<ApplicationSettingsManagerService>()
    settingsState = app.service<PlaygroundSettings>()
    llmServiceManager = app.service<LlmServiceManager>()
    chatRepository = app.service<ChatRepository>()
    println("Services initialized")

    // Initialize Gherkin steps
    steps.clear() // Clear existing steps instead of creating a new map
    println("Steps map initialized")
    registerCommonSteps()
    println("Common steps registered")
  }

  public override fun tearDown() {
    super.tearDown()
  }

  protected fun given(pattern: String, action: (List<String>) -> Unit) {
    println("Registering step: Given $pattern") // Debug logging
    val stepPattern = "Given $pattern"
    steps[stepPattern] = action
    println("Steps after registration: ${steps.keys.joinToString(", ")}") // Debug logging
  }

  protected fun when_(pattern: String, action: (List<String>) -> Unit) {
    val stepPattern = "When $pattern"
    steps[stepPattern] = action
    println("Registered step: $stepPattern") // Debug logging
  }

  protected fun then(pattern: String, action: (List<String>) -> Unit) {
    val stepPattern = "Then $pattern"
    steps[stepPattern] = action
    println("Registered step: $stepPattern") // Debug logging
  }

  protected fun and(pattern: String, action: (List<String>) -> Unit) {
    // Register the step with both "And" and the actual type
    val andPattern = "And $pattern"
    val actualPattern = "$lastStepType $pattern"
    steps[andPattern] = action
    steps[actualPattern] = action
    println("Registered steps: $andPattern, $actualPattern") // Debug logging
  }

  fun executeFeature(featureFile: String) {
    val featureContent = loadFeatureContent(featureFile)
                         ?: throw IllegalStateException("Feature file not found: $featureFile")

    val scenarios = parseFeature(featureContent)
    scenarios.forEach { scenario ->
      try {
        // Reset step type for each scenario
        lastStepType = "Given"
        // Execute background steps before each scenario
        backgroundSteps.forEach { executeStep(it) }
        // Execute scenario steps
        scenario.forEach { executeStep(it) }
      }
      catch (e: Throwable) {
        throw AssertionError("Failed to execute scenario with steps: ${scenario.joinToString("\n")}", e)
      }
    }
  }

  private fun loadFeatureContent(featureFile: String): String? {
    javaClass.classLoader.getResourceAsStream(featureFile)?.use { stream ->
      return stream.bufferedReader().readText()
    }
    return null
  }

  private fun parseFeature(content: String): List<List<String>> {
    val lines = content.lines().map { it.trim() }
    var inBackground = false
    var inScenario = false
    val scenarios = mutableListOf<MutableList<String>>()
    var currentScenario = mutableListOf<String>()

    lines.forEach { line ->
      when {
        line.startsWith("Background:") -> {
          inBackground = true
          inScenario = false
        }
        line.startsWith("Scenario:") -> {
          if (inScenario && currentScenario.isNotEmpty()) {
            scenarios.add(currentScenario)
          }
          currentScenario = mutableListOf()
          inBackground = false
          inScenario = true
        }
        line.startsWith("Given ") || line.startsWith("When ") ||
        line.startsWith("Then ") || line.startsWith("And ") -> {
          if (inBackground) {
            backgroundSteps = backgroundSteps + line
          }
          else if (inScenario) {
            currentScenario.add(line)
          }
        }
      }
    }
    if (currentScenario.isNotEmpty()) {
      scenarios.add(currentScenario)
    }
    return scenarios
  }

  private fun executeStep(step: String) {
    val stepType = when {
      step.startsWith("And ") -> lastStepType
      else -> step.split(" ")[0].also { lastStepType = it }
    }

    val actualStep = if (step.startsWith("And ")) {
      "$stepType ${step.substringAfter("And ")}"
    }
    else {
      step
    }

    val matchingStep = steps.entries.find { (pattern, _) ->
      matchPattern(actualStep, pattern)
    } ?: throw IllegalStateException("No matching step found for: $actualStep\nRegistered steps: ${steps.keys.joinToString("\n")}")

    val params = extractParams(actualStep, matchingStep.key)
    matchingStep.value(params)
  }

  private fun matchPattern(step: String, pattern: String): Boolean {
    val normalizedStep = step.replace(Regex("""("[^"]*")""")) { "\".*\"" }
    val normalizedPattern = pattern.replace(Regex("""("[^"]*")""")) { "\".*\"" }

    val regex = normalizedPattern
      .replace(Regex("""\{[^}]+}"""), "([^\\s]+)")
      .replace(Regex("""\$[^\s]+"""), "([^\\s]+)")
      .let { "^${Regex.escape(it).replace("\".*\"", "\".*\"")}\$" }
      .let { Regex(it) }

    return regex.matches(normalizedStep)
  }

  private fun extractParams(step: String, pattern: String): List<String> {
    val quotedParams = Regex("""["']([^"']*)["']""").findAll(step)
      .map { it.groupValues[1] }
      .toList()

    val regex = pattern
      .replace(Regex("""\{[^}]+}"""), "([^\\s]+)")
      .replace(Regex("""\$[^\s]+"""), "([^\\s]+)")
      .let { Regex("^$it$") }

    val otherParams = regex.find(step)?.groupValues?.drop(1) ?: emptyList()

    return quotedParams + otherParams
  }

  private fun registerCommonSteps() {
    println("Starting registerCommonSteps()") // Debug logging
    println("Current steps before registration: ${steps.keys.joinToString(", ")}") // Debug logging

    // Common setup steps
    given("the AI Playground plugin is installed") {
      setupPlugin()
    }

    given("I have configured valid API credentials") {
      setupApiCredentials()
    }

    given("the chat window is open") {
      openChatWindow()
    }

    println("Finished registerCommonSteps(), final steps: ${steps.keys.joinToString(", ")}") // Debug logging
  }

  protected open fun setupPlugin() {
    // Base plugin setup - can be overridden by specific test classes
    assertNotNull("Settings manager should be available", settingsManager)
    assertNotNull("Settings state should be available", settingsState)
    assertNotNull("LLM service manager should be available", llmServiceManager)
  }

  protected open fun setupApiCredentials() {
    // Base API credentials setup - can be overridden by specific test classes
  }

  protected open fun openChatWindow() {
    // Base chat window opening logic - can be overridden by specific test classes
  }
} 