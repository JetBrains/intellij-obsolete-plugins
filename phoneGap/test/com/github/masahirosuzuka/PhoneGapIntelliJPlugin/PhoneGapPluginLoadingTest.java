// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.github.masahirosuzuka.PhoneGapIntelliJPlugin;

import com.github.masahirosuzuka.PhoneGapIntelliJPlugin.runner.PhoneGapConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.phoneGap.javascript.projectBuilder.PhoneGapTemplatesFactory;
import com.intellij.platform.ProjectTemplatesFactory;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.containers.ContainerUtil;

public class PhoneGapPluginLoadingTest extends BasePlatformTestCase {
  public void testPluginIsEnabled() {
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.github.masahirosuzuka.PhoneGapIntelliJPlugin"));
    assertNotNull(plugin);
    assertTrue(plugin.isEnabled());
  }

  public void testCoreModuleExtensionsAreRegistered() {
    assertNotNull(ConfigurationTypeUtil.findConfigurationType(PhoneGapConfigurationType.class));
  }

  public void testJavaScriptModuleExtensionsAreRegistered() {
    assertNotNull(ContainerUtil.findInstance(ProjectTemplatesFactory.EP_NAME.getExtensionList(), PhoneGapTemplatesFactory.class));
  }
}
