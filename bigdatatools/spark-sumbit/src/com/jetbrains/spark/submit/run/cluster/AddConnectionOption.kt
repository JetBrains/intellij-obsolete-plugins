package com.jetbrains.spark.submit.run.cluster

import com.intellij.icons.AllIcons
import com.intellij.util.concurrency.annotations.RequiresEdt

class AddConnectionOption(
  name: String,
  connGroupId: String,
  override val type: RemoteTargetType,
  @RequiresEdt val onClick: () -> Boolean
) : RemoteTargetOption(name, RemoteTargetId(connGroupId, null, name)) {
  override val icon = AllIcons.General.Add
}