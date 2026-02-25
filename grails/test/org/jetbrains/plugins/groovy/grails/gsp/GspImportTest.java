// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspImportTest extends LightJavaCodeInsightFixtureTestCase {
  public void testImport() {
    myFixture.addFileToProject("aaa/aaa/ClassA1.java", "package aaa.aaa;\n public class ClassA1 {}");
    myFixture.addFileToProject("aaa/aaa/ClassA2.groovy", "package aaa.aaa;\n class ClassA2 {}");
    myFixture.addFileToProject("bbb/ClassB1.java", "package bbb;\n public class ClassB1 {}");
    myFixture.addFileToProject("bbb/ClassB2.groovy", "package bbb;\n class ClassB2 {}");

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page import="aaa.aaa.ClassA2 as A2; bbb.*; aaa.aaa.ClassA1" %>
      <%
          def a = ClassA1
          def a2 = ClassA2
          def a2_ = A2
          def b = ClassB2
          def b2 = ClassB2
      %>
      """);

    GrailsTestCase.checkResolve(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "ClassA2");
  }

  public void testOverrideImport() {
    myFixture.addFileToProject("aaa/aaa/ClassA1.java", "package aaa.aaa;\n public class ClassA1 {}");
    myFixture.addFileToProject("aaa/aaa/A2.java", "package aaa.aaa;\n public class A2 { public static int A2; }");
    myFixture.addFileToProject("aaa/aaa/ClassA2.java", "package aaa.aaa;\n public class ClassA2 {}");

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page import="aaa.aaa.ClassA2 as A2; aaa.aaa.A2; aaa.aaa.ClassA1 as A2; static aaa.aaa.A2.A2 as A2 " %>
      <%
          def fff = A2
      %>
      """);

    GrailsTestCase.checkTypeOfVariable(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "fff", "Class<ClassA1>");
  }

  public void testStaticImportMethod() {
    myFixture.addFileToProject("aaa/aaa/A2.java", """
      package aaa.aaa;
      public class A2 {
        public static String A2 = "";
      
        public static int A2() {
          return 5555;
        }
      }
      """);

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page import="static aaa.aaa.A2.A2 as A2" %>
      <%
          def fff = A2()
      %>
      """);

    GrailsTestCase.checkTypeOfVariable(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "fff", "int");
  }

  public void testStaticImportField() {
    myFixture.addFileToProject("aaa/aaa/A2.java", """
      package aaa.aaa;
      public class A2 {
        public static String A2 = "";
      
        public static int A2() {
          return 5555;
        }
      }
      """);

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page import="static aaa.aaa.A2.A2 as A2" %>
      <%
          def fff = A2
      %>
      """);

    GrailsTestCase.checkTypeOfVariable(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "fff", "String");
  }

  public void testStaticImportMany() {
    myFixture.addFileToProject("aaa/aaa/A2.java", """
      package aaa.aaa;
      public class A2 {
        public static String A2 = "";
      
        public static int A2() {
          return 5555;
        }
      }
      """);

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page import="static aaa.aaa.A2.*" %>
      <%
          def fff1 = A2
          def fff2 = A2()
      %>
      """);

    GrailsTestCase.checkTypeOfVariable(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "fff1", "String");
    GrailsTestCase.checkTypeOfVariable(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "fff2", "int");
  }
}
