// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.compiler.options.CompileStepBeforeRunNoErrorCheck;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointersKt;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.testFramework.MapDataContext;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GrailsRunConfigurationProducerTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    PsiTestUtil.addLibrary(model, "JUnit", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/junit-4.8.1.jar");
  }

  private GrailsRunConfiguration getGrailsRunConfiguration(PsiElement element) {
    return getConfigurationsByElement(element).stream().filter(GrailsRunConfiguration.class::isInstance)
      .map(GrailsRunConfiguration.class::cast).findFirst().orElse(null);
  }

  private List<RunConfiguration> getConfigurationsByElement(PsiElement element) {
    try {
      SmartPsiElementPointer pointer = SmartPointersKt.createSmartPointer(element);
      MvcModuleStructureSynchronizer.forceUpdateProject(getProject());

      MapDataContext dataContext = new MapDataContext();

      dataContext.put(CommonDataKeys.PROJECT, getProject());
      dataContext.put(PlatformCoreDataKeys.MODULE, ModuleUtilCore.findModuleForPsiElement(pointer.getElement()));
      dataContext.put(Location.DATA_KEY, PsiLocation.fromPsiElement(pointer.getElement()));


      ConfigurationContext cc = ConfigurationContext.getFromContext(dataContext, ActionPlaces.UNKNOWN);

      Class<?> preferredProducerFindClass = Class.forName("com.intellij.execution.actions.PreferredProducerFind");
      Method method = ContainerUtil.find(preferredProducerFindClass.getMethods(), m -> "getConfigurationsFromContext".equals(m.getName()));
      method.setAccessible(true);
      @SuppressWarnings("unchecked") List<ConfigurationFromContext> confFromCtxList =
        (List<ConfigurationFromContext>)method.invoke(null, cc.getLocation(), cc, false, true);
      if (confFromCtxList == null) return List.of();

      List<RunConfiguration> res = new ArrayList<>();

      for (ConfigurationFromContext c : confFromCtxList) {
        RunConfiguration configuration = c.getConfiguration();

        if (configuration instanceof GrailsRunConfiguration) {
          RunManagerEx runManager = RunManagerEx.getInstanceEx(myFixture.getProject());
          for (BeforeRunTask task : runManager.getBeforeRunTasks(configuration, CompileStepBeforeRun.ID)) {
            TestCase.assertFalse(task.isEnabled());
          }


          List<CompileStepBeforeRunNoErrorCheck.MakeBeforeRunTaskNoErrorCheck> tasks2 =
            runManager.getBeforeRunTasks(configuration, CompileStepBeforeRunNoErrorCheck.ID);
          for (BeforeRunTask task : tasks2) {
            TestCase.assertFalse(task.isEnabled());
          }
        }
        res.add(configuration);
      }
      return res;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void doTest(String filePath, String fileContent, @Nullable String result) {
    PsiFile package1_T1Tests = myFixture.addFileToProject(filePath, fileContent);

    GrailsRunConfiguration c = getGrailsRunConfiguration(package1_T1Tests);
    if (result == null) {
      assertTrue(c == null || c.getProgramParameters() == null);
    }
    else {
      assertEquals(result, c.getProgramParameters());
    }
  }

  public void testJUnit3() {
    doTest("test/unit/package1/T1Tests.groovy", "package package1; class T1Tests extends junit.framework.TestCase {}",
           "test-app unit: package1.T1 -echoOut");
  }

  public void testJUnit3WithoutS() {
    doTest("test/unit/package1/T1Test.groovy", "package package1; class T1Test extends junit.framework.TestCase {}",
           "test-app unit: package1.T1 -echoOut");
  }

  public void testJUnitNonTest() {
    doTest("test/unit/package1/NoTestFile.groovy", "package package1; class NoTestFile extends junit.framework.TestCase {}", null);
  }

  public void testJUnitNonTest2() {
    doTest("test/unit/package1/NonATestTests.groovy", "package package1; class NonATestTests {}", null);
  }

  public void testJUnitNonTest3() {
    doTest("src/groovy/test/unit/package1/NonATestTests.groovy",
           "package test.unit.package1 class NonATestTests extends junit.framework.TestCase {}", null);
  }

  public void testJUnitNonTest4() {
    doTest("src/groovy/test/unit/package1/Tests.groovy", "package package1; class Tests extends junit.framework.TestCase {}", null);
  }

  public void testJavaClass() {
    doTest("test/unit/package1/T1Tests.java", "package package1; class T1Tests extends junit.framework.TestCase {}",
           "test-app unit: package1.T1 -echoOut");
  }

  public void testIntegration() {
    doTest("test/integration/package1/T1Tests.java", "package package1; class T1Tests extends junit.framework.TestCase {}",
           "test-app integration: package1.T1 -echoOut");
  }

  public void testIntegrationSpock1() throws Exception {
    addSimpleGroovyFile("package spock.lang; public class Specification { }");
    doTest("test/integration/package1/T1Tests.java", "package package1; class T1Tests extends spock.lang.Specification {}",
           "test-app integration: package1.T1 -echoOut");
  }

  public void testIntegrationSpock2() throws Exception {
    addSimpleGroovyFile("package spock.lang; public class Specification { }");
    doTest("test/integration/package1/T1Spec.java", "package package1; class T1Tests extends spock.lang.Specification {}",
           "test-app integration: package1.T1 -echoOut");
  }

  public void testIntegrationSpock3() throws Exception {
    addSimpleGroovyFile("package spock.lang; public class Specification { }");
    doTest("test/integration/package1/T1Specification.java", "package package1; class T1Tests extends spock.lang.Specification {}",
           "test-app integration: package1.T1 -echoOut");
  }

  public void testJUnitDirectory() {
    PsiFile file =
      myFixture.addFileToProject("test/unit/package1/T1Test.groovy", "package package1; class T1Test extends junit.framework.TestCase {}");

    GrailsRunConfiguration c = getGrailsRunConfiguration(file.getContainingDirectory());
    assertEquals("test-app unit: package1.** -echoOut", c.getProgramParameters());
  }

  public void testJUnitDirectory2() {
    PsiFile file =
      myFixture.addFileToProject("test/unit/package1/T1Test.groovy", "package package1; class T1Test extends junit.framework.TestCase {}");

    GrailsRunConfiguration c = getGrailsRunConfiguration(file.getContainingDirectory().getParentDirectory());
    assertEquals("test-app unit: -echoOut", c.getProgramParameters());
  }

  public void testNonTestDirectory() {
    PsiFile file = myFixture.addFileToProject("test/unit/package1/T1Test.groovy",
                                              "package package1; class T1Test extends junit.framework.TestCase {}");

    GrailsRunConfiguration c = getGrailsRunConfiguration(file.getContainingDirectory().getParentDirectory().getParentDirectory());
    TestCase.assertNull(c);
  }

  public void testIntegrationDirectory() {
    PsiFile file = myFixture.addFileToProject("test/integration/package1/T1Test.groovy",
                                              "package package1; class T1Test extends junit.framework.TestCase {}");

    GrailsRunConfiguration c = getGrailsRunConfiguration(file.getContainingDirectory());
    assertEquals("test-app integration: package1.** -echoOut", c.getProgramParameters());
  }

  public void testTestMethodGroovy() {
    PsiFile file = myFixture.addFileToProject("test/integration/package1/T1Test.groovy", """
      package package1;
      class T1Test extends junit.framework.TestCase {
        public void testXxx() {}
        public void testYyy() {}
      }
      """);
    PsiMethod method = (((GroovyFile)file).getTypeDefinitions()[0]).findMethodsByName("testXxx", false)[0];

    GrailsRunConfiguration c = getGrailsRunConfiguration(method);
    assertEquals("test-app integration: package1.T1.testXxx -echoOut", c.getProgramParameters());
  }

  public void testTestMethodJava() {
    PsiFile file = myFixture.addFileToProject("test/integration/package1/T1Test.java", """
      package package1;
      public class T1Test extends junit.framework.TestCase {
        public void testXxx() {}
        public void testYyy() {}
      }
      """);
    PsiMethod method = (((PsiJavaFile)file).getClasses()[0]).findMethodsByName("testXxx", false)[0];

    GrailsRunConfiguration c = getGrailsRunConfiguration(method);
    assertEquals("test-app integration: package1.T1.testXxx -echoOut", c.getProgramParameters());
  }

  public void testTestMethodJUnit4() {
    PsiFile file = myFixture.addFileToProject("test/integration/package1/T1Test.java", """
      package package1;
      public class T1Test {
        @org.junit.Test
        public void xxx() {}
      }
      """);
    PsiMethod method = (((PsiJavaFile)file).getClasses()[0]).findMethodsByName("xxx", false)[0];

    GrailsRunConfiguration c = getGrailsRunConfiguration(method);
    assertEquals("test-app integration: package1.T1.xxx -echoOut", c.getProgramParameters());
  }

  public void testJUnit4Class() {
    doTest("test/integration/package1/T1Test.java", """
      package package1;
      public class T1Test {
        @org.junit.Test
        public void xxx() {}
      }
      """, "test-app integration: package1.T1 -echoOut");
  }

  public void testTestMixinAnnotationClass() {
    doTest("test/integration/package1/T1Test.java", """
      package package1;
      
      @grails.test.mixin.TestMixin(grails.test.mixin.support.GrailsUnitTestMixin)
      public class T1Test {
      
        public void testFoo() {}
      }
      """, "test-app integration: package1.T1 -echoOut");
  }

  public void testNonTestMethod() {
    PsiFile file = myFixture.addFileToProject("test/integration/package1/T1Test.java", """
      package package1;
      public class T1Test extends junit.framework.TestCase {
        public void xxx() {}
      }
      """);
    PsiMethod method = (((PsiJavaFile)file).getClasses()[0]).findMethodsByName("xxx", false)[0];

    GrailsRunConfiguration c = getGrailsRunConfiguration(method);
    assertEquals("test-app integration: package1.T1 -echoOut", c.getProgramParameters());
  }

  @Override
  protected boolean needJUnit() {
    return true;
  }

  public void testNameOfMethodContainsSpace() throws Exception {
    addSimpleGroovyFile("""
                          package spock.lang;
                          
                          import org.junit.runner.RunWith;
                          
                          @RunWith(Sputnik.class)
                          public class Specification {
                          }
                          """);

    PsiFile file = myFixture.addFileToProject("test/integration/package1/T1Spec.groovy", """
      package package1;
      class T1Spec extends spock.lang.Specification {
        void "test something"() {
            expect:
            name.size() == length
      
            where:
            name     | length
            "Spock"  | 5
            "Kirk"   | 4
        }
      }
      """);

    PsiMethod method = (((GroovyFile)file).getClasses()[0]).findMethodsByName("test something", false)[0];

    GrailsRunConfiguration c = getGrailsRunConfiguration(method);
    assertEquals("test-app integration: \"package1.T1Spec.test something\" -echoOut", c.getProgramParameters());
  }
}
