// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.GrailsPluginFieldCompletionProvider;

public class GrailsPluginClassTest extends GrailsTestCase {
  public void testUniqueCompletionVariants() {
    TestCase.assertEquals(GrailsPluginFieldCompletionProvider.VARIANTS.length,
                          ContainerUtil.newHashSet(GrailsPluginFieldCompletionProvider.VARIANTS).size());
  }

  public void testCompletion() {
    myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        def doWith<caret>
      }
      """);
    myFixture.testCompletionVariants("XxxGrailsPlugin.groovy", "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testCompletion1() {
    PsiFile file = myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        def <caret>
      }
      """);
    checkCompletion(file, "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testCompletionStatic() {
    myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        static def doWith<caret>
      }
      """);
    myFixture.testCompletionVariants("XxxGrailsPlugin.groovy", "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testCompletionStatic1() {
    PsiFile file = myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        static def <caret>
      }
      """);
    checkCompletion(file, "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testRenamePluginExcludes() {
    myFixture.addFileToProject("scripts/ppp/aaa.groovy", "");

    PsiFile file = myFixture.addFileToProject("CccGrailsPlugin.groovy", """
      class CccGrailsPlugin {
        def pluginExcludes = ['scripts/ppp/**', 'scripts/ppp/aaa.groovy']
      }
      """);

    myFixture.moveFile("scripts/ppp/aaa.groovy", "scripts");

    TestCase.assertEquals("""
                            class CccGrailsPlugin {
                              def pluginExcludes = ['scripts/ppp/**', 'scripts/aaa.groovy']
                            }
                            """, file.getText());
  }

  public void testCompletionObserveValues() {
    myFixture.addFileToProject("Aaa1GrailsPlugin.groovy", "class Aaa1GrailsPlugin {}");
    myFixture.addFileToProject("Aaa2GrailsPlugin.groovy", "class Aaa2GrailsPlugin {}");
    myFixture.addFileToProject("Aaa3GrailsPlugin.groovy", "class Aaa3GrailsPlugin {}");
    myFixture.addFileToProject("Aaa0GrailsPlugin.groovy", """
      class Aaa2GrailsPlugin {
          def observe = ['aaa1', 'aaa<caret>']
      }
      """);

    myFixture.testCompletionVariants("Aaa0GrailsPlugin.groovy", "aaa2", "aaa3");
  }

  public void testClosureArgumentTypes() {
    PsiFile file = myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
          def onChange = {event ->
              event.containsKey(null)
              event.dfsdkfsdjk()
          }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }
}
