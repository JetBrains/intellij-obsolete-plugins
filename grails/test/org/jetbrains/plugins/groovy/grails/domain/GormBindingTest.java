// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormBindingTest extends GrailsTestCase {
  private void addDomain() {
    addDomain("""
                  class City {
                    String name;
                
                    public int getPopulation() {
                      return 1;
                    }
                  }
                """);
  }

  public void testResolve() {
    if (!useGrails14()) {
      myFixture.addClass("""
        package org.codehaus.groovy.grails.web.servlet.mvc;
        public class GrailsParameterMap implements java.util.Map {}
      """);
    }

    addDomain();
    PsiFile c = addController("""
      class CccController {
        def save = {
          def b = City.get(params.id)
          b.properties = params
          b.save()
          b.unresolvedReference = 2;
        }
      }
    """);
    GrailsTestCase.checkResolve(c, "unresolvedReference");
  }

  public void testCompletion() {
    addDomain();
    PsiFile file = addController("""
                                   class CccController {
                                     def save = {
                                       def b = City.get(params.id)
                                       b.properties['<caret>', ""\"name""\"] = params
                                       b.save()
                                     }
                                   }
                                   """);

    checkCompletionVariants(file, "population", "id", "version");
  }

  public void testRename() {
    addDomain();

    configureByController("""
                            
                            class CccController {
                              def save = {
                                def b = City.get(params.id)
                                b.properties['populatio<caret>n', ""\"name""\"] = params
                                b.properties['population', ""\"name""\"] = params
                                b.save()
                              }
                            }
                            """);

    myFixture.renameElementAtCaret("getPpp");

    myFixture.checkResult("""
                            
                            class CccController {
                              def save = {
                                def b = City.get(params.id)
                                b.properties['ppp', ""\"name""\"] = params
                                b.properties['ppp', ""\"name""\"] = params
                                b.save()
                              }
                            }
                            """);
  }
}
