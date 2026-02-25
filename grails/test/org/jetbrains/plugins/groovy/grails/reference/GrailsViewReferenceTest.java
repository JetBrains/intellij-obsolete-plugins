// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsViewReferenceTest extends GrailsTestCase {
  public void testMove() {
    String path = "grails-app/views/ccc/aView.gsp";
    myFixture.addFileToProject(path, "Some text");
    PsiFile cccControllerFile = addController("""
                                                class CccController {
                                                 def index = {
                                                  render(view: 'aView')
                                                  render(view: '/ccc/aView')
                                                  render(view: '/aView')
                                                 }
                                                }
                                                """);

    PsiFile dddControllerFile = addController("""
                                                class DddController {
                                                 def index = {
                                                  render(view: '/ccc/aView')
                                                  render(view: 'aView')
                                                  render(view: '/aView')
                                                 }
                                                }
                                                """);

    myFixture.addFileToProject("grails-app/views/ccc/v/a.txt", "Dummy file to create folder");
    myFixture.moveFile(path, "grails-app/views/ccc/v");

    assertEquals("""
                   class CccController {
                    def index = {
                     render(view: 'v/aView')
                     render(view: '/ccc/v/aView')
                     render(view: '/aView')
                    }
                   }
                   """, cccControllerFile.getText());

    assertEquals("""
                   class DddController {
                    def index = {
                     render(view: '/ccc/v/aView')
                     render(view: 'aView')
                     render(view: '/aView')
                    }
                   }
                   """, dddControllerFile.getText());

    myFixture.addFileToProject("grails-app/views/shared/a.txt", "Dummy file to create folder");
    myFixture.moveFile("grails-app/views/ccc/v/aView.gsp", "grails-app/views/shared/");

    assertEquals("""
                   class CccController {
                    def index = {
                     render(view: '/shared/aView')
                     render(view: '/shared/aView')
                     render(view: '/aView')
                    }
                   }
                   """, cccControllerFile.getText());

    assertEquals("""
                   class DddController {
                    def index = {
                     render(view: '/shared/aView')
                     render(view: 'aView')
                     render(view: '/aView')
                    }
                   }
                   """, dddControllerFile.getText());
  }

  public void testRenameGsp() {
    PsiFile gspFile = myFixture.addFileToProject("grails-app/views/ccc/aView.gsp", "Some text");
    myFixture.addFileToProject("grails-app/views/ccc/aView.jsp", "Some text");
    PsiFile cccControllerFile = addController("""
                                                class CccController {
                                                 def index = {
                                                  render(view: "aView")
                                                  render(view: "/ccc/aView")
                                                  render(view: "/aView")
                                                 }
                                                }
                                                """);

    PsiFile dddControllerFile = configureByController("""
                                                        class DddController {
                                                         def index = {
                                                          render(view: "/ccc/aView<caret>")
                                                          render(view: "aView")
                                                          render(view: "/aView")
                                                         }
                                                        }
                                                        """);

    myFixture.renameElementAtCaret("ttt.gsp");

    assertEquals("ttt.gsp", gspFile.getName());

    assertEquals("""
                   class CccController {
                    def index = {
                     render(view: "ttt")
                     render(view: "/ccc/ttt")
                     render(view: "/aView")
                    }
                   }
                   """, cccControllerFile.getText());

    assertEquals("""
                   class DddController {
                    def index = {
                     render(view: "/ccc/ttt")
                     render(view: "aView")
                     render(view: "/aView")
                    }
                   }
                   """, dddControllerFile.getText());
  }

  public void testRenameJsp() {
    PsiFile gspFile = myFixture.addFileToProject("grails-app/views/ccc/aView.jsp", "Some text");
    PsiFile cccControllerFile = addController("""
                                                class CccController {
                                                 def index = {
                                                  render(view: "aView")
                                                  render(view: "/ccc/aView")
                                                  render(view: "/aView")
                                                 }
                                                }
                                                """);

    PsiFile dddControllerFile = configureByController("""
                                                        class DddController {
                                                         def index = {
                                                          render(view: "/ccc/aView<caret>")
                                                          render(view: "aView")
                                                          render(view: "/aView")
                                                         }
                                                        }
                                                        """);

    myFixture.renameElementAtCaret("ttt.jsp");

    assertEquals("ttt.jsp", gspFile.getName());

    assertEquals("""
                   class CccController {
                    def index = {
                     render(view: "ttt")
                     render(view: "/ccc/ttt")
                     render(view: "/aView")
                    }
                   }
                   """, cccControllerFile.getText());

    assertEquals("""
                   class DddController {
                    def index = {
                     render(view: "/ccc/ttt")
                     render(view: "aView")
                     render(view: "/aView")
                    }
                   }
                   """, dddControllerFile.getText());
  }
}
