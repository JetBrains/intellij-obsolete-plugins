// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;

public class GrailsControllerActionReferenceTest extends GrailsTestCase {
  public void testControllerCompletion() {
    addController("class WwwController {def action={} }");
    PsiFile file = addController("""
                                   class CccController {
                                     def action = {
                                       redirect controller: "<caret>"
                                     }
                                   }
                                   """);
    checkCompletionVariants(file, "ccc", "www");
  }

  public void testControllerCompletion2() {
    addController("class WwwController {def action={} }");
    PsiFile file = addController("""
                                   class CccController {
                                     def action = {
                                       redirect(controller: "<caret>")
                                     }
                                   }
                                   """);
    checkCompletionVariants(file, "ccc", "www");
  }

  public void testControllerCompletion3() {
    addController("class WwwController {def action={} }");
    PsiFile file = addController("""
                                   class CccController {
                                     def action = {
                                       redirect([controller: "<caret>"])
                                     }
                                   }
                                   """);
    checkCompletionVariants(file, "ccc", "www");
  }

  public void testActionCompletion1() {
    addController("class WwwController {def waction={} }");
    PsiFile file = addController("""
                                   class CccController {
                                     def Action = {
                                       redirect([id: 4, action: "<caret>"])
                                     }
                                   }
                                   """);
    checkCompletionVariants(file, "action");
  }

  public void testActionCompletion2() {
    addController("class WwwController {def wAction={} }");
    PsiFile file = addController("""
                                   class CccController {
                                     def action = {
                                       redirect id: 4, controller: "www", action: "<caret>"
                                     }
                                   }
                                   """);
    checkCompletionVariants(file, "wAction");
  }

  public void testActionCompletion3() {
    addController("class WwwController {def waction={} }");
    PsiFile file = addController("""
                                   class CccController {
                                     def action = {
                                       redirect(id: 4, controller: "www", action: "<caret>")
                                     }
                                   }
                                   """);
    checkCompletionVariants(file, "waction");
  }

  public void testUnknownController() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {}
                                     def action = {
                                       redirect(id: 4, controller: "${someController}", action: "<caret>")
                                     }
                                   }
                                   """);
    checkCompletionVariants(file);
  }

  public void testActionRename() {
    PsiFile www = addController("class WwwController {def waction = {} }");
    PsiFile tagLib = addTaglib("""
                                 class MyTagLib {
                                   def customTag = {
                                     out << link(action: 'waction', controller: 'www')
                                     out << g.link(action: 'waction', controller: 'www')
                                     out << link(action: 'waction')
                                   }
                                 }
                                 """);
    PsiFile gsp = myFixture.addFileToProject("grails-app/views/www/g.gsp", """
      <g:link url="[action:'waction']" />
      <g:link action="waction" controller="www" />
      <% g.link(action: 'waction');
         link(action: 'waction')%>""");
    PsiFile ccc = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def action = {
          redirect(action: "waction<caret>", controller: "www")
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(ccc.getVirtualFile());

    myFixture.renameElementAtCaret("bbb");
    assertEquals("class WwwController {def bbb = {} }", www.getText());
    assertEquals("""
                   class CccController {
                     def action = {
                       redirect(action: "bbb", controller: "www")
                     }
                   }
                   """, ccc.getText());
    assertEquals("""
                   <g:link url="[action:'bbb']" />
                   <g:link action="bbb" controller="www" />
                   <% g.link(action: 'bbb');
                      link(action: 'bbb')%>""", gsp.getText());
    assertEquals("""
                   class MyTagLib {
                     def customTag = {
                       out << link(action: 'bbb', controller: 'www')
                       out << g.link(action: 'bbb', controller: 'www')
                       out << link(action: 'waction')
                     }
                   }
                   """, tagLib.getText());
  }

  public void testControllerRename() {
    PsiFile www = addController("class WwwController {def waction={} }");
    PsiFile tagLib = addTaglib("""
                                 class MyTagLib {
                                   def customTag = {
                                     out << link(action: 'waction', controller: 'www')
                                     out << g.link(action: 'waction', controller: 'www')
                                   }
                                 }
                                 """);
    PsiFile gsp = myFixture.addFileToProject("grails-app/views/g.gsp", """
      <g:link url="[controller:'www', action:'waction']" />
      <g:link action="waction" controller="www" />
      <% g.link(action: 'waction',controller: 'www');
         link(action: 'waction',controller: 'www')%>""");
    PsiFile ccc = addController("""
                                  class CccController {
                                    def action = {
                                      redirect(action: "waction", controller: "www<caret>")
                                    }
                                  }
                                  """);
    myFixture.configureFromExistingVirtualFile(ccc.getVirtualFile());

    myFixture.renameElementAtCaret("BbbController");
    assertEquals("class BbbController {def waction={} }", www.getText());
    assertEquals("""
                   class CccController {
                     def action = {
                       redirect(action: "waction", controller: "bbb")
                     }
                   }
                   """, ccc.getText());
    assertEquals("""
                   <g:link url="[controller:'bbb', action:'waction']" />
                   <g:link action="waction" controller="bbb" />
                   <% g.link(action: 'waction',controller: 'bbb');
                      link(action: 'waction',controller: 'bbb')%>""", gsp.getText());

    assertEquals("""
                   class MyTagLib {
                     def customTag = {
                       out << link(action: 'waction', controller: 'bbb')
                       out << g.link(action: 'waction', controller: 'bbb')
                     }
                   }
                   """, tagLib.getText());
  }

  public void testUrlMappingTest() {
    myFixture.addFileToProject("grails-app/controllers/WwwController.groovy", "class WwwController {def waction={} }");
    PsiFile file = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
        static mappings = {
         "/zz"(controller:"www", action:"<caret>")
        }
      }
      """);

    checkCompletionVariants(file, "waction");
  }

  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    VirtualFile dir;
    try {
      dir = myFixture.getTempDirFixture().findOrCreateDir("grails-app/conf");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    contentEntry.addSourceFolder(dir, false);
  }

  public void testDefaultActionField() {
    PsiFile file = addController("""
                                   class CccController {
                                     static defaultAction = "<caret>"
                                   
                                     def xxx = {}
                                     def yyy = {}
                                     public def getZzz() {
                                   
                                     }
                                   }
                                   """);

    checkCompletionVariants(file, "xxx", "yyy");
  }
}
