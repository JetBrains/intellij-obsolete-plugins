package com.intellij.aidebugger.common.viewModels


/**
 * A ViewModel class that represents a textual content for display purposes.
 *
 * @property content The string content to be displayed in the associated view.
 */
data class InputContentViewVM(val content: String) : ViewModelBase

/**
 * A ViewModel class that represents a structured content for display purposes.
 *
 * @property content The structured content to be displayed in the associated view.
 */
data class InputStructuredContentViewVM(val content: Any) : ViewModelBase

/**
 * A ViewModel class that represents a textual content for display purposes.
 *
 * @property content The string content to be displayed in the associated view.
 */
data class OutputContentViewVM(val content: String) : ViewModelBase

/**
 * A ViewModel class that represents a structured content for display purposes.
 *
 * @property content The structured content to be displayed in the associated view.
 */
data class OutputStructuredContentViewVM(val content: Any) : ViewModelBase

/**
 * Represents a view model that contains content data and a list of tool calls.
 *
 * This class is used to combine a content string, such as a user-generated
 * input or system-provided text, with a list of `ToolCallVM` instances, which
 * encapsulate tool invocation details. It inherits from the `ViewModelBase`
 * interface to align with the application's view model structure.
 *
 * @property toolCalls A list of `ToolCallVM` objects representing individual tool
 *                     invocations along with their respective details, such as
 *                     tool names and their arguments.
 */
data class ViewWithToolCallsVM(
    val toolCalls: List<ToolCallVM>
) : ViewModelBase

/**
 * Represents the view model for a tool call, encapsulating the tool's name
 * and its associated arguments.
 *
 * This data model is intended to organize and represent a tool invocation
 * within a system, where each tool is associated with one or more arguments.
 *
 * @property name The identifier or name of the tool being called.
 * @property arguments A collection of arguments specified for the tool,
 *                     represented as a list of ToolArgumentVM instances.
 */
data class ToolCallVM(
    val name: String,
    val arguments: List<ToolArgumentVM>
) : ViewModelBase

/**
 * Represents a data model for tool arguments, defining a pair of name and value.
 *
 * This model is typically used to handle arguments or properties used within
 * a system, identified by a name and assigned a specific string value.
 *
 * @property name The name of the argument, typically identifying the property.
 * @property value The value assigned to the argument, represented as a string.
 */
data class ToolArgumentVM(
    val name: String,
    val value: String,
)
