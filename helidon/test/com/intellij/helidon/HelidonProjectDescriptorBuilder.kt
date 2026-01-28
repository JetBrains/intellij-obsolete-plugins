// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon

import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor

class HelidonProjectDescriptorBuilder : DefaultLightProjectDescriptor(IdeaTestUtil::getMockJdk18) {

  fun withConfig(): HelidonProjectDescriptorBuilder {
    withRepositoryLibrary(Helidon4SE.HELIDON_CONFIG)
    return this
  }

  fun withSecurity(): HelidonProjectDescriptorBuilder {
    withRepositoryLibrary(Helidon4SE.HELIDON_SECURITY)
    return this
  }

  fun withServer(): HelidonProjectDescriptorBuilder {
    withRepositoryLibrary(Helidon4SE.HELIDON_SERVER)
    return this
  }

  fun build(): LightProjectDescriptor {
    return this
  }
}