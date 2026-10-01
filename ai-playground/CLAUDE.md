# AI Playground Plugin Development Reference

## Project

- You cannot build or run the project by your own, ask human for that and always ask for the feedback

## Code Style
- Follow Kotlin style guidelines with 2-space indentation
- Prefer val over var, immutable data structures when possible
- Use descriptive names (camelCase for functions/variables, PascalCase for classes)
- Add KDoc comments for public APIs and complex methods
- Handle nullability explicitly with nullable types and safe calls
- Follow MVVM pattern for UI components
- Use coroutines for asynchronous operations; consider thread safety in UI operations

## Coroutines and Concurrency
- NEVER MIX REGULAR LOCKS AND COROUTINES - this can lead to deadlocks
- DO NOT use Java's synchronized blocks with coroutines
- DO NOT call suspending functions inside synchronized blocks
- Use kotlinx.coroutines.sync.Mutex for thread-safety when needed
- Use AtomicInteger/AtomicBoolean for simple shared counters
- Use withContext() to switch between dispatchers
- Use structured concurrency with launch{} within a defined scope
- In UI components, use viewModelScope or a lifecycle-aware coroutineScope
- Be careful with MutableStateFlow updates from multiple coroutines
- For complex state management, consider using a single-threaded context
- Always handle exceptions in coroutines using try/catch or supervisorScope

## Project Structure
- src directory has package prefix: "com.intellij.aiplayground"
- UI components use JetBrains Compose with Jewel components
- Follow IntelliJ plugin development guidelines
- Use proper disposal for resources with DisposableScope
- Implement token management for LLM API calls

## Model Selection UI Design
- Models are grouped by provider in a single scrollable list
- Model section starts empty by default for new users
- Selected models are visually distinct and persist between IDE restarts
- Model selection state is saved to persistent storage automatically
- Users can toggle models on/off, including removing the last one
- UI adapts responsively to available screen space
- Interface prioritizes clarity with minimal redundancy
- No initial welcome message for a cleaner start experience
- Message input supports Enter key for sending messages

## Chat UI Design
- Multiple chats are supported with persistent storage
- Chat messages are saved automatically between sessions
- Each chat maintains its own history and active model selection
- Chat selector header shows current chat with dropdown for switching
- Users can create, rename, and delete chats
- Chat titles are auto-generated from the first user message
- New chats inherit active model settings from global preferences
- Messages are displayed with provider attribution and timestamps

## Jewel UI Component Guidelines

### Components to Use
- Always use Jewel components from `org.jetbrains.jewel.ui.component.*` when available
- Prefer Jewel's `Text`, `TextField`, `Icon`, `IconButton` components over Material equivalents
- Use `JewelTheme.globalColors` for accessing theme colors
- Use standard `Box`, `Column`, `Row` from Compose foundation for layout

### Components to Avoid
- Do NOT use Material components (Material3 is not available in the project)
- Do NOT use `Button` - create custom buttons using `Box` + `clickable` modifier
- Do NOT use `Surface` - use `Box` with background and border instead
- Do NOT use Material's `Divider` - create a simple divider with a Box with height and background

### Dialogs and Modals
- Use `androidx.compose.ui.window.Dialog` for dialogs
- Build dialog content with `Box` and other foundation components
- Use theme colors: `JewelTheme.globalColors.panelBackground` for dialog backgrounds
- Use `JewelTheme.globalColors.borders.normal` for borders
- For dividers use: `Box` with `height(1.dp)` and background color

### Theme and Colors
- Use `JewelTheme.isDark` to check if dark theme is active
- Use `JewelTheme.globalColors.panelBackground` for default background
- Use `JewelTheme.globalColors.borders.normal` for borders
- Use `primaryColor` and `primaryColorLight` for accent colors
- Use `surfaceColor()` composable function to get the current surface color

### Icons
- Always use icons from `AllIconsKeys.*` namespace
- Common icons:
  - `AllIconsKeys.General.Add` (plus icon)
  - `AllIconsKeys.General.Remove` (minus icon)
  - `AllIconsKeys.General.Menu` (hamburger menu)
  - `AllIconsKeys.General.ChevronDown` (dropdown indicator)
  - `AllIconsKeys.Actions.Edit` (pencil icon)
  - `AllIconsKeys.General.Close` (X icon)

### Custom Components
- Create reusable components when needed (e.g., `DialogButton`)
- Document components with KDoc comments
- Follow Jewel's design style (rounded corners, proper spacing)
- Use theme colors for consistency with IDE
- Test on both light and dark themes