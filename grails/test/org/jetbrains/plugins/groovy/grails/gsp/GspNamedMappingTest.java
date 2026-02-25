// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspNamedMappingTest extends GrailsTestCase {
  private void addMappingFile() {
    myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
      
        static mappings = {
          name mapping111: "/m1/$param1/${param2}" {
            controller = 'ccc'
            action = 'zzz'
          }
      
          name mappingWithOptionalParams: "/m1/$param1?/${param2}?" {
            controller = 'ccc'
            action = 'zzz'
          }
      
          name (mapping222: "/m1/dsddsad" {
            controller = 'ccc'
            action = 'zzz'
          })
      
          "/"(view: "/index")
          "500"(view: '/error')
        }
      }
      """);
  }

  public void testHighlighting() {
    addMappingFile();
    PsiFile file = myFixture.addFileToProject("grails-app/views/g.gsp", """
      <link:mapping111 param1="asd" param2="asdasd" zzz="sda">Link</link:mapping111>
      <link:mapping222>L</link:mapping222>
      <link:mappingWithOptionalParams>L</link:mappingWithOptionalParams>
      <link:mappingWithOptionalParams param1="1" param2="2">LLL</link:mappingWithOptionalParams>
      
      <<error descr="Element link:noMapping is not allowed here">link:noMapping</error>>Link</<error descr="Element link:noMapping is not allowed here">link:noMapping</error>>
      
      <g:link mapping="mapping222">Link</g:link>
      <g:link mapping="<error descr="Cannot resolve symbol 'NoMapping'">NoMapping</error>">Link</g:link>
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting();
  }

  public void testAttributeCompletion() {
    addMappingFile();
    PsiFile file = myFixture.addFileToProject("grails-app/views/g.gsp", "<link:mapping111 <caret>");
    checkCompletionVariants(file, "attrs", "param1", "param2");
  }
}
