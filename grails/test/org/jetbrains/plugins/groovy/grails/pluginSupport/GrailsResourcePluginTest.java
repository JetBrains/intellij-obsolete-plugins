// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.pluginSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.io.IOException;

public class GrailsResourcePluginTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    try {
      TempDirTestFixture tdf = myFixture.getTempDirFixture();
      VirtualFile file = tdf.findOrCreateDir("grails-app/conf");
      contentEntry.addSourceFolder(file, false);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    addSimpleGroovyFile("""
                          package org.grails.plugin.resource.module;
                          class ModuleBuilder {
                              void dependsOn(List dependencies) {}
                              void dependsOn(String[] dependencies) {}
                              void dependsOn(String dependencies) {}
                              void defaultBundle(value) {}
                              Object getResource() {}
                              void resource(args) {}
                          }
                          """);

    addTaglib("""
                class SssTagLib {
                    static namespace = "r"
                
                    def require = {}
                    def img = {}
                    def external = {}
                }
                """);
  }

  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.resources.modules = {
        aaa {}
        bbb {}
      }
      """);

    configureByView("a.gsp", """
      <r:require module="aaa" />
      <r:require module="<error>zzz</error>" />
      <r:require modules="aaa, bbb  ,ccc  , <error>zzz</error> " />
      ${r.require(module: "aaa")}
      ${r.require(modules: ""\"aaa, bbb  ,ccc  , zzz ""\")}
      """);
  }

  public void testRename() {
    PsiFile confFile = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.resources.modules = {
        aaa<caret> {}
      }
      """);

    PsiFile file = myFixture.addFileToProject("grails-app/conf/AppResources.groovy", """
      modules = {
        xxx {
          dependsOn "aaa"
          dependsOn 'aaa'
          dependsOn f ? 'aaa' : 'aaa'
          dependsOn(['aaa', f ? 'aaa' : 'aaa'])
          dependsOn 'aaa', 'aaa', 'aaa'
          dependsOn 'aaa, aaa,aaa'
          resource 'aaa'
        }
      }
      """);
    PsiFile gspFile = addView("a.gsp", """
      <r:require module="aaa" />
      <r:require modules="aaa, aaa, aaa aaa" />
      <r:renderModule name='aaa' />
      ${r.require(module: "aaa")}
      ${r.require(modules: ""\"aaa,aaa,   aaa,aaa""\")}
      """);

    myFixture.configureFromExistingVirtualFile(confFile.getVirtualFile());
    myFixture.renameElementAtCaret("bbb111");

    myFixture.checkResult("""
                            grails.resources.modules = {
                              bbb111 {}
                            }
                            """);

    TestCase.assertEquals("""
                            modules = {
                              xxx {
                                dependsOn "bbb111"
                                dependsOn 'bbb111'
                                dependsOn f ? 'bbb111' : 'bbb111'
                                dependsOn(['bbb111', f ? 'bbb111' : 'bbb111'])
                                dependsOn 'bbb111', 'bbb111', 'bbb111'
                                dependsOn 'bbb111, bbb111,bbb111'
                                resource 'aaa'
                              }
                            }
                            """, file.getText());

    TestCase.assertEquals("""
                            <r:require module="bbb111" />
                            <r:require modules="bbb111, bbb111, bbb111 bbb111" />
                            <r:renderModule name='bbb111' />
                            ${r.require(module: "bbb111")}
                            ${r.require(modules: ""\"bbb111,bbb111,   bbb111,bbb111""\")}
                            """, gspFile.getText());
  }

  public void testRenameModuleDeclaredAsString() {
    PsiFile confFile = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.resources.modules = {
        "aaa-bbb<caret>" {}
        zzz {
          dependsOn 'aaa-bbb'
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(confFile.getVirtualFile());
    myFixture.renameElementAtCaret("qqq");

    myFixture.checkResult("""
                            grails.resources.modules = {
                              qqq {}
                              zzz {
                                dependsOn 'qqq'
                              }
                            }
                            """);
  }

  public void testCompletion() {
    PsiFile confFile = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      environments {
        development {
          grails.resources.modules = {
            ddd {}
            uuu {}
          }
        }
      }
      
      grails.resources.modules = {
        aaa {}
        bbb {}
        ccc {
          dependsOn(""\"aaa, <caret> ""\")
        }
      }
      """);
    checkCompletion(confFile, "bbb", "ddd", "uuu");
  }

  public void testUrlReference() {
    myFixture.addFileToProject("web-app/css/ccc.css", "");

    PsiFile gsp = addView("a.gsp", """
      <r:img uri="/css/ccc.css" />
      """);

    PsiFile confFile = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.resources.modules = {
        zzz {
          resource("/css/ccc.css")
          resource("css/ccc.css")
          resource(url: "css/ccc.css")
          resource(url: "/css/ccc.css<caret>")
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(confFile.getVirtualFile());
    myFixture.renameElementAtCaret("c.css");

    myFixture.checkResult("""
                            grails.resources.modules = {
                              zzz {
                                resource("/css/c.css")
                                resource("css/c.css")
                                resource(url: "css/c.css")
                                resource(url: "/css/c.css")
                              }
                            }
                            """);

    TestCase.assertEquals("""
                            <r:img uri="/css/c.css" />
                            """, gsp.getText());
  }
}
