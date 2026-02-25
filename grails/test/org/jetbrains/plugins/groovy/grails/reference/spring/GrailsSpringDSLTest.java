// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.spring;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsSpringDSLTest extends Grails14TestCase {
  public void testResolveResourcesGroovy() {
    addSimpleGroovyFile("class Foo1 { String name; def xxx }");

    String textForResolve = """
        if (application.allArtefacts.length == 0) {
          bean1(Foo1) {
            name = ""
            xxx = ref("pluginManager")
            getParentCtx()
          }
        }
      
        ref("bean1")
        getParentCtx()
      
        unresolvedRef()
        unresolvedRef2(foo: '1')
      """;

    PsiFile resourcesFile = addResourcesGroovy("""
                                                 beans = {
                                                 """ + textForResolve + """
                                                 
                                                 }
                                                 """);

    GrailsTestCase.checkResolve(resourcesFile, "unresolvedRef", "unresolvedRef2");

    PsiFile pluginFile = addSimpleGroovyFile("""
                                               class MyGrailsPlugin {
                                                 def doWithSpring = {
                                               """ + textForResolve + """
                                               
                                                 }
                                               }
                                               """);

    GrailsTestCase.checkResolve(pluginFile, "unresolvedRef", "unresolvedRef2");
  }

  // todo failing, missing deps?!
  public void testHighlightUsages() {
    addSimpleGroovyFile("class Foo1 { String name }");

    addResourcesGroovy("""
                         beans = {
                           if (a == 1) {
                             bean1(Foo1)
                           }
                           else if (a == 2) {
                             bean1(Foo1)
                           }
                           else {
                             bean1<caret>(Foo1)
                           }
                         }
                         """);

    RangeHighlighter[] res = myFixture.testHighlightUsages("grails-app/conf/spring/resources.groovy");
    assertEquals(3, res.length);
  }

  public void testHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    addSimpleGroovyFile("class Foo { String name }");
    addSimpleGroovyFile("""
                          class BeanClass {
                            String name
                            Foo foo
                            Foo foo2
                            Foo foo3
                          }
                          """);

    configureBySimpleGroovyFile("""
                                  class MyGrailsPlugin {
                                    def doWithSpring = {
                                      fooBean(Foo)
                                  
                                      bean(BeanClass) {
                                        name = ""
                                        foo = ref("fooBean")
                                        <warning>foo2</warning> = 2
                                        foo3 = new Foo()
                                      }
                                    }
                                  }
                                  """);

    myFixture.checkHighlighting(true, false, true);
  }

  private PsiFile addResourcesGroovy(String text) {
    return myFixture.addFileToProject("grails-app/conf/spring/resources.groovy", text);
  }
}
