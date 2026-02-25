// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.config;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class GrailsConfigSelectWordTest extends LightJavaCodeInsightFixtureTestCase {
  private void doTest(String src, String... variants) {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BuildConfig.groovy", src);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    for (String v : variants) {
      myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET);
      myFixture.checkResult(v);
    }

    for (int i = variants.length - 2; i >= 0; i--) {
      myFixture.performEditorAction("EditorUnSelectWord");
      myFixture.checkResult(variants[i]);
    }
  }

  public void testLiteralCenter() {
    doTest("""
             grails.project.dependency.resolution = {
               dependencies {
                 runtime 'mysql:mysql-conne<caret>ctor-java:5.1.16'
               }
             }
             """,
             """
             grails.project.dependency.resolution = {
               dependencies {
                 runtime 'mysql:mysql-<selection>connector</selection>-java:5.1.16'
               }
             }
             """,
           """
             grails.project.dependency.resolution = {
               dependencies {
                 runtime 'mysql:<selection>mysql-connector-java</selection>:5.1.16'
               }
             }
             """,
           """
             grails.project.dependency.resolution = {
               dependencies {
                 runtime '<selection>mysql:mysql-connector-java:5.1.16</selection>'
               }
             }
             """);
  }

  public void testGString() {
    doTest("""
             
             grails.project.dependency.resolution = {
               dependencies {
                 runtime 'mysql:mysql-conne<caret>ctor-java:$version'
               }
             }
             """, """
             
             grails.project.dependency.resolution = {
               dependencies {
                 runtime 'mysql:mysql-<selection>connector</selection>-java:$version'
               }
             }
             """, """
             
             grails.project.dependency.resolution = {
               dependencies {
                 runtime 'mysql:<selection>mysql-connector-java</selection>:$version'
               }
             }
             """);
  }

  public void testLiteralEndPlugin() {
    doTest("""
             grails.project.dependency.resolution = {
               plugins {
                 runtime ":resources:1.<caret>1.5"
               }
             }
             """,
           """
             grails.project.dependency.resolution = {
               plugins {
                 runtime ":resources:1.<selection>1</selection>.5"
               }
             }
             """,
           """
             grails.project.dependency.resolution = {
               plugins {
                 runtime ":resources:<selection>1.1.5</selection>"
               }
             }
             """,
           """
             grails.project.dependency.resolution = {
               plugins {
                 runtime "<selection>:resources:1.1.5</selection>"
               }
             }
             """);
  }
}
