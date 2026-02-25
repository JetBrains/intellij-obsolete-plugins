// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;

public class Grails14MimePluginTest extends Grails14TestCase {
  @Override
  protected void configureGrails(@NotNull Module module, @NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    super.configureGrails(module, model, contentEntry);
    PsiTestUtil.addLibrary(model, "MimeTypes", GrailsTestUtil.getMockGrails14LibraryHome(), "grails-plugin-mimetypes-1.4.0.M1.jar");
  }

  public void testResolve() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                         html {}
                                         js {}
                                         unresolved(1, 2)
                                       }
                                     }
                                   }
                                   """);
    GrailsTestCase.checkResolve(file, "unresolved");
  }

  public void testWithFormatReturnType() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                       }.<caret>
                                     }
                                   }
                                   """);
    checkCompletion(file, "size", "putAll", "containsKey");
  }
}
