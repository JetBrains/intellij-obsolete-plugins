// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.completion;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GrailsNamedAttributeTest extends LightJavaCodeInsightFixtureTestCase {
  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return GroovyProjectDescriptors.GROOVY_1_7;
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/completion/");
  }

  public void testGrailsNamedAttribute() {
    PsiFile taglib = myFixture.addFileToProject("grails-app/taglib/MyTagLib.groovy", """
      class MyTagLib {
        def customTag = {attr, body ->
          def param1 = attr.attr1
          attr.keySet().size()
          attr.keySet  \t().size()
          out << attr['attr2']
          out << attr  ['attr3']
      
          def param4 = attr.remove('attr4')
          System.out.println(attr.containsKey  ('attr5'))
      
          if ( attr.get('attr6')) {
            out << SomeClass.attr.someAttr
          }
        }
      }
      """);
    PsiTestUtil.addSourceRoot(myFixture.getModule(), taglib.getVirtualFile().getParent());

    try {
      myFixture.addFileToProject("grails-app/view/file.gsp", "<g:customTag <caret> />");

      myFixture.testCompletionVariants("grails-app/view/file.gsp", "attr1", "attr2", "attr3", "attr4", "attr5", "attr6");
    }
    finally {
      PsiTestUtil.removeSourceRoot(myFixture.getModule(), taglib.getVirtualFile().getParent());
    }
  }
}
