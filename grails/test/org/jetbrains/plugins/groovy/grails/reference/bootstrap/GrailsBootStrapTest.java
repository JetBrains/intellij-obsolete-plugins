// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.bootstrap;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsBootStrapTest extends Grails14TestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PsiTestUtil.addSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/conf"));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      PsiTestUtil.removeSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/conf"));
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testCompletion() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            <caret>
          }
      }
      """);
    checkCompletion(file, "environments");
  }

  public void testCompletion2() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            environments {
              <caret>
            }
          }
      }
      """);
    checkCompletion(file);
    checkNonExistingCompletionVariants("environments");
  }

  public void testCompletionEnvName() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            environments({
              <caret>
            })
          }
      }
      """);
    checkCompletion(file, "test", "development", "production");
  }

  public void testCompletionEnvNameNotCompleted() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            environments({
              if (1 == 2) {
                <caret>
              }
            })
          }
      }
      """);
    checkCompletion(file);
    checkNonExistingCompletionVariants("test", "development", "production");
  }

  public void testResolve() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      class BootStrap {
          def init = { servletContext ->
            String.<warning>environments</warning> {
              <warning>test</warning> {
              }
      
            }
      
            environments {
              if (1 == 2) {
                <warning>test</warning> {
                }
              }
      
              development({
              })
      
              production {
      
              }
      
              <warning>environments</warning> {
              }
            }
      
            environments ({
            });
          }
      
          def foo() {
           <warning>environments</warning> {
           }
          }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testLogVariable() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      class BootStrap {
        def init = { servletContext ->
          <caret>
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    checkCompletion("log");
  }
}
