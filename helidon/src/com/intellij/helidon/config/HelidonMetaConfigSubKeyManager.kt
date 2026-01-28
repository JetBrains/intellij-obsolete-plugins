// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.MetaConfigKeyManager
import com.intellij.openapi.module.Module

internal class HelidonMetaConfigSubKeyManager(private val parent: HelidonMetaConfigKey) : MetaConfigKeyManager() {

  override fun getAllMetaConfigKeys(module: Module?): List<MetaConfigKey> = parent.subKeys

  override fun getConfigKeyNameBinder(module: Module): ConfigKeyNameBinder = HelidonMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(module)
}