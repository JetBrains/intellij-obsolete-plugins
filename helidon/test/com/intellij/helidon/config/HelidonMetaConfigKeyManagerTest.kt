// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.helidon.HelidonHighlightingTestCase

class HelidonMetaConfigKeyManagerTest : HelidonHighlightingTestCase() {

  fun testFindCanonicalApplicationMetaConfigKey() {
    val key = HelidonMetaConfigKeyManager.getInstance().findCanonicalApplicationMetaConfigKey(module, "server.host")
    assertNotNull(key)

    val keyWithNonCanonicalName = HelidonMetaConfigKeyManager.getInstance().findCanonicalApplicationMetaConfigKey(module, "Server.host")
    assertNull(keyWithNonCanonicalName)
  }
}