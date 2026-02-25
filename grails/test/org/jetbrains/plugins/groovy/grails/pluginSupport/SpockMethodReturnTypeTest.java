// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.pluginSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;

public class SpockMethodReturnTypeTest extends GrailsTestCase {
  @Override
  protected void configureGrails(@NotNull Module module, @NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    super.configureGrails(module, model, contentEntry);
    PsiTestUtil.addLibrary(model, "Spoc", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/spock-grails-support-0.5-groovy-1.7.jar");
  }

  public void testControllerTestCompletion() {
    addController("class CccController { def zzz = {} }");

    PsiFile file = myFixture.addFileToProject("test/unit/CccControllerSpec.groovy", """
      class CccControllerSpec extends grails.plugin.spock.ControllerSpec {
        def "test"() {
          controllerClass.newInstance().<caret>
        }
      }
      """);

    checkCompletion(file, "zzz", "session", "request");
  }

  public void testTagLibTestCompletion() {
    addTaglib("class MyTagLib { def zzz = {} }");

    PsiFile file = myFixture.addFileToProject("test/unit/MyTagLibSpec.groovy", """
      class MyTagLibSpec extends grails.plugin.spock.TagLibSpec {
        def "test"() {
          tagLibClass.newInstance().<caret>
        }
      }
      """);

    checkCompletion(file, "zzz");
  }
}
