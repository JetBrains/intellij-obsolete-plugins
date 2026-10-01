# Test Scenarios

## Overview

This document outlines the behavior-driven development (BDD) test scenarios for the AI Playground plugin, focusing on chat functionality and
Python development integration.

## 1. Chat Functionality

### 1.1 Basic Chat Operations

- **Single Provider Chat**
  ```gherkin
  Given the chat window is open
  And a provider is configured
  When I send a message
  Then the provider should respond
  And the response should stream in real-time
  And the message should be saved in history
  ```

- **Multi-Provider Chat**
  ```gherkin
  Given multiple providers are configured
  And the chat window is open
  When I send a message
  Then all active providers should respond
  And responses should stream concurrently
  And be displayed side by side
  ```

### 1.2 Chat History Management

- **History Persistence**
  ```gherkin
  Given I have multiple chat sessions
  When I close and reopen the IDE
  Then all chat histories should be preserved
  And include message timestamps
  And include token usage information
  ```

- **Chat Navigation**
  ```gherkin
  Given I have multiple historical chats
  When I switch between chats
  Then each chat's messages should load correctly
  And maintain their provider configurations
  And show accurate token usage statistics
  ```

## 2. Python Integration

### 2.1 Context Awareness

- **API Call Detection**
  ```gherkin
  Given I have a Python file with LLM API calls
  When I open the file in the editor
  Then API calls should be highlighted
  And quick chat actions should be available
  ```

- **Parameter Extraction**
  ```gherkin
  Given I have a Python file with LLM API calls
  When I open chat from an API call
  Then relevant parameters should be extracted
  And used in the chat context
  ```

### 2.2 IDE Integration

- **Tool Window Integration**
  ```gherkin
  Given PyCharm is running
  When the plugin is active
  Then the chat window should integrate with IDE
  And follow the IDE theme
  And preserve state across IDE restarts
  ```

## 3. Provider Management

### 3.1 Provider Configuration

- **Provider Setup**
  ```gherkin
  Given I am in settings
  When I configure a new provider
  Then the provider should be validated
  And available models should be listed
  And credentials should be stored securely
  ```

- **Model Selection**
  ```gherkin
  Given I have multiple providers
  When I select models for comparison
  Then selected models should be activated
  And configurations saved per chat
  ```

### 3.2 Error Handling

- **Provider Failures**
  ```gherkin
  Given I am chatting with multiple providers
  When one provider fails
  Then other providers should continue working
  And appropriate error should be shown
  And retry option should be available
  ```

## 4. Performance and Security

### 4.1 Resource Management

- **Memory Usage**
  ```gherkin
  Given I have long chat sessions
  When accumulating chat history
  Then memory usage should remain stable
  And performance should not degrade
  ```

- **Concurrent Operations**
  ```gherkin
  Given multiple providers are active
  When streaming responses concurrently
  Then UI should remain responsive
  And responses should be properly synchronized
  ```

### 4.2 Security

- **Credential Management**
  ```gherkin
  Given I have provider credentials
  When storing them
  Then they should be encrypted
  And not exposed in logs or UI
  ```

- **Data Privacy**
  ```gherkin
  Given I have sensitive chat data
  When managing chat history
  Then data should be stored securely
  And follow retention policies
  ```

## Test Implementation Notes

### Running Tests

```bash
./bazel.cmd test //python/intellij.aiplayground/tests:test
```

### Test Structure

- Features organized by functionality
- Scenarios focus on user-facing behavior
- Each scenario includes clear success criteria
- Emphasis on Python/PyCharm integration
- Focus on chat and provider management

### Implementation Examples

#### 1. Basic Test Structure

```kotlin
@RunWith(GherkinTestRunner::class)
@GherkinTest("features/chat.feature")
class ChatFeatureTest : GherkinTestBase() {
  @Before
  override fun setUp() {
    super.setUp()
    registerChatSteps()
  }

  private fun registerChatSteps() {
    // Background steps
    given("the AI Playground plugin is installed") {
      assertNotNull("Settings manager should be available", settingsManager)
      assertNotNull("LLM service should be available", llmService)
    }

    // Test steps
    given("the chat window is open") {
      // Setup code
    }

    when_("I send a message") {
      // Action code
    }

    then("I should receive a response") {
      // Verification code
    }
  }
}
```

#### 2. Multi-Model Chat Example

```kotlin
@RunWith(GherkinTestRunner::class)
@GherkinTest("features/multi_model_chat.feature")
class MultiModelChatTest : GherkinTestBase() {
  private fun registerMultiModelSteps() {
    given("multiple models are active") {
      val providers = createTestProviders()
      providers.forEach { provider ->
        settingsManager.storeApiKey(provider, "test-key-${provider.id}")
      }
    }

    when_("I send a message") {
      runBlocking {
        llmService.complete("test message", provider = testProvider)
      }
    }

    then("all active models should respond") {
      val responses = chatHistory.value
      assertTrue("Should have multiple responses",
                 responses.count { it is AssistantMessage } > 1)
    }
  }
}
```

#### 3. Chat History Example

```kotlin
@RunWith(GherkinTestRunner::class)
@GherkinTest("features/chat_history.feature")
class ChatHistoryTest : GherkinTestBase() {
  private fun registerHistorySteps() {
    given("multiple chat sessions exist") {
      val chat1 = chatRepository.createChat("Chat 1")
      val chat2 = chatRepository.createChat("Chat 2")
      chatRepository.updateActiveChat(listOf(
        UserMessage("test message 1"),
        AssistantMessage("response 1", testProvider)
      ))
    }

    when_("I close and reopen the IDE") {
      simulateIDERestart()
    }

    then("all chat histories should be preserved") {
      val chats = chatRepository.getAllChats().value
      assertTrue("Should have multiple chats", chats.size > 1)
      val messages = chats.first().messages
      assertTrue("Should have preserved messages", messages.isNotEmpty())
    }
  }
}
```

#### 4. Python Integration Example

```kotlin
@RunWith(GherkinTestRunner::class)
@GherkinTest("features/python_integration.feature")
class PythonIntegrationTest : GherkinTestBase() {
  private fun registerPythonSteps() {
    given("a Python file with LLM API calls") {
      fixture.configureByText("test.py", """
                import openai
                response = openai.ChatCompletion.create(
                    model="gpt-3.5-turbo",
                    messages=[{"role": "user", "content": "Hello"}]
                )
            """.trimIndent())
    }

    when_("viewing the file") {
      // Action happens automatically when file is opened
    }

    then("highlight API calls appropriately") {
      val highlights = fixture.doHighlighting()
      assertTrue("Should have API call highlights",
                 highlights.any { it.text.contains("openai.ChatCompletion.create") })
    }
  }
}
```

### Test Utilities

The `GherkinTestBase` class provides several utilities:

1. Service Access

```kotlin
protected lateinit var settingsManager: SettingsManagerService
protected lateinit var settingsState: AppSettingsState
protected lateinit var llmService: LlmService
```

2. Test Fixture

```kotlin
protected lateinit var fixture: CodeInsightTestFixture
```

3. Step Registration Methods

```kotlin
protected fun given(pattern: String, action: (List<String>) -> Unit)
protected fun when_(pattern: String, action: (List<String>) -> Unit)
protected fun then(pattern: String, action: (List<String>) -> Unit)
protected fun and(pattern: String, action: (List<String>) -> Unit)
```

4. Common Setup Methods

```kotlin
protected open fun setupPlugin()
protected open fun setupApiCredentials()
protected open fun openChatWindow()
```

### Best Practices

1. **Step Organization**

- Register steps in logical groups
- Use clear, descriptive step names
- Keep step implementations focused

2. **Test Data**

- Use test providers and models
- Create minimal test data
- Clean up after tests

3. **Assertions**

- Use descriptive assertion messages
- Verify both positive and negative cases
- Check state consistency

4. **Error Handling**

- Test error scenarios
- Verify error messages
- Check recovery behavior