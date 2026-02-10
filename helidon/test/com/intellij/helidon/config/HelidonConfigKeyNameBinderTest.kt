// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKey.AccessType
import com.intellij.microservices.jvm.config.MetaConfigKeyManager

class HelidonConfigKeyNameBinderTest : HelidonMetaConfigKeyTestCase() {

  private lateinit var binder: MetaConfigKeyManager.ConfigKeyNameBinder

  override fun setUp() {
    super.setUp()
    binder = HelidonConfigKeyNameBinder
  }

  fun testMatches() {
    val key = createKey("my.my-fqn.property-name", null, AccessType.NORMAL)
    assertTrue(binder.bindsTo(key, "my.my-fqn.propertyName"))
  }

  fun testMatchesPrefix() {
    val key: MetaConfigKey = createKey("my.pre-fqn.property-name", null, AccessType.NORMAL)
    assertTrue(binder.matchesPrefix(key, "my.pre-fqn"))
    assertTrue(binder.matchesPrefix(key, "my.preFqn"))
    assertFalse(binder.matchesPrefix(key, "my.not"))
    assertFalse(binder.matchesPrefix(key, "my.fqn.too.long"))
    assertFalse(binder.matchesPrefix(key, "not.my.prefix"))
  }
}