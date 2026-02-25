// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.groovy.grails.compiler;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyResourceLoader;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareInjectionOperation;
import org.jetbrains.groovy.compiler.rt.CompilationUnitPatcher;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @noinspection UnusedDeclaration
 */
public class GrailsJUnitPatcher extends CompilationUnitPatcher {

  @Override
  public void patchCompilationUnit(CompilationUnit compilationUnit, GroovyResourceLoader resourceLoader, File[] srcFiles) {
    Object instance;

    ClassInjector[] injectors = {new GrailsJUnitInjector()};

    try {
      try {
        // for Grails 1.4 call new GrailsAwareInjectionOperation(new ClassInjector[]{...})
        Constructor c = GrailsAwareInjectionOperation.class.getConstructor(ClassInjector[].class);
        instance = c.newInstance(new Object[]{injectors});
      }
      catch (NoSuchMethodException e) {
        // for Grails 1.3 call new GrailsAwareInjectionOperation(resourceLoader, new ClassInjector[]{...})
        Constructor c = GrailsAwareInjectionOperation.class.getConstructor(GroovyResourceLoader.class, ClassInjector[].class);
        instance = c.newInstance(resourceLoader, injectors);
      }
    }
    catch (Exception ee) {
      throw new RuntimeException(ee);
    }

    compilationUnit.addPhaseOperation((GrailsAwareInjectionOperation)instance, Phases.INSTRUCTION_SELECTION);
  }

  private static class GrailsJUnitInjector implements ClassInjector {

    private static final Pattern URL_PATTERN = Pattern.compile(".+/test/(.+/)?.+Tests?\\.groovy");

    @Override
    public void performInjection(SourceUnit sourceUnit, GeneratorContext generatorContext, ClassNode classNode) {
      addEnableGlobally(classNode);

      if (isExtendsGrailsUnitTestCace(classNode)) {
        patchReturnType(classNode);
      }
    }

    private static void addEnableGlobally(ClassNode classNode) {
      List<Statement> initializer = new ArrayList<>();
      initializer.add(new ExpressionStatement(new StaticMethodCallExpression(new ClassNode(ExpandoMetaClass.class),
                                                                             "enableGlobally",
                                                                             new ArgumentListExpression())));
      initializer.add(new ExpressionStatement(new StaticMethodCallExpression(new ClassNode(System.class),
                                                                             "setProperty",
                                                                             new ArgumentListExpression(
                                                                               new ConstantExpression("net.sf.ehcache.skipUpdateCheck"),
                                                                               new ConstantExpression("true")))));
      classNode.addStaticInitializerStatements(initializer, true);
    }

    private static void patchReturnType(ClassNode classNode) {
      List<MethodNode> methods = classNode.getMethods();

      for (MethodNode method : methods) {
        if (!isTestMethod(method)) continue;

        if (!"java.lang.Object".equals(method.getReturnType().getName())) continue;

        if (isReturnNonVoid(method)) continue;

        method.setReturnType(ClassHelper.VOID_TYPE);
      }
    }

    private static boolean isExtendsGrailsUnitTestCace(ClassNode classNode) {
      for (ClassNode parent = classNode.getSuperClass(); parent != null; parent = parent.getSuperClass()) {
        if ("grails.test.GrailsUnitTestCase".equals(parent.getName())) {
          return true;
        }
      }

      return false;
    }

    @Override
    public void performInjection(SourceUnit sourceUnit, ClassNode classNode) {
      performInjection(sourceUnit, null, classNode);
    }

    @Override
    public boolean shouldInject(URL url) {
      if (url == null) return false;

      return URL_PATTERN.matcher(url.getFile()).find();
    }
  }

  public static boolean isReturnNonVoid(MethodNode method) {
    Statement code = method.getCode();
    if (code == null) return false;

    final boolean[] res = new boolean[1];

    code.visit(new CodeVisitorSupport() {
      @Override
      public void visitReturnStatement(ReturnStatement statement) {
        if (!statement.isReturningNullOrVoid()) {
          res[0] = true;
        }
      }
    });

    return res[0];
  }

  public static boolean isTestMethod(MethodNode methodNode) {
    return methodNode.getParameters().length == 0
           && methodNode.getName().startsWith("test")
           && (methodNode.getModifiers() & (Modifier.ABSTRACT | Modifier.PRIVATE | Modifier.PROTECTED)) == 0;
  }
}