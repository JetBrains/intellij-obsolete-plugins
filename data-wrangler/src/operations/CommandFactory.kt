package com.intellij.dataWrangler.operations

import com.intellij.dataWrangler.DataWranglerBundle
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

enum class CommandFactoryGroup(private val displayNamePtr: Supplier<@Nls String>) {
  FIND_AND_REPLACE(DataWranglerBundle.messagePointer("dw.group.name.find")),
  SORT_AND_FILTER(DataWranglerBundle.messagePointer("dw.group.name.filter")),
  DROP(DataWranglerBundle.messagePointer("dw.group.name.drop")),
  ADD(DataWranglerBundle.messagePointer("dw.group.name.create")),
  NORMALIZATION_AND_SCALING(DataWranglerBundle.messagePointer("dw.group.name.normalizing")),
  HANDLING_OUTLIERS_AND_SKEWED(DataWranglerBundle.messagePointer("dw.group.name.handlingOutliersAndSkewed")),
  OTHERS(DataWranglerBundle.messagePointer("dw.group.name.other")),
  CUSTOM(DataWranglerBundle.messagePointer("dw.group.name.custom"));

  val displayName: @NlsSafe String get() = displayNamePtr.get()
}

/**
 * Interface for command factory that responsible for the parameters panel of exact command.
 *
 * P: Any is a data class containing parameters for a command,
 * where fields that need to be modified by the user should be declared as var.
 */
interface CommandFactory<P : Any, C : DataWranglerContext> {
  /**
   * Command unique identifier to refer to commands internally
   */
  val id: String get() = this::class.java.name
  /**
   * User-visible command name in the drop-down menu when the user selects it
   */
  val commandName: @Nls String

  /**
   * Build meta structure over data class for parameters panel generation
   */
  val parametersMetaType: MetaStruct<P>

  /**
   * Set default parameters based on runtime information (for an example columns names that could be extracted from the context)
   */
  fun initDefaultParameters(context: C, defaultParams: P) {}

  /**
   * Create DataWranglerCommand with the final state of the parameters
   */
  fun createCommand(parameters: P): DataWranglerCommand<C>

  /**
   * Group name to split operations into groups
   */
  fun getGroupName(): CommandFactoryGroup = CommandFactoryGroup.OTHERS

  @Nls
  fun getDescription(): String? = null
}