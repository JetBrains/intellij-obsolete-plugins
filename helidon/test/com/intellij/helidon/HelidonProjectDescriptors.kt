// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon

import com.intellij.testFramework.LightProjectDescriptor

val HELIDON_PROJECT: LightProjectDescriptor = HelidonProjectDescriptorBuilder()
  .withConfig()
  .withSecurity()
  .withServer()
  .build()