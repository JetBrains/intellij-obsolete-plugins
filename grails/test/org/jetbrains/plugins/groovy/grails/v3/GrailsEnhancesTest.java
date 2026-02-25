// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.v3;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.LightGroovyTestCase;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;

public class GrailsEnhancesTest extends LightGroovyTestCase {

  private final GrailsProjectDescriptor projectDescriptor = new GrailsProjectDescriptor("grails-app/controllers/");

  @Override
  public final @NotNull GrailsProjectDescriptor getProjectDescriptor() {
    return projectDescriptor;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getFixture().addClass("""
                            package grails.artefact;
                            public @interface Enhances { String[] value(); }
                            """);
  }

  public void testEnhances() {
    getFixture().addFileToProject("com/foo/traits.groovy", """
      package com.foo
      
      @grails.artefact.Enhances("Controller")
      trait HelloTrait {
        String hello() { "Hello" }
      }
      
      @grails.artefact.Enhances(["Controller"])
      trait WorldTrait {
        String world() { "world" }
      }
      """);
    PsiFile file = getFixture().addFileToProject("com/bar/SimpleController.groovy", """
      package com.bar
      class SimpleController {
        def index() {
          hello()
          world()
        }
      }
      """);
    getFixture().configureFromExistingVirtualFile(file.getVirtualFile());
    getFixture().enableInspections(GrUnresolvedAccessInspection.class);
    getFixture().checkHighlighting();
  }
}
