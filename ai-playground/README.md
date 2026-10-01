# AI Playground IntelliJ Plugin

An IntelliJ IDEA plugin that provides seamless integration with various Large Language Models (LLMs) directly within your IDE. Enhance your
development workflow with AI-powered assistance.

## Features

- 🤖 **Multiple LLM Providers**
  - OpenAI GPT models
  - Anthropic Claude models
  - Mistral AI models
  - Extensible provider system

- 💻 **Native IDE Integration**
  - Tool window integration
  - Context-aware assistance
  - Code generation
  - Native IDE look and feel

- 🔒 **Secure and Private**
  - Secure credential storage
  - Local history management
  - Privacy-focused design
  - Configurable data retention

- ⚡ **High Performance**
  - Streaming responses
  - Efficient resource usage
  - Background processing
  - Response caching

## Quick Start

1. **Installation**

- Install from JetBrains Marketplace
- Or build from source (from repository root):
    ```bash
    cd ../..  # Go to repository root
    ./bazel.cmd build //python/intellij.aiplayground:plugin
    ```

2. **Configuration**

- Open Settings/Preferences
- Navigate to Tools > AI Playground
- Configure your API keys
- Select preferred models

3. **Usage**

- Open AI Playground tool window
- Select a model
- Start chatting or coding
- Use context menu integrations

## Project Structure

```
intellij.aiplayground/
├── anthropic/              # Anthropic Claude integration
├── docs/                   # Documentation files
├── mistral/               # Mistral AI integration
├── models/                # Common model interfaces and utilities
├── openai/                # OpenAI integration
├── python/                # Python-specific functionality
├── resources/             # Resource files
├── settings/              # Plugin settings and configuration
├── test/                  # Test sources
├── testData/              # Test data files
└── ui/                    # UI components and Compose UI implementation
```

## Technology Stack

- **Kotlin** - Primary language
- **JetBrains Compose** - UI framework
- **JetBrains Jewel** - Design system
- **LangChain4j** - LLM integration
- **Kotlin Coroutines** - Async programming

## Documentation

- [Architecture](./docs/architecture.md) - Detailed technical architecture and design
- [Contributing](./docs/contributing.md) - Development workflow and guidelines
- [Requirements](./docs/requirements.md) - Project requirements and specifications
- [Test Scenarios](./docs/test_scenarios.md) - Test cases and scenarios
- [CLAUDE.md](./CLAUDE.md) - Claude-specific implementation details

Each document serves a specific purpose:

- **Architecture** - System design, components, data flow, and best practices
- **Contributing** - How to contribute, development workflow, and guidelines
- **Requirements** - Functional and non-functional requirements
- **Test Scenarios** - Test cases, testing strategy, and validation scenarios
- **CLAUDE.md** - Anthropic Claude integration specifics

## Contributing

We welcome contributions! Please see our [Contributing Guide](./docs/contributing.md) for details on:

- Development workflow
- Code style guidelines
- Pull request process
- Testing requirements

## Development Setup

1. **Prerequisites**

- IntelliJ IDEA (Latest version recommended)
- JDK 17 or later
- Kotlin 1.9.x
- Git

2. **Project Structure and Directory Context**

- This plugin is part of the larger IntelliJ platform repository
- The plugin root is at `intellij.aiplayground/`
- Repository root (REPO_ROOT) is two directories up from the plugin directory
- Directory structure:
  ```
  <parent_dirs>/
  └── REPO_ROOT/          # Repository root directory
      └── python/
          └── intellij.aiplayground/  # Plugin root (You are here)
  ```

3. **Important Build Notes**

- Bazel commands must be run from the repository root (REPO_ROOT)
- To check if you're in the repository root:
  ```bash
  # Should show your REPO_ROOT directory
  pwd
  
  # If you're in the plugin directory, go to repository root:
  cd ../..
  ```
- When Bazel shows paths like `python/intellij.aiplayground/test/...`, they are relative to REPO_ROOT
- Example: If you see `python/intellij.aiplayground/test/MyTest.kt` in an error message:
  - From REPO_ROOT: use the path as is
  - From plugin root: look for `test/MyTest.kt`

4. **Build and Run**
   ```bash
   # First, ensure you're in the repository root (REPO_ROOT)
   pwd  # Should show your REPO_ROOT directory path
   
   # If you're in the plugin directory, navigate to repository root
   cd ../..
   
   # Build the plugin
   ./bazel.cmd build //python/intellij.aiplayground:plugin
   ```

5. **Testing**
   ```bash
   # Ensure you're in the repository root (REPO_ROOT) first!
   pwd  # Should show your REPO_ROOT directory path
   
   # Run all tests
   ./bazel.cmd test //python/intellij.aiplayground/tests:test
   
   # Run with debug output
   ./bazel.cmd test //python/intellij.aiplayground/tests:test --test_output=all
   
   # Run specific module tests
   ./bazel.cmd test //python/intellij.aiplayground/settings:aiplayground-settings_test
   ```

   The project includes several types of tests:

- Unit tests: Located in each module's `test/` directory
- Integration tests: Located in `test/integration/`
- End-to-end tests: Located in `test/e2e/`
- BDD feature tests: Located in `tests/spec/features/`

6. **Common Issues**

- If `bazel.cmd` is not found, verify you're in REPO_ROOT
- If test paths don't resolve, check your current directory
- Always use `pwd` to verify your location before running commands
- Use absolute paths in Bazel commands (starting with //python/...)
