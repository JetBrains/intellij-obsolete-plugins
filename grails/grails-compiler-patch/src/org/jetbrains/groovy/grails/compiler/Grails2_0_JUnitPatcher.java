// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.groovy.grails.compiler;

import groovy.lang.GroovyResourceLoader;
import groovyjarjarasm.asm.Opcodes;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.MixinNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.jetbrains.groovy.compiler.rt.CompilationUnitPatcher;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.List;

public class Grails2_0_JUnitPatcher extends CompilationUnitPatcher {
  @Override
  public void patchCompilationUnit(CompilationUnit compilationUnit, GroovyResourceLoader resourceLoader, File[] srcFiles) {
    compilationUnit.addPhaseOperation(new CompilationUnit.PrimaryClassNodeOperation() {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        if (hasTestForAnnotation(classNode)) {
          source.getAST().addStaticStarImport(null, ClassHelper.make("org.junit.Assert"));
        }
      }

      @Override
      public void doPhaseOperation(CompilationUnit unit) throws CompilationFailedException {
        super.doPhaseOperation(unit);
      }
    }, Phases.CONVERSION);

    compilationUnit.addPhaseOperation(new CompilationUnit.PrimaryClassNodeOperation() {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        if (hasTestForAnnotation(classNode)) {
          List<MethodNode> methods = classNode.getMethods();

          for (MethodNode method : methods) {
            if (!GrailsJUnitPatcher.isTestMethod(method)) continue;
            if (hasAnnotation(method, "org.junit.Test")) continue;

            if ("java.lang.Object".equals(method.getReturnType().getName())) {
              if (GrailsJUnitPatcher.isReturnNonVoid(method)) continue;
              method.setReturnType(ClassHelper.VOID_TYPE);
            }
            else {
              if (!ClassHelper.VOID_TYPE.equals(method.getReturnType())) continue;
            }

            AnnotationNode annotationNode = new AnnotationNode(new ClassNode("org.junit.Test",
                                                                             Opcodes.ACC_PUBLIC |
                                                                             Opcodes.ACC_INTERFACE |
                                                                             Opcodes.ACC_ABSTRACT
                                                                             |
                                                                             Opcodes.ACC_ANNOTATION,
                                                                             ClassHelper.OBJECT_TYPE,
                                                                             new ClassNode[]{ClassHelper.make(Annotation.class)},
                                                                             MixinNode.EMPTY_ARRAY
            ));
            annotationNode.setRuntimeRetention(true);
            annotationNode.setAllowedTargets(AnnotationNode.METHOD_TARGET);

            method.addAnnotation(annotationNode);
          }
        }
      }

      @Override
      public void doPhaseOperation(CompilationUnit unit) throws CompilationFailedException {
        super.doPhaseOperation(unit);
      }
    }, Phases.INSTRUCTION_SELECTION);
  }

  public static boolean hasAnnotation(AnnotatedNode node, String annotationName) {
    List<AnnotationNode> annotations = node.getAnnotations();
    for (AnnotationNode annotationNode : annotations) {
      if (annotationName.equals(annotationNode.getClassNode().getName())) {
        return true;
      }
    }

    return false;
  }

  public static boolean hasTestForAnnotation(ClassNode classNode) {
    List<AnnotationNode> annotations = classNode.getAnnotations();
    for (AnnotationNode annotationNode : annotations) {
      String annotationName = annotationNode.getClassNode().getName();

      if ("grails.test.mixin.TestFor".equals(annotationName)
          || "TestFor".equals(annotationName)
          || "grails.test.mixin.TestMixin".equals(annotationName)
          || "TestMixin".equals(annotationName)
          || "grails.test.mixin.Mock".equals(annotationName)
          || "Mock".equals(annotationName)
          || "grails.buildtestdata.mixin.Build".equals(annotationName)
          || "Build".equals(annotationName)
        ) {
        return true;
      }
    }

    return false;
  }

}
