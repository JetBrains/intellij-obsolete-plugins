// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.pluginSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GrailsShiroPluginTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    try {
      VirtualFile applicationProperties = myFixture.getTempDirFixture().findOrCreateDir("application.properties");
      applicationProperties.setBinaryContent("plugins.shiro=1.1.3".getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addAccessControllerBuilder() {
    myFixture.copyFileToProject("AccessControlBuilder.groovy", "src/groovy/org/apache/shiro/grails/AccessControlBuilder.groovy");
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/shiro/");
  }

  public void testSearchableFieldCompletion() {
    configureByController("""
                            class CccController {
                              static <caret>
                            }
                            """);
    checkCompletion("accessControl");
  }

  public void testCompletionMethodsFromAccessControlBuilder() {
    addAccessControllerBuilder();

    configureByController("""
                            class CccController {
                              static accessControl = {
                                <caret>
                              }
                            }
                            """);
    checkCompletion("role", "permission");
  }

  public void testResolveMethodsFromAccessControlBuilder() {
    addAccessControllerBuilder();

    PsiFile file = addController("""
                                   class CccController {
                                     static accessControl = {
                                       role(name: 'Editor', only:['createNews'] )
                                     }
                                   
                                     def createNews = {}
                                   }
                                   """);
    GrailsTestCase.checkResolve(file);
  }

  public void testActionReferenceRename() {
    addAccessControllerBuilder();

    configureByController("""
                            class CccController {
                              static accessControl = {
                                role(name: 'RoleName1', only:["xxx", "yyy"] )
                                permission(name: 'RoleName1', only:["xxx", "yyy"] )
                            
                                role(name: 'RoleName2', action: 'xxx')
                                permission(name: 'RoleName2', action: 'xxx')
                              }
                            
                              def xxx<caret> = {}
                            
                              def yyy = {}
                            }
                            """);

    myFixture.renameElementAtCaret("f");

    myFixture.checkResult("""
                            class CccController {
                              static accessControl = {
                                role(name: 'RoleName1', only:["f", "yyy"] )
                                permission(name: 'RoleName1', only:["f", "yyy"] )
                            
                                role(name: 'RoleName2', action: 'f')
                                permission(name: 'RoleName2', action: 'f')
                              }
                            
                              def f = {}
                            
                              def yyy = {}
                            }
                            """);
  }
}
