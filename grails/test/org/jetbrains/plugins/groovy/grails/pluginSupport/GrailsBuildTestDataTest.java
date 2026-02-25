// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.pluginSupport;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsBuildTestDataTest extends GrailsTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addFileToProject("src/java/grails/buildtestdata/mixin/Build.java", """
      package grails.buildtestdata.mixin;
      public @interface Build {
          Class<?>[] value();
      }
      """);
  }

  public void testResolveOneDomain() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);

    addDomain("""
                class Ddd { String name }
                """);
    addDomain("""
                class Ggg { String name }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeTest.groovy", """
      @grails.buildtestdata.mixin.Build(Ddd)
      class SomeTest {
        def test() {
          Ggg.<warning>build</warning>()
          Ddd.<warning>unresolvedMethod</warning>()
          def ddd = Ddd.build()
          def x = ddd.name
          def y = ddd.<warning>unresolvedProperty</warning>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testResolveDomainList() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);
    addDomain("""
                class Ddd { String name }
                """);
    addDomain("""
                class Ggg { String name }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeTest.groovy", """
      @grails.buildtestdata.mixin.Build([Ddd, Ggg])
      class SomeTest {
        def test() {
          Ggg.build()
          Ddd.build()
          Ddd.<warning>foo</warning>()
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testDomainPropertiesCompletion() {
    addDomain("""
                class Ddd {
                  String firstName
                  String secondName
                }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeTest.groovy", """
      @grails.buildtestdata.mixin.Build(Ddd)
      class SomeTest {
        def test() {
          Ddd.build(<caret>)
        }
      }
      """);

    checkCompletion(file, "firstName", "secondName");
  }
}
