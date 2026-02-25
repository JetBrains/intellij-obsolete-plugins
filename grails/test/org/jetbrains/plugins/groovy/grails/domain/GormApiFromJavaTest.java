// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GormApiFromJavaTest extends Grails14TestCase {
  public void testCompletionStatic() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);

    PsiFile file = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          Ddd.<caret>
        }
      }
      """);

    checkCompletion(file, "create", "saveAll", "get");
    checkNonExistingCompletionVariants("createQueryMapForExample", "getId", "hasErrors", "setTransactionManager", "getExtendedMethods");
  }

  public void testCompletion() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);

    PsiFile file = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          new Ddd().<caret>
        }
      }
      """);

    checkCompletion(file, "refresh", "getName", "clearErrors", "hasErrors", "validate");
    checkNonExistingCompletionVariants("create", "getErrors", "filterErrors", "setTransactionManager", "getExtendedMethods");
  }

  public void testHighlighting() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);

    PsiFile file = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          Ddd.saveAll(new Ddd(), new Ddd(), new Ddd());
          Ddd.saveAll(new Ddd[]{new Ddd(), new Ddd(), new Ddd()});
      
          java.util.List list = Ddd.getAll();
      
          new Ddd().refresh();
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }
}
