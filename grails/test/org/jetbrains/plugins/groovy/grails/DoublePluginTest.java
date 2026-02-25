// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.plugins.grails.config.GrailsFramework;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;

public class DoublePluginTest extends HddGrailsTestCase {
  public void testDoublePlugin() throws IOException {
    LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
    map.put("grails.project.plugins.dir", "./myplugins");
    GrailsTestUtil.createBuildConfig(myFixture, ".", map);

    GrailsTestUtil.createGrailsApplication(myFixture, "./plugins/pluginA-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./plugins/pluginA-1.0");

    GrailsTestUtil.createGrailsApplication(myFixture, "./plugins/pluginB-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./plugins/pluginB-1.0");

    GrailsTestUtil.createGrailsApplication(myFixture, "./myplugins/pluginA-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./myplugins/pluginA-1.0");

    GrailsTestUtil.createGrailsApplication(myFixture, "./myplugins/pluginC-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./myplugins/pluginC-1.0");

    Collection<VirtualFile> plugins = GrailsFramework.getInstance().getAllPluginRoots(myFixture.getModule(), true);

    UsefulTestCase.assertSize(3, plugins);
  }
}
