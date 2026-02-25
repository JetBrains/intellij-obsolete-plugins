// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.spring;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.facet.SpringFacet;
import com.intellij.spring.model.utils.SpringCommonUtils;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

import java.util.List;

public class GrailsSpringIntegrationTest extends HddGrailsTestCase {
  public void testGetBeanCompletion() {
    assertNotNull(SpringFacet.getInstance(getModule()));

    myFixture.addFileToProject("grails-app/services/MyService.groovy", "class MyService {}");

    myFixture.addFileToProject("grails-app/views/index.gsp", "<% applicationContext.getBean(\"<caret>\") %>");

    myFixture.testCompletionVariants("grails-app/views/index.gsp", "grailsConfigurator", "myService", "pluginManager",
                                     "grailsResourceHolder", "conversionService");
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder moduleBuilder) {
    super.tuneFixture(moduleBuilder);
    moduleBuilder.addLibraryJars("Spring", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-spring-1.3.1.jar",
                                 "/lib/org.springframework.beans-3.0.3.RELEASE.jar", "/lib/org.springframework.context-3.0.3.RELEASE.jar");
    moduleBuilder.addLibraryJars("org.springframework:spring-core:3.0.3.RELEASE", GrailsTestUtil.getMockGrails11LibraryHome(),
                                 "/lib/spring-core-3.0.3.RELEASE.jar");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addFileToProject("web-app/WEB-INF/applicationContext.xml", """
      
      <?xml version="1.0" encoding="UTF-8"?>
      <beans xmlns="http://www.springframework.org/schema/beans"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="
      http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd">
      
        <bean id="pluginManager" class="org.codehaus.groovy.grails.plugins.GrailsPluginManagerFactoryBean">
          <description>A bean that manages Grails plugins</description>
              <property name="grailsDescriptor" value="/WEB-INF/grails.xml" />
              <property name="application" ref="grailsApplication" />
        </bean>
      
          <bean id="grailsConfigurator" class="org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator">
              <constructor-arg>
                  <ref bean="grailsApplication" />
              </constructor-arg>
              <property name="pluginManager" ref="pluginManager" />
          </bean>
      
          <bean id="grailsResourceHolder" scope="prototype" class="org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder">
              <property name="resources">
                    <value>classpath*:**/grails-app/**/*.groovy</value>
              </property>
          </bean>
      </beans>
      """);

    // todo remove this
    String path = GrailsTestUtil.getMockGrails11LibraryHome() +
                  "/dist/grails-core-1.3.1.jar!/org/codehaus/groovy/grails/plugins/GrailsPluginManager.class";
    final VirtualFile path1 = JarFileSystem.getInstance().findFileByPath(path);
    (path1 == null ? null : path1.getParent()).refresh(false, true);

    setupFacets();
  }

  public void testBeanInjection() {
    PsiFile serviceFile = myFixture.addFileToProject("grails-app/services/MyService.groovy", """
      class MyService {
          def someService
          def xxx() {
            someService.yyy()
          }
      }
      """);

    PsiFile serviceFile2 = myFixture.addFileToProject("grails-app/services/SomeService.groovy", """
      class SomeService {
        def myService = "";
        def yyy() {
          myService.xxx()
        }
      }
      """);
    HddGrailsTestCase.checkResolve(serviceFile);
    HddGrailsTestCase.checkResolve(serviceFile2, "xxx");

    PsiFile controllerFile = addController("""
                                             class CccController {
                                               def myService
                                               {
                                                 myService.xxx() + myService.yyy()
                                               }
                                             }
                                             """);
    HddGrailsTestCase.checkResolve(controllerFile, "yyy");

    PsiFile nonArtifactFile = myFixture.addFileToProject("grails-app/controllers/Ccc.groovy", """
      class Ccc {
        def myService
        {
          myService.xxx()
        }
      }
      """);
    HddGrailsTestCase.checkResolve(nonArtifactFile, "xxx");

    PsiFile bootstrapFile = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      class BootStrap {
        def myService
        {
          myService.xxx() + myService.yyy()
        }
      }
      """);
    HddGrailsTestCase.checkResolve(bootstrapFile, "yyy");

    PsiFile testFile = myFixture.addFileToProject("test/unit/XxxTest.groovy", """
      import grails.test.*;
      
      class XxxTest extends GrailsUnitTestCase {
        def myService
        {
          myService.xxx() + myService.yyy()
        }
      }
      """);
    HddGrailsTestCase.checkResolve(testFile, "yyy");
  }

  public void testSpringInjectionAnnotator1() {
    PsiFile controller = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def grailsConfigurator
        org.codehaus.groovy.grails.plugins.GrailsPluginManager pluginManager
        String grailsResourceHolder
        org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder fieldNameNotEqualsBeanName
      }
      """);
    myFixture.configureFromExistingVirtualFile(controller.getVirtualFile());
    myFixture.checkHighlighting();

    List<GutterMark> gutters = myFixture.findAllGutters();
    assertSize(2, ContainerUtil.filter(gutters, gutter -> gutter.getIcon().equals(SpringApiIcons.Gutter.ShowAutowiredDependencies)));
  }

  public void testSpringInjectionAnnotator2() {
    PsiFile file = myFixture.addFileToProject("grails-app/controllers/NotAController_.groovy", """
      
      class NotAController_ {
        def grailsConfigurator
        org.springframework.beans.factory.FactoryBean pluginManager
        String grailsResourceHolder
        org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder fieldNameNotEqualsBeanName
      }
      """);

    checkGutters(file);
  }

  public void testInjectionCompletion1() {
    myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      
      class CccController {
        def grailsConfigurator
        def <caret>
        org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager pluginManager
      }
      """);

    List<String> variants = myFixture.getCompletionVariants("grails-app/controllers/CccController.groovy");
    assertTrue(variants.contains("grailsResourceHolder"));
    assertFalse(variants.contains("grailsConfigurator"));
    assertFalse(variants.contains("pluginManager"));
  }

  public void testInjectionCompletion2() {
    myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def grailsConfigu<caret>rator
        org.springframework.beans.factory.FactoryBean pluginManager
      }
      """);

    List<String> variants = myFixture.getCompletionVariants("grails-app/controllers/CccController.groovy");
    assertEquals(List.of("grailsConfigurator"), variants);
  }

  public void testInjectionCommand() {
    PsiFile file = myFixture.addFileToProject("grails-app/controllers/ZzzCommand.groovy", """
      class ZzzCommand {
        def <caret>
      }
      """);

    checkCompletion(file, "pluginManager", "grailsConfigurator");
  }

  public void testContextGetBeanType() {
    myFixture.addFileToProject("src/groovy/GrUtil.groovy", "class GrUtil { public static Object getBean(String s) { 'a' } }");
    PsiFile gspFile = myFixture.addFileToProject("grails-app/views/g.gsp", """
      ${applicationContext.getBean('grailsConfigurator').configureDomainOnly()}
      ${applicationContext.getBean('grailsConfigurator', ['a', 'b']).configureDomainOnly()}
      ${applicationContext.getBean('grailsConfigurator', 'a', 'b').configureDomainOnly()}
      ${GrUtil.getBean('grailsConfigurator').xxx}
      """);

    PsiFile groovyFile = gspFile.getViewProvider().getPsi(GroovyLanguage.INSTANCE);
    HddGrailsTestCase.checkResolve(groovyFile, "xxx");
  }

  public void testRenameInjectionOnRenameService() {
    PsiFile serviceFile =
      myFixture.addFileToProject("grails-app/services/MyService.groovy", "class MyService { def xxx() {MyService<caret>.class} }");

    PsiFile testFile = myFixture.addFileToProject("test/unit/XxxTest.groovy", """
      import grails.test.*;
      
      class XxxTest extends GrailsUnitTestCase {
        def myService
        {
          myService.xxx()
        }
      }
      """);

    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def myService
        def index = {
          myService.xxx()
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(serviceFile.getVirtualFile());
    PsiElement element = myFixture.getElementAtCaret();
    assertSize(3, myFixture.findUsages(element));

    myFixture.renameElementAtCaret("SomeService");

    assertEquals("SomeService.groovy", serviceFile.getName());

    assertEquals("""
                   class CccController {
                     def someService
                     def index = {
                       someService.xxx()
                     }
                   }
                   """, controllerFile.getText());

    assertEquals("""
                   import grails.test.*;
                   
                   class XxxTest extends GrailsUnitTestCase {
                     def someService
                     {
                       someService.xxx()
                     }
                   }
                   """, testFile.getText());
  }

  public void testRenameInjectionOnRenameDomBean() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def pluginManager
        def index = {
          pluginManager.getFailedLoadPlugins()
        }
      }
      """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/XxxTest.groovy", """
      import grails.test.*;
      
      class XxxTest extends GrailsUnitTestCase {
        def pluginManager
        {
          pluginManager.xxx()
        }
      }
      """);

    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, "${applicationContext.getBean('pluginManage<caret>r')}");
    PsiElement element = myFixture.getElementAtCaret();
    assertSize(5, myFixture.findUsages(element));

    myFixture.renameElementAtCaret("pluginManage123");

    assertEquals("""
                   class CccController {
                     def pluginManage123
                     def index = {
                       pluginManage123.getFailedLoadPlugins()
                     }
                   }
                   """, controllerFile.getText());

    assertEquals("""
                   import grails.test.*;
                   
                   class XxxTest extends GrailsUnitTestCase {
                     def pluginManage123
                     {
                       pluginManage123.xxx()
                     }
                   }
                   """, testFile.getText());
  }

  public void testRenameCustomBean() {
    PsiFile beanFile =
      myFixture.addFileToProject("src/java/xxx/SomeBeam.java", "public class SomeBeam { void xxx() {SomeBeam<caret>.class.getName()} }");

    PsiFile resourceFile = myFixture.addFileToProject("grails-app/conf/spring/resources.xml", """
      <?xml version="1.0" encoding="UTF-8"?>
      <beans xmlns="http://www.springframework.org/schema/beans"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="
      http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd">
      
          <bean id="beanName" class="xxx.SomeBean" />
      </beans>
      """);

    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def beanName
        def index = {
          beanName.getFailedLoadPlugins()
        }
      }
      """);

    SpringFacet.getInstance(myFixture.getModule()).getFileSets().iterator().next().addFile(resourceFile.getVirtualFile());

    myFixture.configureFromExistingVirtualFile(beanFile.getVirtualFile());

    myFixture.renameElementAtCaret("NewBeanName");

    assertEquals("NewBeanName.java", beanFile.getName());

    assertEquals("""
                   class CccController {
                     def beanName
                     def index = {
                       beanName.getFailedLoadPlugins()
                     }
                   }
                   """, controllerFile.getText());
  }

  public void testSimpleBeanAccessResolve() {
    myFixture.addFileToProject("grails-app/services/SssService.groovy", "class SssService { def xxx() {SssService.class} }");
    PsiFile fileA = myFixture.addFileToProject("grails-app/views/a.gsp", "<% out << applicationContext.sssService %>");
    HddGrailsTestCase.checkResolve(fileA);
  }

  public void testSimpleBeanAccessCompletion() {
    myFixture.addFileToProject("grails-app/services/SssService.groovy", "class SssService { def xxx() {SssService.class} }");
    PsiFile fileA = myFixture.addFileToProject("grails-app/views/a.gsp", "<% out << applicationContext.<caret> %>");

    checkCompletion(fileA, "sssService");
  }

  public void testSimpleBeanAccessRenameClass() {
    PsiFile fileA = myFixture.addFileToProject("grails-app/views/a.gsp", "<% out << applicationContext.sssService %>");

    PsiFile serviceFile =
      myFixture.addFileToProject("grails-app/services/SssService.groovy", "class SssService { def xxx() {SssService<caret>.class} }");
    myFixture.configureFromExistingVirtualFile(serviceFile.getVirtualFile());

    myFixture.renameElementAtCaret("XxxService");

    assertEquals("<% out << applicationContext.xxxService %>", fileA.getText());
  }

  public void testSimpleBeanAccessRenameReference() {
    myFixture.addFileToProject("grails-app/services/SssService.groovy", "class SssService { def xxx() {SssService<caret>.class} }");

    PsiFile fileA = myFixture.addFileToProject("grails-app/views/a.gsp", "<% out << applicationContext.sssService<caret> %>");
    myFixture.configureFromExistingVirtualFile(fileA.getVirtualFile());

    myFixture.renameElementAtCaret("xxxService");

    assertEquals("<% out << applicationContext.xxxService %>", fileA.getText());

    VirtualFile newClassFile = myFixture.findFileInTempDir("grails-app/services/XxxService.groovy");
    assertNotNull(newClassFile);
    assertTrue(
      Character.isUpperCase(newClassFile.getName().charAt(0)));// Check case of file name. (First letter should be uppercase)
  }

  public void testBeansFromResourceGroovyCompletion() {
    addSimpleGroovyFile("package aaa.bbb; class Foo1 {}");
    addSimpleGroovyFile("package aaa.bbb; class Foo2 {}");

    myFixture.addFileToProject("grails-app/conf/spring/resources.groovy", """
      import aaa.bbb.*;
      
      beans {
        if (1 == 2) {
          bean1(Foo1)
        }
      }
      
      beans = {
        bean2(Foo2)
      }
      """);

    PsiFile c = addController("""
                                class CccController {
                                  def bean<caret>
                                }
                                """);

    checkCompletion(c, "bean1", "bean2");
  }

  public void testBeansFromResourceType() {
    addSimpleGroovyFile("""
                          class AbstractFoo {
                            String field1
                            String field2
                          }
                          """);
    addSimpleGroovyFile("""
                          class Foo1 extends AbstractFoo {
                            String field3
                          }
                          """);
    addSimpleGroovyFile("""
                          class Foo2 extends AbstractFoo {
                            String field4
                          }
                          """);

    myFixture.addFileToProject("grails-app/conf/spring/resources.groovy", """
      beans = {
        if (a == b) {
          bean(Foo1)
        }
        else {
          bean(Foo2)
        }
      }
      """);

    PsiFile c = addController("""
                                class CccController {
                                  def bean
                                
                                  def index = {
                                    bean.field<caret>
                                  }
                                }
                                """);

    checkCompletion(c, "field1", "field2");
    checkNonExistingCompletionVariants("field3", "field4");
  }

  public void testBeanReferenceCompletion() {
    addSimpleGroovyFile("package grails.spring; class BeanBuilder {}");

    addSimpleGroovyFile("class Foo1 {}");
    addSimpleGroovyFile("class Foo2 {}");

    PsiFile file = myFixture.addFileToProject("grails-app/conf/spring/resources.groovy", """
      beans = {
        bean1(Foo1)
        ref("<caret>")
      }
      """);

    checkCompletion(file, "bean1");
  }

  public void testGrailsAutoConfigurationIsSpringConfiguration() {
    myFixture.addClass("""
                         package grails.boot.config; public class GrailsAutoConfiguration {}""");
    myFixture.configureByText("Application.groovy", "class Application extends grails.boot.config.GrailsAutoConfiguration {}");
    assertTrue(SpringCommonUtils.isConfigurationOrMeta(myFixture.findClass("Application")));
  }
}
