// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.microservices.jvm.config.yaml.ConfigYamlKeyCompletionContributor

internal class HelidonYamlKeyCompletionContributor : ConfigYamlKeyCompletionContributor(
  HelidonYamlKeyCompletionProvider(), APPLICATION_YAML_CONDITION) {
  init {
    extendSequenceItem()
  }
}
