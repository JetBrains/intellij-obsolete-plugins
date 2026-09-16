package com.jetbrains.spark.submit.run.cluster

import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import javax.swing.Icon

object LoadingRemoteTargetOption : SelectableRemoteTargetOption(SparkMessagesBundle.message("cluster.status.loading"), LOADING_ID) {
  override val type: RemoteTargetType? get() = null
  override fun setupItem(item: SimpleColoredComponent) {
    item.icon = AnimatedIcon.Default()
    item.append(name, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }
}

object NotSelectedRemoteTargetOption : SelectableRemoteTargetOption(SparkMessagesBundle.message("cluster.status.not.selected"), RemoteTargetId("remoteTarget.option.not.selected", null, SparkMessagesBundle.message("cluster.status.not.selected"))) {
  override val type: RemoteTargetType? get() = null
  override fun setupItem(item: SimpleColoredComponent) {
    item.icon = null
    item.append(name, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }
}

class UnresolvedRemoteTargetOption(name: String, id: RemoteTargetId) : SelectableRemoteTargetOption(name, id) {
  override val type: RemoteTargetType? get() = null
  override fun setupItem(item: SimpleColoredComponent) {
    item.append(name, SimpleTextAttributes.ERROR_ATTRIBUTES)
  }
}

sealed class SelectableRemoteTargetOption(name: String, id: RemoteTargetId) : RemoteTargetOption(name, id)

sealed class RemoteTargetOption(@NlsSafe val name: String, val id: RemoteTargetId) {
  open val icon: Icon? = null
  abstract val type: RemoteTargetType?

  companion object {
    val LOADING_ID = RemoteTargetId("remoteTarget.option.loading", null, SparkMessagesBundle.message("cluster.status.loading"))
  }

  open fun setupItem(item: SimpleColoredComponent) {
    item.icon = this@RemoteTargetOption.icon
    item.append(this@RemoteTargetOption.name)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RemoteTargetOption) return false

    if (name != other.name) return false
    return id == other.id
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + id.hashCode()
    return result
  }
}


