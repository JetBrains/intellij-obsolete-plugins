package com.intellij.dataWrangler.impl.view.transformation

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.llm.DWCommandAction
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.ide.util.treeView.PathElementIdProvider
import com.intellij.util.asSafely
import org.jetbrains.annotations.Nls
import java.util.Objects
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode

class TransformationsNode(userObject: TransformationsNodeDescriptor) : DefaultMutableTreeNode(userObject) {
  override fun getUserObject(): TransformationsNodeDescriptor? {
    return super.getUserObject().asSafely<TransformationsNodeDescriptor>()
  }

  sealed class TransformationsNodeDescriptor: PathElementIdProvider {
    abstract val children: List<TransformationsNodeDescriptor>
    abstract val displayName: @Nls String
    abstract val icon: Icon?

    override fun equals(other: Any?): Boolean {
      return other?.javaClass == javaClass && other is TransformationsNodeDescriptor &&
             children == other.children &&
             displayName == other.displayName &&
             icon == other.icon
    }

    override fun hashCode(): Int {
      return Objects.hash(children, displayName, icon)
    }

    override fun getPathElementId(): String = displayName
  }

  class RootNodeDescriptor(commandActions: List<DWCommandAction>, factories: List<CommandFactory<*, *>>) : TransformationsNodeDescriptor() {
    override var children: List<TransformationsNodeDescriptor> =
      commandActions.map { AIActionNodeDescriptor(it) } + createGroups(factories)
    override val displayName: @Nls String = ""
    override val icon: Icon? = null

    fun update(factories: List<CommandFactory<*, *>>): Boolean {
      val oldGroups = children.filterIsInstance<GroupNodeDescriptor>()
      val newGroups = createGroups(factories)
      if (oldGroups == newGroups) return false
      children = newGroups + children.filter { it !is GroupNodeDescriptor }
      return true
    }

    private fun createGroups(factories: List<CommandFactory<*, *>>): List<GroupNodeDescriptor> =
      filterNonEmptyGroups(factories, CommandFactoryGroup.entries).map { GroupNodeDescriptor(it, factories) }

    private fun filterNonEmptyGroups(factories: List<CommandFactory<*, *>>, groups: List<CommandFactoryGroup>): List<CommandFactoryGroup> {
      val groupSet = factories.mapTo(HashSet()) { it.getGroupName().displayName }
      return groups.filter { groupSet.contains(it.displayName) }
    }
  }

  class GroupNodeDescriptor(val group: CommandFactoryGroup, commands: List<CommandFactory<*, *>>) : TransformationsNodeDescriptor() {
    override val children: List<TransformationsNodeDescriptor> =
      commands.filter { it.getGroupName().displayName == group.displayName }.map { CommandNodeDescriptor(it) }

    override val displayName: @Nls String = group.displayName
    override val icon: Icon? = null
  }

  class CommandNodeDescriptor<C : DataWranglerContext>(val command: CommandFactory<*, C>) : TransformationsNodeDescriptor() {
    override val children: List<TransformationsNodeDescriptor> = emptyList()
    override val displayName: @Nls String = command.commandName
    override val icon: Icon? = null

    override fun equals(other: Any?): Boolean {
      return super.equals(other) && other is CommandNodeDescriptor<*> && command == other.command
    }

    override fun hashCode(): Int {
      return Objects.hash(super.hashCode(), command)
    }
  }

  class AIActionNodeDescriptor(val commandAction: DWCommandAction) : TransformationsNodeDescriptor() {
    override val children: List<TransformationsNodeDescriptor> = emptyList()
    override val displayName: @Nls String = commandAction.name
    override val icon: Icon = commandAction.icon
  }

}