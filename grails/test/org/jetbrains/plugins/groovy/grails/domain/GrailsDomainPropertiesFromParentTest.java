// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsDomainPropertiesFromParentTest extends GrailsTestCase {
  public void testCompletion() {
    myFixture.addFileToProject("src/groovy/Parent.groovy", """
      
      class Parent {
        String name;
      
        static hasMany = [many:String];
        static hasOne = [one:String];
      }
      """);

    addDomain("""
                
                class Ddd extends Parent {
                
                }
                """);

    PsiFile file = myFixture.addFileToProject("src/groovy/T.groovy", "new Ddd().<caret>");

    checkCompletion(file, "name", "one", "many");
  }
}
