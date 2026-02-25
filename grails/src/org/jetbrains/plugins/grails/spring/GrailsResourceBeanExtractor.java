// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrOpenBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GrailsResourceBeanExtractor {

  private static boolean isLooksLikeClassReference(@NotNull GrExpression expression) {
    if (!(expression instanceof GrReferenceExpression ref)) return false;

    PsiElement nameElement = ref.getReferenceNameElement();
    if (!PsiImplUtil.isLeafElementOfType(nameElement, GroovyTokenTypes.mIDENT)) return false;

    String text = nameElement.getText();
    if (text.isEmpty() || !Character.isUpperCase(text.charAt(0))) return false;

    for (GrExpression q = ref.getQualifierExpression(); q != null; ) {
      if (!(q instanceof GrReferenceExpression)) return false;

      nameElement = ((GrReferenceExpression)q).getReferenceNameElement();
      if (!PsiImplUtil.isLeafElementOfType(nameElement, GroovyTokenTypes.mIDENT)) return false;

      q = ((GrReferenceExpression)q).getQualifierExpression();
    }

    return true;
  }

  public static @Nullable Pair<String, GrReferenceExpression> getBeanDefinition(@NotNull GrMethodCallExpression methodCall) {
    if (!(methodCall.getParent() instanceof GrCodeBlock)) return null;

    String beanName = PsiUtil.getUnqualifiedMethodName(methodCall);
    if (beanName == null) return null;

    GrExpression[] allArguments = PsiUtil.getAllArguments(methodCall);
    if (allArguments.length == 0) return null;

    int classNameArgumentIndex = 0;
    if (allArguments[0] == null) {
      if (allArguments.length == 1) return null;
      classNameArgumentIndex = 1;
    }

    if (!isLooksLikeClassReference(allArguments[classNameArgumentIndex])) return null;

    return Pair.create(beanName, (GrReferenceExpression)allArguments[classNameArgumentIndex]);
  }

  private static void processBeanDefinitionClosure(final Map<String, BeanDescriptor> descriptorMap, @NotNull GrClosableBlock closure) {
    closure.acceptChildren(new GroovyRecursiveElementVisitor() {
      @Override
      public void visitMethodCallExpression(@NotNull GrMethodCallExpression methodCallExpression) {
        Pair<String, GrReferenceExpression> pair = getBeanDefinition(methodCallExpression);
        if (pair != null) {
          BeanDescriptor beanDescriptor = descriptorMap.get(pair.first);
          if (beanDescriptor == null) {
            beanDescriptor = new BeanDescriptor(pair.first);
            descriptorMap.put(pair.first, beanDescriptor);
          }

          beanDescriptor.getReferences().add(pair.second);
        }
      }
    });
  }

  private static List<BeanDescriptor> evaluateBeanDescriptorsFromResourcesGroovy(@NotNull GroovyFile file) {
    Map<String, BeanDescriptor> descriptorMap = new HashMap<>();

    for (PsiElement e = file.getFirstChild(); e != null; e = e.getNextSibling()) {
      if (e instanceof GrMethodCallExpression) {
        String methodName = PsiUtil.getUnqualifiedMethodName((GrMethodCall)e);
        if ("beans".equals(methodName)) {
          GrExpression[] allArguments = PsiUtil.getAllArguments((GrCall)e);
          if (allArguments.length == 1 && allArguments[0] instanceof GrClosableBlock) {
            processBeanDefinitionClosure(descriptorMap, (GrClosableBlock)allArguments[0]);
          }
        }
      }

      if (e instanceof GrAssignmentExpression assign) {

        if (!assign.isOperatorAssignment()) {
          GrExpression rValue = ((GrAssignmentExpression)e).getRValue();
          if (rValue instanceof GrClosableBlock) {
            GrExpression lValue = ((GrAssignmentExpression)e).getLValue();
            if (lValue instanceof GrReferenceExpression && "beans".equals(lValue.getText())) {
              processBeanDefinitionClosure(descriptorMap, (GrClosableBlock)rValue);
            }
          }
        }
      }
    }

    return new ArrayList<>(descriptorMap.values());
  }

  private static List<BeanDescriptor> evaluateBeansFromPluginClass(@NotNull GrTypeDefinition pluginClass) {
    final GrClosableBlock closure = getInitializer(pluginClass);
    if (closure == null) return Collections.emptyList();
    Map<String, BeanDescriptor> descriptorMap = new HashMap<>();
    processBeanDefinitionClosure(descriptorMap, closure);
    return new ArrayList<>(descriptorMap.values());
  }

  private static GrClosableBlock getInitializer(@NotNull GrTypeDefinition pluginClass) {
    PsiField field = pluginClass.findCodeFieldByName("doWithSpring", false);
    if (field instanceof GrField) {
      GrExpression initializer = ((GrField)field).getInitializerGroovy();
      if (initializer instanceof GrClosableBlock) {
        return ((GrClosableBlock)initializer);
      }
    }

    PsiMethod[] methods = pluginClass.findCodeMethodsByName("doWithSpring", false);
    if (methods.length == 1) {
      PsiMethod method = methods[0];
      if (method instanceof GrMethod) {
        GrOpenBlock block = ((GrMethod)method).getBlock();
        if (block != null) {
          GrStatement[] statements = block.getStatements();
          if (statements.length == 1) {
            GrStatement statement = statements[0];
            if (statement instanceof GrClosableBlock) {
              return (GrClosableBlock)statement;
            }
          }
        }
      }
    }

    return null;
  }

  public static List<BeanDescriptor> getBeanDescriptorsFromResourcesGroovy(@NotNull GroovyFile file) {
    return CachedValuesManager.getCachedValue(file, () -> Result.create(
      evaluateBeanDescriptorsFromResourcesGroovy(file), file
    ));
  }

  public static List<BeanDescriptor> getBeanDescriptorsFromPluginClass(@NotNull GrTypeDefinition pluginClass) {
    return CachedValuesManager.getCachedValue(pluginClass, () -> Result.create(
      evaluateBeansFromPluginClass(pluginClass), pluginClass
    ));
  }

  public static class BeanDescriptor {

    private final String myName;
    private final List<GrReferenceExpression> myReferences = new ArrayList<>();

    public BeanDescriptor(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }

    public List<GrReferenceExpression> getReferences() {
      return myReferences;
    }

    public @Nullable PsiType getType() {
      PsiManager manager = myReferences.get(0).getManager();

      PsiType res = null;

      for (GrReferenceExpression reference : myReferences) {
        PsiElement resolve = reference.resolve();

        if (resolve instanceof PsiClass) {
          res = TypesUtil.getLeastUpperBoundNullable(res, PsiTypesUtil.getClassType((PsiClass)resolve), manager);
        }
      }

      return res;
    }
  }

}
