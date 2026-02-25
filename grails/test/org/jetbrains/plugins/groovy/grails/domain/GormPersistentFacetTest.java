// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.facet.FacetManager;
import com.intellij.hibernate.facet.HibernateFacet;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

public class GormPersistentFacetTest extends HddGrailsTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addFileToProject("grails-app/conf/hibernate/hibernate.cfg.xml", "<hibernate-configuration></hibernate-configuration>");
    setupFacets();
  }

  public void testAddFacet() {
    HibernateFacet facet = FacetManager.getInstance(getModule()).getFacetByType(HibernateFacet.ID);
    TestCase.assertNotNull(facet);
    UsefulTestCase.assertSize(1, facet.getDescriptors());
  }

  public void testCompletionInFindAll() {
    addDomain("""
                
                class Ddd {
                  String name;
                  static hasMany = [many: String]
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       Ddd.findAll("from Ddd d where d.<caret>")
                                     }
                                   }
                                   """);
    checkCompletion(file, "id", "version", "many", "name");
  }

  public void testCompletionInExecuteQuery() {
    addDomain("""
                
                class Ddd {
                  String name;
                  static hasMany = [many: String]
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       Ddd.executeQuery("from Ddd d where d.<caret>")
                                     }
                                   }
                                   """);
    checkCompletion(file, "id", "version", "many", "name");
  }

  public void testCompletionInFind() {
    addDomain("""
                
                class Ddd {
                  String name;
                  static hasMany = [many: String]
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       Ddd.find("from Ddd d where d.<caret>")
                                     }
                                   }
                                   """);
    checkCompletion(file, "id", "version", "many", "name");
  }
}
