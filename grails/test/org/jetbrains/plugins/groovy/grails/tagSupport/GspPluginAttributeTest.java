// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

import java.util.Map;

public class GspPluginAttributeTest extends HddGrailsTestCase {
  private void prepareData() {
    myFixture.addFileToProject("/plugins/some-plugin1-1.0b/_somePlugin1.gsp", "Template text");
    myFixture.addFileToProject("/plugins/some-plugin1-1.0b/SomePlugin1GrailsPlugin.groovy", "");

    myFixture.addFileToProject("/plugins/somePlugin2-1.0b/_somePlugin2.gsp", "Template text");
    myFixture.addFileToProject("/plugins/somePlugin2-1.0b/SomePlugin2GrailsPlugin.groovy", "");
    saveProperties("""
                     
                     plugins.some-plugin1=1.0b
                     plugins.some-plugin2=1.0b
                     """);

    GrailsTestUtil.createPluginXml(myFixture, "/plugins/some-plugin1-1.0b");
    GrailsTestUtil.createPluginXml(myFixture, "/plugins/somePlugin2-1.0b");

    GrailsTestUtil.createBuildConfig(myFixture, ".", Map.of("grails.project.plugins.dir", "plugins"));
  }

  public void testHighlighting() {
    prepareData();

    PsiFile file = myFixture.addFileToProject("grails-app/views/page.gsp", """
      
          <g:render template="/somePlugin1" plugin="some-plugin1" />
          <g:render template="/somePlugin1" plugin="somePlugin1" />
          <g:render template="/somePlugin2" plugin="some-plugin2" />
          <g:render template="/somePlugin2" plugin="somePlugin2" />
      
          <g:render template="/<error descr="Cannot resolve template '_somePlugin1.gsp'">somePlugin1</error>" plugin="<error descr="Cannot resolve symbol 'some-plugin1-1.0b'">some-plugin1-1.0b</error>" />
      
          <g:render template="/<error descr="Cannot resolve template '_somePlugin1.gsp'">somePlugin1</error>" plugin="somePlugin2" />
      """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCompletion() {
    prepareData();
    PsiFile page = myFixture.addFileToProject("grails-app/views/page.gsp", "<g:render plugin='<caret>' />");
    PsiFile tagLib = addTaglib("""
                                 
                                 class MyTagLib {
                                   def xxx = {
                                     render(plugin: '<caret>')
                                   }
                                 }
                                 """);

    checkCompletionVariants(page, "some-plugin1", "some-plugin2");
    checkCompletionVariants(tagLib, "some-plugin1", "some-plugin2");
  }
}
