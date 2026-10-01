// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.tags

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls

@ApiStatus.Internal
open class HfTag(
  val id: String,
  @param:Nls val label: String,
  val type: String,
  open val subType: String? = null
)
