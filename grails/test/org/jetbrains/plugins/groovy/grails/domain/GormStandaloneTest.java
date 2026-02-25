// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;

import java.io.IOException;

public class GormStandaloneTest extends LightJavaCodeInsightFixtureTestCase {
  private void createTestClasses() {
    myFixture.addFileToProject("City.groovy", """
      @grails.persistence.Entity
      class City {
      
        String name
      
        static hasMany = [street: Street];
      }
      """);

    myFixture.addFileToProject("Street.groovy", """
      
      @grails.persistence.Entity
      class Street {
        String name
      
        int index
      
        static hasMany = [houseNumbers: Integer]
      }
      """);
  }

  public void testAddToHighlighting() {
    createTestClasses();

    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = myFixture.addFileToProject("A.groovy", """
      
          def spb = new City(name: "Spb").save()
      
          spb.addToStreet(name: 'qqqRrr')
          spb.addToStreet(name: 12)
          spb.addToStreet(name: [1,2,3])
          spb.addToStreet(name: [:])
      
          spb.addToStreet(name: 'qqqRrr', index: <warning descr="Type of argument 'index' can not be 'String'">'Rrr'</warning>)
          spb.addToStreet(name: 'qqq12', index: 12)
          spb.addToStreet(name: 'qqq12', index: <warning descr="Type of argument 'index' can not be 'ArrayList<Integer>'">[1,2,3]</warning>)
      
          spb.addToStreet(name: 'qqq12', houseNumbers: [1,2,3,4,5])
          spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'LinkedHashMap<String, Integer>'">['asda':232]</warning>)
          spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'ArrayList<String>'">["1", '2']</warning>)
          spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'String'">'asdasdasa'</warning>)
          spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'Integer'">23</warning>)
      """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCompletion() {
    createTestClasses();

    PsiFile file = myFixture.addFileToProject("A.groovy", """
      
      def spb = new City(name: "Spb").save()
      spb.addToStreet(<caret>: )
         \s""");

    GrailsTestCase.checkCompletionStatic(myFixture, file, "name", "index", "houseNumbers");
  }

  public void testRenameParameter() {
    createTestClasses();

    PsiFile file = myFixture.addFileToProject("A.groovy", """
      
      def spb = new City(name: "Spb").save()
      
      spb.addToStreet(name<caret>: 'Nevskiy pr.')
      spb.addToStreet(name: 'Lanskoe sh.')
      
      Street st = new Street()
      println(st.name)
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.renameElementAtCaret("zzz");

    TestCase.assertEquals("""
                            
                            def spb = new City(name: "Spb").save()
                            
                            spb.addToStreet(zzz: 'Nevskiy pr.')
                            spb.addToStreet(zzz: 'Lanskoe sh.')
                            
                            Street st = new Street()
                            println(st.zzz)
                            """, file.getText());
  }

  public void testRenameMethod() {
    PsiFile file = myFixture.addFileToProject("City.groovy", """
      
      @grails.persistence.Entity
      class City {
      
          String name
          int peopleCount
          Set<String> street;
      
          static transients = ["sss", 'street']
      
          public String getSss<caret>() {
      
          }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.renameElementAtCaret("getS");

    myFixture.checkResult("""
                            
                            @grails.persistence.Entity
                            class City {
                            
                                String name
                                int peopleCount
                                Set<String> street;
                            
                                static transients = ["s", 'street']
                            
                                public String getS() {
                            
                                }
                            }
                            """);
  }

  @Override
  public final @NotNull LightProjectDescriptor getProjectDescriptor() {
    return projectDescriptor;
  }

  private final LightProjectDescriptor projectDescriptor = new DefaultLightProjectDescriptor() {
    @Override
    public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
      super.configureModule(module, model, contentEntry);

      PsiTestUtil.addLibrary(model, "Grails14", GrailsTestUtil.getMockGrails14LibraryHome(), "grails-core-2.0.0.M1.jar");
      try {
        GrailsTestUtil.createGrailsApplication(myFixture, module, contentEntry, ".", false);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  };
}
