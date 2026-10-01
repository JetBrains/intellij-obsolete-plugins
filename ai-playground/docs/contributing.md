# Contributing to AI Playground

Thank you for your interest in contributing to the AI Playground plugin! This document provides guidelines and best practices for
contributing to the project.

## Development Setup

1. **Prerequisites**

- IntelliJ IDEA (Latest version)
- JDK 17 or later
- Kotlin 1.9.x
- Git

2. **Build and Run**
   ```bash
   # From repository root (../../)
   cd ../..
   
   # Build plugin
   ./bazel.cmd build //python/intellij.aiplayground:plugin
   
   # Run tests
   ./bazel.cmd test //python/intellij.aiplayground/tests:test
   
   # Run with debug output
   ./bazel.cmd test //python/intellij.aiplayground/tests:test --test_output=all
   ```

3. **Important Notes**

- Always run bazel.cmd from the repository root (../../)
- When Bazel shows paths like `python/intellij.aiplayground/test/...`, resolve them relative to the plugin content root
- Example: If you see `python/intellij.aiplayground/test/MyTest.kt` in an error message, look for `test/MyTest.kt` in the plugin directory

## Code Style

### Kotlin Style

- Use 2 spaces for indentation
- Maximum line length: 120 characters
- Use trailing commas in multi-line constructs
- Keep functions focused and small (< 30 lines preferred)

### Naming Conventions

- Classes: PascalCase (e.g., `ChatViewModel`)
- Functions/Properties: camelCase (e.g., `sendMessage`)
- Constants: SCREAMING_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`)
- Test functions: descriptive with underscores (e.g., `should_send_message_when_valid`)

### Documentation

```kotlin
/**
 * Provides a description of the class/function purpose
 *
 * @param param1 Description of first parameter
 * @return Description of return value
 * @throws Exception Description of when/why exception is thrown
 */
```

## UI Development

### Component Structure

```kotlin
@Composable
fun MyComponent(
  modifier: Modifier = Modifier,
  // Required parameters first
  requiredParam: Type,
  // Optional parameters with defaults last
  optionalParam: Type = DefaultValue
) {
  // Implementation
}
```

### Theme Usage

```kotlin
val colors = JewelTheme.globalColors
val isDark = JewelTheme.isDark

Box(
  modifier = Modifier
    .background(colors.panelBackground)
    .border(1.dp, colors.borders.normal)
)
```

## Testing

### Test Structure

The project includes several types of tests:

1. **Unit Tests** (`test/` in each module)

- Test individual components and functions
- Fast and isolated
- Use mocking for external dependencies

2. **Integration Tests** (`test/integration/`)

- Test interaction between components
- May require some external dependencies
- Focus on module integration points

3. **End-to-End Tests** (`test/e2e/`)

- Test complete workflows
- Require full system setup
- Slower but comprehensive

4. **BDD Feature Tests** (`tests/spec/features/`)

- Behavior-driven development tests
- Written in Gherkin syntax
- Focus on user scenarios

### Running Tests

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

### Unit Tests

```kotlin
@Test
fun `should handle successful response`() = runTest {
    // Given
    val service = mockk<LlmService>()
    coEvery { service.complete(any()) } returns flowOf(successResponse)

    // When
    val result = service.complete("prompt").first()

    // Then
    assertEquals(expected, result)
  }
```

### UI Tests

```kotlin
@Test
fun `should display loading state`() {
  composeTestRule.setContent {
    ChatScreen(viewModel)
  }

  composeTestRule
    .onNodeWithTag("loading_indicator")
    .assertIsDisplayed()
}
```

### Test Categories

Each test should be appropriately tagged for selective execution:

```kotlin
@Test
@Tag("unit")
fun `test specific functionality`() {
  // Test implementation
}
```

### Test Location

Tests should be placed in the corresponding module's test directory:

- Unit tests: `test/` directory in each module
- Integration tests: `test/integration/` directory
- End-to-end tests: `test/e2e/` directory
- BDD tests: `tests/spec/features/` directory

### Best Practices

1. **Test Naming**

- Use descriptive names
- Follow the pattern: `should_do_something_when_condition`
- Include the expected behavior

2. **Test Structure**

- Arrange (Given): Set up test data and conditions
- Act (When): Perform the action being tested
- Assert (Then): Verify the results

3. **Mocking**

- Use mockk for Kotlin mocking
- Mock only what's necessary
- Prefer real implementations for simple dependencies

4. **Test Coverage**

- Aim for high coverage of business logic
- Focus on critical paths
- Include edge cases and error scenarios

5. **Test Independence**

- Each test should be independent
- Clean up resources after tests
- Don't rely on test execution order

6. **Performance**

- Keep unit tests fast
- Group slow tests separately
- Use appropriate test categories

## Git Workflow

1. **Branch Naming**

- Features: `feature/description`
- Fixes: `fix/issue-number`
- Docs: `docs/description`

2. **Commit Messages**
   ```
   feat: add new model selection UI
   fix: resolve memory leak in chat history
   docs: update architecture documentation
   refactor: improve error handling
   ```

3. **Pull Request Process**

- Create branch from main
- Make changes
- Run tests
- Update documentation
- Create pull request
- Address review comments

## Code Review Checklist

- [ ] Code follows style guide
- [ ] Tests are included
- [ ] Documentation is updated
- [ ] No unnecessary dependencies
- [ ] Performance impact considered
- [ ] Error handling implemented
- [ ] Security considerations addressed

## Best Practices

### Error Handling

```kotlin
sealed class Result<out T> {
  data class Success<T>(val data: T) : Result<T>()
  data class Error(val exception: Throwable) : Result<Nothing>()
}

suspend fun performOperation(): Result<Data> = try {
  val data = service.getData()
  Result.Success(data)
}
catch (e: Exception) {
  logger.error("Failed to get data", e)
  Result.Error(e)
}
```

### Coroutines

```kotlin
class MyViewModel : ViewModel() {
  private val _state = MutableStateFlow<UiState>(UiState.Initial)
  val state = _state.asStateFlow()

  fun performAction() = viewModelScope.launch {
    _state.value = UiState.Loading
    try {
      withContext(Dispatchers.IO) {
        // IO operations
      }
      _state.value = UiState.Success
    }
    catch (e: Exception) {
      _state.value = UiState.Error(e)
    }
  }
}
```

## Getting Help

- Check existing issues
- Join developer discussions
- Review pull requests
- Improve documentation

Remember:

- Write clean, maintainable code
- Add tests for new features
- Update documentation
- Follow the code style guide
- Be respectful and constructive in discussions 