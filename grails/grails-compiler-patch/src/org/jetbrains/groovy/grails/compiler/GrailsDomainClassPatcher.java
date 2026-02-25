// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.groovy.grails.compiler;

import groovy.lang.GroovyResourceLoader;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.grails.compiler.injection.DefaultGrailsDomainClassInjector;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareInjectionOperation;
import org.jetbrains.groovy.compiler.rt.CompilationUnitPatcher;

import java.io.File;

/**
 * @noinspection UnusedDeclaration
 */
public class GrailsDomainClassPatcher extends CompilationUnitPatcher {

  @Override
  public void patchCompilationUnit(CompilationUnit compilationUnit, GroovyResourceLoader resourceLoader, File[] srcFiles) {
    final DefaultGrailsDomainClassInjector injector = new DefaultGrailsDomainClassInjector();
    compilationUnit.addPhaseOperation(new GrailsAwareInjectionOperation(resourceLoader, new ClassInjector[]{injector}), Phases.CONVERSION);
  }

}