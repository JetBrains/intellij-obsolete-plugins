# AI Playground Architecture

## Overview

The AI Playground plugin is designed as a modular PyCharm/IntelliJ IDEA plugin that provides seamless chat-based integration with various
LLM providers, with a focus on Python development. The architecture follows clean architecture patterns to ensure maintainability and
extensibility.

## Core Components

### 1. LLM Integration (`models` package)

```kotlin
com.intellij.aiplayground.models
├── LlmService          // Core interface for LLM chat operations
├── LlmProvider        // Provider interface and implementations
├── LlmModel          // Model data classes
├── ChatRequestConfig // Chat request configuration
└── LlmResponse      // Response models
```

The LLM service handles:

- Initialization of LLM providers
- Chat message streaming
- Token usage tracking
- Error handling
- Multi-model response coordination
- Provider parameter management

### 2. Chat System (`models.chat` package)

```kotlin
com.intellij.aiplayground.models.chat
├── ChatViewModel           // Business logic and state management with multi-provider support
├── ChatViewModelInterface // Interface for chat functionality
├── ChatMessage           // Message data classes
├── ChatRepository       // Chat history management
└── ChatPersistence     // Chat persistence handling
```

Features:

- Real-time message streaming
- Multi-provider chat support
- Chat history management
- Message formatting
- Context management
- Multi-model response handling
- Response comparison interface

### 3. Python Integration (`python` package)

```kotlin
com.intellij.aiplayground.python
├── PythonContext         // Python code context handling
├── ApiCallDetector      // LLM API call detection
├── ParameterExtractor  // API parameter extraction
└── PythonUI           // Python-specific UI components
```

Features:

- Python code parsing
- API call detection
- Parameter extraction
- Context-aware assistance
- Python-specific UI elements

### 4. UI Layer (`ui` package)

```kotlin
com.intellij.aiplayground.ui
├── chat/                // Chat-related UI components
│   ├── ChatToolWindow  // Main chat interface with multi-provider support
│   ├── ChatScreen     // Multi-model chat and comparison UI
│   ├── ModelSelector  // Model selection UI
│   └── components/   // Reusable UI components
├── python/           // Python-specific UI
│   ├── ApiCallHighlighter
│   └── ContextViewer
├── providers/        // Provider management UI
│   └── ParameterManager
└── common/          // Common UI utilities
```

Built with JetBrains Compose:

- Native IDE look and feel
- Multi-provider chat support
- Multi-model response comparison
- Responsive design
- Theme integration
- Accessibility support
- Python integration
- Provider management

### 5. Settings (`settings` package)

```kotlin
com.intellij.aiplayground.settings
├── PlaygroundSettingsState    // Settings storage
├── SettingsComponent         // Settings UI
├── ProviderSettings        // Provider configuration
└── TokenSettings         // Token management
```

Manages:

- Model selection and configuration
- UI preferences
- Provider settings
- Default configurations
- Token usage and costs
- Model parameters

### 6. Provider Implementations

```kotlin
com.intellij.aiplayground
├── openai/    // OpenAI provider implementation
├── anthropic/ // Anthropic provider implementation
└── mistral/   // Mistral provider implementation
```

Features:

- Provider-specific chat implementations
- API integration
- Model management
- Response handling
- Token usage tracking
- Parameter management

## Data Flow

1. **Chat Flow**

```
User Input → ChatViewModel → Active Provider(s) → Response Collection → UI Update
```

Note: ChatViewModel handles all chat interactions, managing both single and multiple providers through the same flow. When multiple
providers are active, responses are collected and displayed in comparison view.

2. **Python Integration Flow**

```
Python File → ApiCallDetector → ParameterExtractor → ChatToolWindow → LlmService
```

3. **Token Usage Flow**

```
Provider Response → TokenUsage Calculation → AssistantMessage → Session Statistics Update
```

## Implementation Details

### 1. ChatViewModel Implementation

- Manages multiple active models via _activeModels StateFlow
- Tracks active providers through _activeProviders StateFlow
- Handles multi-provider message sending
- Manages per-provider model selection
- Processes concurrent streaming responses
- Persists chat state and model selections

### 2. ChatScreen Implementation

- Provides multi-provider selection interface
- Manages provider state in UI
- Handles automatic provider initialization
- Displays multi-model streaming responses
- Implements response comparison interface
- Shows provider-specific messages

### 3. Multi-Model Support

- Concurrent message processing
- Provider-specific response handling
- Model state persistence
- Response synchronization
- Provider state management
- UI comparison features

### 4. Token Usage and Chat History Implementation

Token usage is tracked at two levels, while chat history is persisted across sessions:

```kotlin
// Message-level token tracking - persisted with chat history
data class TokenUsage(
  val promptTokens: Int,    // Tokens in the prompt
  val completionTokens: Int, // Tokens in the completion
  val totalTokens: Int      // Total tokens used
)

// Token usage in assistant messages - part of persisted chat history
data class AssistantMessage(
  val tokenUsage: TokenUsage?,  // Per-message token usage
  val provider: LlmProvider,    // Provider that generated the response
  val model: String?,           // Model used for generation
  // ... other fields
)

// Current session statistics - resets when IDE restarts
interface SessionSettingsState {
  val totalTokensUsed: StateFlow<Int>  // Running total for current session
  val requestCount: StateFlow<Int>     // Number of requests in current session
  fun recordRequest(tokenCount: Int, responseTimeMs: Long)
}

// Chat persistence across sessions
interface ChatRepository {
  fun getAllChats(): StateFlow<List<Chat>>  // Get all available chats
  fun getChat(id: String): Chat?           // Get a specific chat
  fun saveChat(chat: Chat)                // Save chat with all messages and token usage
  fun createChat(): Chat                 // Create a new chat
  fun getActiveChat(): StateFlow<Chat?> // Get currently active chat
}

// Chat data model with history
data class Chat(
  val id: String,
  val title: String,
  val messages: List<ChatMessage>,     // Includes all messages with token usage
  val activeModels: Set<Pair<String, String>>, // Provider-Model pairs
  val createdAt: Long,
  val updatedAt: Long
)

### Chat History Implementation

Chat history is persisted using JSON files in the IDE 's system directory:

```kotlin
/**
 * Serializer for persisting chats to disk
 */
class ChatPersistenceSerializer {
  private val chatDirectoryPath: String
    get() = "${PathManager.getSystemPath()}/aiplayground/chats"

  // Each chat is stored in a separate JSON file
  // Format: ${chat.id}.json
}

// Chat data model
data class Chat(
  val id: String,
  val title: String,
  val messages: List<ChatMessage>,     // Includes all messages with token usage
  val activeModels: Set<Pair<String, String>>, // Provider-Model pairs
  val createdAt: Long,
  val updatedAt: Long
)

// Serializable DTO for chat
@Serializable
data class ChatDto(
  val id: String,
  val title: String,
  val messages: List<ChatMessageDto>,
  val createdAt: Long,
  val updatedAt: Long,
  val activeModels: List<ProviderModelPairDto>
)
```

Features:

- File-based persistence in IDE system directory
- One JSON file per chat for efficient loading/saving
- Full chat history with messages and metadata
- Provider and model configurations per chat
- Message-level token usage tracking
- Automatic cleanup of deleted chats
- Backward compatibility support
- Error recovery for corrupted files

## Performance Considerations

1. **Memory Management**

- Chat history pagination
- Response streaming
- Resource cleanup
- Cache management

2. **Threading**

- UI thread safety
- Background processing
- Coroutine usage
- Resource pooling

## Build System

### Bazel Configuration

The project uses Bazel as the build system, which must be run from the repository root using the `bazel.cmd` script.

1. **Build Location**

```bash
# Always run from repository root
cd ../..  # From plugin directory
```

2. **Build Targets**

```python
# Main plugin target
//python/intellij.aiplayground:plugin

# Test targets
//python/intellij.aiplayground/test:all
```

3. **Path Resolution**

- When Bazel shows paths in outputs (e.g. `python/intellij.aiplayground/test/...`), resolve them relative to the plugin content root
- Example: `python/intellij.aiplayground/test/MyTest.kt` → `test/MyTest.kt`

4. **Common Commands**

```bash
# From repository root (../../)
cd ../..

# Build plugin
./bazel.cmd build //python/intellij.aiplayground:plugin

# Run tests
./bazel.cmd test //python/intellij.aiplayground/...

# Clean build
./bazel.cmd clean
```

## Security

1. **Provider Configuration**

- API key validation
- Secure transmission
- Access logging
- Provider-specific security

2. **Data Privacy**

- Local storage encryption
- Data retention policies
- Provider data handling
- Secure defaults

## Testing Strategy

1. **Unit Tests**

```kotlin
@Test
fun `should handle successful chat response`() = runTest {
    val service = mockk<LlmService>()
    coEvery { service.complete(any()) } returns flowOf(successResponse)
    val result = service.complete("chat message").first()
    assertEquals(expected, result)
  }
```

2. **Running Tests with Bazel**

```bash
# From repository root (../../)
cd ../..

# Run all tests
./bazel.cmd test //python/intellij.aiplayground/tests:test

# Run with debug output
./bazel.cmd test //python/intellij.aiplayground/tests:test --test_output=all

# Run specific module tests
./bazel.cmd test //python/intellij.aiplayground/settings:aiplayground-settings_test
```

3. **UI Tests**

```kotlin
@Test
fun `should display chat loading state`() {
  composeTestRule.setContent {
    ChatScreen(viewModel)
  }
  composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
}
```

## Best Practices

1. **Error Handling**

```kotlin
sealed class Result<out T> {
  data class Success<T>(val data: T) : Result<T>()
  data class Error(val exception: Throwable) : Result<Nothing>()
}
```

2. **Coroutines Usage**

```kotlin
class ChatViewModel {
  fun performAction() = viewModelScope.launch {
    try {
      withContext(Dispatchers.IO) {
        // Chat-related async operations
      }
    }
    catch (e: Exception) {
      handleError(e)
    }
  }
}
```

3. **Provider Implementation**

```kotlin
class OpenAiProvider : LlmProvider {
  override suspend fun complete(request: ChatRequest): Flow<LlmResponse> = flow {
    // Chat-specific implementation
  }
}
```

## Test Architecture

### 1. BDD Test Structure

```kotlin
com.intellij.aiplayground.tests
├── spec/                    // BDD specifications
│   ├── features/           // Gherkin feature files
│   │   ├── chat.feature           // Core chat functionality
│   │   ├── python_integration.feature // Python integration
│   │   ├── history.feature        // Chat history management
│   │   ├── integration.feature    // Component integration
│   │   ├── provider.feature       // Provider management
│   │   └── settings.feature       // Settings management
│   └── steps/              // Step definitions
├── mocks/                  // Test mocks and stubs
└── utils/                 // Test utilities
```

### 2. Test Categories

1. **Functional Tests**

- Chat functionality
- Python integration
- Provider management
- Settings management
- History management

2. **Non-Functional Tests**

- Performance testing
- Security validation
- Resource management
- UI responsiveness

3. **Integration Tests**

- Component interaction
- State management
- Data consistency
- Error handling

### 3. Test Implementation Patterns

1. **Chat Testing**

- Multi-provider scenarios
- Streaming response validation
- History persistence checks
- Token usage verification

2. **Python Integration Testing**

- Context extraction validation
- API call detection
- Parameter handling
- IDE integration

3. **Provider Testing**

- Provider lifecycle management
- Model configuration
- Error handling
- Rate limiting

4. **Settings Testing**

- Configuration persistence
- Security validation
- UI interaction
- State management

### 4. Test Data Management

```kotlin
com.intellij.aiplayground.tests.data
├── ChatTestData        // Chat test fixtures
├── ProviderTestData   // Provider test configurations
├── PythonTestData    // Python code samples
└── TestConstants    // Common test constants
```

### 5. Test Execution

Tests are executed via Bazel:

```bash
./bazel.cmd test //python/intellij.aiplayground/tests:test
```

Key aspects:

- Isolated test environments
- Reproducible test execution
- Clear test dependencies
- Efficient test parallelization 