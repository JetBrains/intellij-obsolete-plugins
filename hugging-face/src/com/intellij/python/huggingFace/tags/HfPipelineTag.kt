// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.tags

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls

@ApiStatus.Internal
class HfPipelineTag(
  id: String,
  @Nls label: String,
  override val subType: String  // is mandatory here
): HfTag(id, label, type = PIPELINE_TAG) {
  companion object {
    const val PIPELINE_TAG: String = "pipeline_tag"
  }
}
