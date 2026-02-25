// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrMethodWrapper;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtilKt;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GrailsResourcesGroovyMemberContributor extends NonCodeMembersContributor {
  private static final Key<PsiMethod[]> REF_METHOD_CACHE_KEY = Key.create("GrailsResourcesGroovyMemberContributor.REF_METHOD_KEY");

  private static final String CLASS_SOURCE = "class GrailsSpringResources {" +
                                              "  org.codehaus.groovy.grails.commons.GrailsApplication application;" +
                                              "  org.codehaus.groovy.grails.commons.GrailsApplication grailsApplication;" +
                                              "}";
  public static final String RUNTIME_BEAN_REF = "org.springframework.beans.factory.config.RuntimeBeanReference";

  public static final String REF_METHOD_KIND = "BeanBuilder:ref()";
  public static final String BEAN_BUILDER = "grails.spring.BeanBuilder";

  @Override
  protected @NotNull String getParentClassName() {
    return "resources"; // grails-app/conf/spring/resources.groovy
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;

    PsiFile psiFile = aClass.getContainingFile();
    VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
    if (virtualFile == null) return;

    if (!virtualFile.getPath().endsWith("grails-app/conf/spring/resources.groovy")) return;

    GrClosableBlock block = PsiTreeUtil.getParentOfType(place, GrClosableBlock.class);
    if (block == null) return;

    GrClosableBlock beansClosure = null;
    GrClosableBlock beanDefinitionClosure = null;

    if (isClosureAssignedToBeanProperty(block)) {
      beansClosure = block;
    }
    else {
      GrClosableBlock parentBlock = PsiTreeUtil.getParentOfType(block, GrClosableBlock.class);
      if (parentBlock != null && isClosureAssignedToBeanProperty(parentBlock)) {
        beansClosure = parentBlock;
        beanDefinitionClosure = block;
      }
    }

    if (beansClosure == null) return;

    if (!DynamicMemberUtils.process(processor, false, place, CLASS_SOURCE)) return;

    if (!processBeanDefinition(processor, place, state, beansClosure, beanDefinitionClosure)) return;
  }

  public static boolean processBeanDefinition(PsiScopeProcessor processor,
                                               PsiElement place,
                                               ResolveState state,
                                               GrClosableBlock beansClosure,
                                               @Nullable GrClosableBlock beanDefinitionClosure) {
    if (!(place instanceof GrReferenceExpression)) return true;

    PsiManager psiManager = beansClosure.getManager();
    GlobalSearchScope resolveScope = beansClosure.getResolveScope();
    String nameHint = ResolveUtil.getNameHint(processor);

    PsiClass beanBuilder = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(BEAN_BUILDER, resolveScope);
    if (beanBuilder != null) {
      if (!beanBuilder.processDeclarations(processor, state, null, place)) return false;

      if (ResolveUtilKt.shouldProcessMethods(processor) && (nameHint == null || nameHint.equals("ref"))) {
        if (!processRefMethods(processor, state, beanBuilder, resolveScope)) return false;
      }
    }

    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return true;
    if (beanDefinitionClosure == null) {
      if (nameHint == null) return true;

      PsiElement eMethodCall = place.getParent();
      if (!(eMethodCall instanceof GrMethodCallExpression)) return true;

      Pair<String,GrReferenceExpression> pair = GrailsResourceBeanExtractor.getBeanDefinition((GrMethodCallExpression)eMethodCall);
      if (pair == null) return true;

      String beanName = pair.first;

      if (!nameHint.equals(beanName)) return true;

      ConcurrentMap<String, GrLightMethodBuilder> methodMap = CachedValuesManager.getCachedValue(beansClosure, () -> Result.create(
        (ConcurrentMap<String, GrLightMethodBuilder>)new ConcurrentHashMap<String, GrLightMethodBuilder>(), beansClosure
      ));

      GrLightMethodBuilder res = methodMap.get(beanName);
      if (res == null) {
        res = new GrLightMethodBuilder(psiManager, beanName);
        res.addParameter("beanClass", CommonClassNames.JAVA_LANG_CLASS);

        PsiClassType objectType = PsiType.getJavaLangObject(res.getManager(), resolveScope);

        res.addParameter("args", new PsiEllipsisType(objectType));

        res.setReturnType("org.codehaus.groovy.grails.commons.spring.BeanConfiguration", resolveScope);

        GrLightMethodBuilder prevValue = methodMap.putIfAbsent(beanName, res);
        if (prevValue != null) {
          res = prevValue;
        }
      }

      return processor.execute(res, state);
    }
    else {
      // Process bean property initializers
      if (((GrReferenceExpression)place).isQualified()) return true;

      PsiElement eMethodCall = beanDefinitionClosure.getParent();
      if (eMethodCall instanceof GrArgumentList) eMethodCall = eMethodCall.getParent();
      if (!(eMethodCall instanceof GrMethodCallExpression)) return true;

      Pair<String,GrReferenceExpression> pair = GrailsResourceBeanExtractor.getBeanDefinition((GrMethodCallExpression)eMethodCall);
      if (pair == null) return true;

      if (nameHint != null) {
        if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return true;

        if (!GroovyPropertyUtils.isSetterName(nameHint)) return true;

        PsiElement eAssign = place.getParent();
        if (!(eAssign instanceof GrAssignmentExpression assign)) return true;

        if (assign.isOperatorAssignment()) return true;

        PsiElement beanClass = pair.second.resolve();
        if (!(beanClass instanceof PsiClass)) return true;

        PsiMethod setter = null;
        for (PsiMethod method : ((PsiClass)beanClass).findMethodsByName(nameHint, true)) {
          if (method.hasModifierProperty(PsiModifier.STATIC)) continue;

          if (GroovyPropertyUtils.isSimplePropertySetter(method)) {
            setter = method;
            break;
          }
        }

        if (setter == null) return true;

        GrExpression rValue = assign.getRValue();
        if (rValue != null) {
          PsiType rValueType = rValue.getType();
          if (rValueType != null) {
            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiManager.getProject());
            PsiClassType beanRefType = elementFactory.createTypeByFQClassName(RUNTIME_BEAN_REF, resolveScope);
            if (beanRefType.isAssignableFrom(rValueType)) {
              GrMethodWrapper m = GrMethodWrapper.wrap(setter);
              m.getParameterList().clear();
              m.addParameter("value", RUNTIME_BEAN_REF);
              setter = m;
            }
          }
        }

        if (!processor.execute(setter, state)) return false;
      }
    }

    return true;
  }

  private static boolean processRefMethods(PsiScopeProcessor processor,
                                           ResolveState state,
                                           @NotNull PsiClass beanBuilder,
                                           GlobalSearchScope resolveScope) {
    PsiMethod[] refMethods = beanBuilder.getUserData(REF_METHOD_CACHE_KEY);
    if (refMethods == null) {
      GrLightMethodBuilder refMethod1 = new GrLightMethodBuilder(beanBuilder.getManager(), "ref");
      refMethod1.setModifiers(GrModifierFlags.PUBLIC_MASK);
      refMethod1.setReturnType(RUNTIME_BEAN_REF, resolveScope);
      refMethod1.setContainingClass(beanBuilder);
      refMethod1.setMethodKind(REF_METHOD_KIND);

      GrLightMethodBuilder refMethod2 = refMethod1.copy();


      refMethod1.addParameter("beanName", CommonClassNames.JAVA_LANG_STRING);
      refMethod2.addParameter("bean", RUNTIME_BEAN_REF);

      refMethods = new PsiMethod[]{refMethod1, refMethod2};

      beanBuilder.putUserData(REF_METHOD_CACHE_KEY, refMethods);
    }

    for (PsiMethod method : refMethods) {
      if (!processor.execute(method, state)) return false;
    }

    return true;
  }

  private static boolean isClosureAssignedToBeanProperty(GrClosableBlock closure) {
    PsiElement parent = closure.getParent();
    if (!(parent instanceof GrAssignmentExpression assign)) return false;

    if (!(parent.getParent() instanceof PsiFile)) return false;

    return "beans".equals(assign.getLValue().getText());
  }
}
