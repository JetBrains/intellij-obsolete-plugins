# Active Context

## Project Overview

The AI Playground is an IntelliJ IDEA plugin that provides integration with various LLM providers (OpenAI, Anthropic Claude, Mistral AI)
directly within the IDE.

## Current Focus

We are fixing the SettingsFeatureTest which tests the plugin's settings and configuration functionality.

## Key Components

### Settings Management

- `SettingsManagerService`: Core service managing all settings
- `PlaygroundSettingsState`: Handles persistent app settings
- `SessionSettings`: Manages current session state
- `LlmSettingsViewModel`: Handles settings UI state and validation

### Test Structure

- BDD tests using Gherkin format
- Test scenarios defined in `tests/spec/features/settings.feature`
- Implementation in `SettingsFeatureTest.kt`

### Important Notes

1. Settings are provider-specific and need to be stored separately
2. API keys must be securely stored and validated
3. Settings changes must persist across IDE restarts
4. UI must reflect current settings state accurately

## Critical Requirements

1. All API credentials must be validated before storage
2. Invalid credentials must not be saved
3. Provider settings must be isolated
4. Settings changes must be immediately reflected in UI
5. Theme changes must be properly handled

## Current Status

Working on fixing SettingsFeatureTest scenarios with focus on:

1. API credential management
2. Provider configuration
3. Model selection
4. Settings persistence

## Current Task

Aligning tests with project specifications and requirements

## Analysis Phase

### Project Structure Analysis

- Located in `/python/intellij.aiplayground/tests/spec/features/`
- Current BDD feature files:
  - `integration.feature`
  - `completion.feature`
  - `provider.feature`
  - `settings.feature`

### Dependencies

- Using Cucumber for BDD testing (TR2.1)
- Tests run via Bazel: `./bazel.cmd test //python/intellij.aiplayground/tests:test`

### Current Patterns

- Each feature file follows Gherkin syntax
- Common background setup in each feature
- Scenario-based test organization
- Provider-specific test isolation

### Test Coverage Analysis

#### Alignment Issues

1. **Missing Core Features**

- No dedicated chat functionality tests
- No Python integration tests
- No history management tests
- No streaming response tests

2. **Outdated Features**

- Completion functionality tests present despite removal from goals
- Integration tests include completion scenarios

3. **Coverage Gaps**

- Python-specific integrations (FR3.1-FR3.6)
- Chat history management (FR5.1-FR5.6)
- Streaming responses (FR1.1-FR1.6)
- Token usage tracking (NFR1.2)
- Chat data privacy (NFR2.4)
- Concurrent chat performance (NFR1.3)

### Required Changes

#### Files to Remove

- `completion.feature` - Obsolete functionality

#### Files to Add

1. `chat.feature`

- Core chat functionality
- Multi-model interactions
- Streaming responses

2. `python_integration.feature`

- IDE integration
- Context awareness
- API call detection

3. `history.feature`

- Chat persistence
- Token tracking
- Session management

#### Files to Update

1. `integration.feature`

- Remove completion scenarios
- Add chat integration tests
- Add Python context tests

2. `provider.feature`

- Focus on chat interactions
- Add streaming scenarios
- Update model management

3. `settings.feature`

- Add chat-specific settings
- Add retention policies
- Update provider configuration

## Next Steps

1. Await review of Analysis phase
2. Proceed to Specification phase:

- Update requirements.md
- Update test_scenarios.md
- Create new feature files

3. Implementation phase will follow after specification review

## Current Task

Implementing chat-focused functionality with Python/PyCharm integration and multi-model comparison capabilities.

## Phase: Specification

### Project Analysis Status ✓

1. **Code Structure** ✓

- Reviewed README.md
- Examined project structure
- Identified key components
- Located relevant documentation
- Identified Python-specific integration points
- Located editor integration points
- Analyzed ChatViewModel and ChatScreen implementations

2. **Dependencies** ✓

- Identified build system (Bazel)
- Located test commands
- Understood project root context
- Identified PyCharm SDK dependencies
- Located Python-specific components

3. **Current Patterns** ✓

- ChatViewModel provides comprehensive multi-model support:
  - Multiple active models tracking
  - Provider state management
  - Concurrent message processing
  - Response streaming
  - Model persistence
- ChatScreen implements full multi-model UI:
  - Provider selection interface
  - Response comparison view
  - Streaming updates
  - Model selection UI
- Chat persistence using JSON files:
  - Stored in IDE system directory
  - One file per chat
  - Full history with metadata
  - Token usage tracking
- Need to implement API call highlighting
- Need to enhance provider management

4. **Test Implementation** ✓

- Located in tests directory
- Using JUnit4 with GherkinTestBase
- BDD test structure defined
- Test command: `./bazel.cmd test //python/intellij.aiplayground/tests:test`
- Need to add Python integration tests

### Documentation Status

1. **Project Context** ✓

- Created docs/project_context.md
- Documented current state
- Outlined goals and non-goals
- Identified affected components
- Added Python/PyCharm focus
- Documented existing multi-model capabilities

2. **Architecture** ✓

- Updated docs/architecture.md
- Documented component structure
- Added data flow diagrams
- Clarified chat persistence
- Added Python integration details
- Documented multi-model support

3. **Requirements** ✓

- Updated docs/requirements.md
- Focused on chat functionality
- Added Python integration requirements
- Specified multi-model features
- Defined persistence requirements
- Added provider management needs

4. **Test Scenarios** ✓

- Created docs/test_scenarios.md
- Defined BDD scenarios
- Added implementation examples
- Documented test utilities
- Specified best practices
- Added Python integration tests

### Current Phase Tasks

1. **Specification Phase** (In Progress)

- ✓ Update project context
- ✓ Update architecture documentation
- ✓ Update requirements
- ✓ Create test scenarios
- ⚪ Create technical specifications for:
  - Python API call detection
  - Provider management enhancements
  - Chat history optimization
- ⚪ Review and finalize specifications

2. **Implementation Phase** (Not Started)

- Remove completion code
- Update provider interface
- Remove completion tests
- Update documentation
- Implement Python integration
- Enhance provider management
- Add API call highlighting

## Current Focus

Completing technical specifications for remaining features before transitioning to Implementation phase.

## Tracked Changes

### Documentation Updates

1. Created project_context.md ✓
2. Created active_context.md ✓
3. Updated architecture.md ✓
4. Updated requirements.md ✓
5. Created test_scenarios.md ✓
6. Pending:

- Create Python integration spec
- Create provider management spec
- Create chat optimization spec

### Code Changes (Planned)

1. Remove completion functionality
2. Update LLMProvider interface
3. Remove completion tests
4. Add Python/PyCharm integration
5. Enhance provider management
6. Add API call highlighting

## Notes

- All changes must be run from repository root (../../)
- Test command: `./bazel.cmd test //python/intellij.aiplayground/tests:test`
- BDD tests use JUnit4 with GherkinTestBase
- Chat persistence uses JSON files in IDE system directory
- Focus on Python/PyCharm integration
- Leverage existing multi-model chat implementation
- Ensure robust provider management

## Current Task: Chat Integration

Converting the chat interface from a tool window to a window-based implementation.

### Requirements

1. Multiple windows can be opened simultaneously

- Each chat can have its own window
- Multiple chats can be open at the same time

2. One window per chat

- Each chat should have exactly one window
- No duplicate windows for the same chat

3. Focus management

- When selecting a chat that already has an open window, focus should move to that window
- No new window should be created if one already exists for the chat

4. IDE Integration

- Using existing window manager from platform
- Reference implementation: `com.intellij.ide.actions.ShowSettingsUtilImpl.Companion#showInternal`

### Current Implementation Analysis

1. Chat UI Components

- Currently implemented as tool window in `ChatToolWindow`
- Uses JetBrains Compose for UI
- Supports multiple active models
- Has streaming response handling

2. Chat State Management

- `ChatViewModel` handles chat state
- Supports multiple active providers
- Manages chat history and persistence
- Handles token usage tracking

3. Window Integration Points

- Need to use `WindowManager` for window creation
- Need to implement window state persistence
- Need to handle window focus management
- Need to implement window-specific UI components

### Implementation Plan

1. Create new window-based UI components
2. Implement window state management
3. Handle chat-window mapping
4. Implement focus management
5. Update existing chat functionality
6. Add window persistence 