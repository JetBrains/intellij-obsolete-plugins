// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsPluginWebHelpReferenceProvider;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.grails.pluginSupport.resources.GrailsResourcesReferenceProvider;
import org.jetbrains.plugins.grails.pluginSupport.webflow.WebFlowStateNameReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormNamedArgumentReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.criteria.CriteriaPropertyReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.detachedCriteria.DetachedCriteriaReferenceProvider;
import org.jetbrains.plugins.grails.references.tagSupport.GspTagSupportGspReferenceProvider;
import org.jetbrains.plugins.grails.references.tagSupport.TagAttributeReferenceProvider;
import org.jetbrains.plugins.grails.spring.GrailsSpringMethodReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrConditionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GrailsMethodNamedArgumentReferenceProvider extends PsiReferenceProvider {

  public static volatile GrailsMethodNamedArgumentReferenceProvider instance;

  private final Map<Object, Map<String, List<Pair<Contributor.Provider, Condition<PsiMethod>>>>> MAP =
    new HashMap<>();

  private GrailsMethodNamedArgumentReferenceProvider() {
  }

  public static GrailsMethodNamedArgumentReferenceProvider getInstance() {
    GrailsMethodNamedArgumentReferenceProvider res = instance;
    if (res == null) {
      res = new GrailsMethodNamedArgumentReferenceProvider();

      new GrailsResourcesReferenceProvider().register(res);
      new GormNamedArgumentReferenceProvider().register(res);
      new DetachedCriteriaReferenceProvider().register(res);
      new CriteriaPropertyReferenceProvider().register(res);
      new GrailsSpringMethodReferenceProvider().register(res);
      new GrailsPluginWebHelpReferenceProvider().register(res);

      Condition<PsiMethod> condition = new Contributor.LightMethodCondition(TagLibNamespaceDescriptor.GSP_TAG_METHOD_MARKER);

      for (TagAttributeReferenceProvider provider : GspTagSupportGspReferenceProvider.PROVIDERS) {
        res.register(provider.getAttributeName(), provider, condition, provider.getTagNames());
      }

      res.register(0, WebFlowStateNameReferenceProvider.class, new Contributor.ClassNameCondition("org.codehaus.groovy.grails.webflow.engine.builder.TransitionTo"), "to");

      instance = res;
    }

    return res;
  }

  public void register(@NotNull Object attrNameOrParameterIndex,
                       Class<? extends Contributor.Provider> referenceGenerator,
                       Condition<PsiMethod> methodCondition,
                       String @Nullable ... methods) {
    register(attrNameOrParameterIndex, new ProviderProxy(referenceGenerator), methodCondition, methods);
  }

  public void register(@NotNull Object attrNameOrParameterIndex,
                       Contributor.Provider referenceGenerator,
                       Condition<PsiMethod> methodCondition,
                       String @Nullable ... methods) {
    assert methods == null || methods.length > 0;

    Map<String, List<Pair<Contributor.Provider, Condition<PsiMethod>>>> methodsMap = MAP.get(attrNameOrParameterIndex);
    if (methodsMap == null) {
      methodsMap = new HashMap<>();
      MAP.put(attrNameOrParameterIndex, methodsMap);
    }

    if (methods == null) {
      List<Pair<Contributor.Provider, Condition<PsiMethod>>> list = methodsMap.get(null);
      if (list == null) {
        list = new ArrayList<>();
        methodsMap.put(null, list);
      }

      list.add(Pair.create(referenceGenerator, methodCondition));
    }
    else {
      for (String methodName : methods) {
        List<Pair<Contributor.Provider, Condition<PsiMethod>>> list = methodsMap.get(methodName);
        if (list == null) {
          list = new ArrayList<>();
          methodsMap.put(methodName, list);
        }

        list.add(Pair.create(referenceGenerator, methodCondition));
      }
    }

  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    GrExpression argument = (GrExpression)element;
    PsiElement parent = element.getParent();

    if (parent instanceof GrConditionalExpression) {
      if (((GrConditionalExpression)parent).getCondition() == parent) return PsiReference.EMPTY_ARRAY;
      argument = (GrConditionalExpression)parent;
      parent = parent.getParent();
    }

    if (parent instanceof GrListOrMap) {
      if (!((GrListOrMap)parent).isMap()) {
        argument = (GrListOrMap)parent;
        parent = parent.getParent();
      }
    }

    PsiElement call;
    Object attrNameOrParameterIndex;
    GrNamedArgument namedArgument = null;

    if (parent instanceof GrNamedArgument) {
      namedArgument = (GrNamedArgument)parent;
      attrNameOrParameterIndex = namedArgument.getLabelName();
      if (attrNameOrParameterIndex == null) return PsiReference.EMPTY_ARRAY;
      call = PsiUtil.getCallByNamedParameter(namedArgument);
    }
    else if (parent instanceof GrArgumentList argumentList) {
      attrNameOrParameterIndex = argumentList.getExpressionArgumentIndex(argument);

      call = argumentList.getParent();
    }
    else {
      return PsiReference.EMPTY_ARRAY;
    }

    if (!(call instanceof GrMethodCall)) return PsiReference.EMPTY_ARRAY;

    PsiReference[] res = createReferencesInternal(element, attrNameOrParameterIndex, (GrMethodCall)call, namedArgument);
    if (res.length == 0 && attrNameOrParameterIndex instanceof Integer) {
      res = createReferencesInternal(element, -1, (GrMethodCall)call, null);
    }

    return res;
  }

  private PsiReference @NotNull [] createReferencesInternal(@NotNull PsiElement element,
                                                            @NotNull Object attrNameOrParameterIndex,
                                                            @NotNull GrMethodCall methodCall,
                                                            GrNamedArgument namedArgument) {
    Map<String, List<Pair<Contributor.Provider, Condition<PsiMethod>>>> methodMap = MAP.get(attrNameOrParameterIndex);
    if (methodMap == null) return PsiReference.EMPTY_ARRAY;

    GrExpression invokedExpression = methodCall.getInvokedExpression();
    if (!(invokedExpression instanceof GrReferenceExpression)) return PsiReference.EMPTY_ARRAY;

    String methodName = ((GrReferenceExpression)invokedExpression).getReferenceName();
    if (methodName == null) return PsiReference.EMPTY_ARRAY;

    for (String key : new String[] {methodName, null}) {
      List<Pair<Contributor.Provider, Condition<PsiMethod>>> list = methodMap.get(key);
      if (list != null) {
        for (GroovyResolveResult result : ((GrReferenceExpression)invokedExpression).multiResolve(false)) {
          PsiElement eMethod = result.getElement();
          if (eMethod instanceof PsiMethod method) {

            if (key != null && !key.equals(method.getName())) continue;

            for (Pair<Contributor.Provider, Condition<PsiMethod>> pair : list) {
              if (pair.second.value(method)) {
                PsiReference[] res;
                if (attrNameOrParameterIndex instanceof Integer) {
                  res = pair.first.createRef(element, methodCall, (Integer)attrNameOrParameterIndex, result);
                }
                else {
                  assert namedArgument != null;
                  res = pair.first.createRef(element, namedArgument, result);
                }

                if (res.length > 0) {
                  return res;
                }
              }
            }
          }
        }
      }
    }

    return PsiReference.EMPTY_ARRAY;
  }

  private static final class ProviderProxy extends Contributor.Provider {

    private final Class<? extends Contributor.Provider> myClass;
    private Contributor.Provider myInstance;

    private ProviderProxy(Class<? extends Contributor.Provider> aClass) {
      myClass = aClass;
    }

    private void ensureInit() {
      if (myInstance == null) {
        try {
          myInstance = myClass.newInstance();
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public PsiReference[] createRef(@NotNull PsiElement element,
                                    @NotNull GrNamedArgument namedArgument,
                                    @NotNull GroovyResolveResult resolveResult) {
      ensureInit();
      return myInstance.createRef(element, namedArgument, resolveResult);
    }

    @Override
    public PsiReference[] createRef(@NotNull PsiElement element,
                                    @NotNull GrMethodCall methodCall,
                                    int parameterIndex,
                                    @NotNull GroovyResolveResult resolveResult) {
      ensureInit();
      return myInstance.createRef(element, methodCall, parameterIndex, resolveResult);
    }
  }

  public interface Contributor {
    void register(GrailsMethodNamedArgumentReferenceProvider registrar);

    abstract class Provider {

      protected PsiReference[] createRef(@NotNull PsiElement element, @NotNull GroovyResolveResult resolveResult) {
        throw new UnsupportedOperationException();
      }

      public PsiReference[] createRef(@NotNull PsiElement element,
                                      @NotNull GrNamedArgument namedArgument,
                                      @NotNull GroovyResolveResult resolveResult) {
        return createRef(element, resolveResult);
      }

      public PsiReference[] createRef(@NotNull PsiElement element,
                                      @NotNull GrMethodCall methodCall,
                                      int argumentIndex,
                                      @NotNull GroovyResolveResult resolveResult) {
        return createRef(element, resolveResult);
      }
    }

    class ProviderAdapter extends Provider {
      private static final ProcessingContext CONTEXT = new ProcessingContext();

      private final PsiReferenceProvider myProvider;

      public ProviderAdapter(@NotNull PsiReferenceProvider provider) {
        myProvider = provider;
      }

      @Override
      protected PsiReference[] createRef(@NotNull PsiElement element, @NotNull GroovyResolveResult resolveResult) {
        return myProvider.getReferencesByElement(element, CONTEXT);
      }
    }

    class ClassNameCondition implements Condition<PsiMethod> {
      private final String myClassName;

      public ClassNameCondition(@NotNull String className) {
        myClassName = className;
      }

      @Override
      public boolean value(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return false;

        return myClassName.equals(containingClass.getQualifiedName());
      }
    }

    class LightMethodCondition implements Condition<PsiMethod> {

      private final Object myKey;

      public LightMethodCondition(@NotNull Object key) {
        myKey = key;
      }

      @Override
      public boolean value(PsiMethod method) {
        return GrLightMethodBuilder.checkKind(method, myKey);
      }
    }

    final class ClassNameWithSuperCondition implements Condition<PsiMethod> {
      private final String myClassName;

      private ClassNameWithSuperCondition(String className) {
        myClassName = className;
      }

      @Override
      public boolean value(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return false;

        return InheritanceUtil.isInheritor(containingClass, myClassName);
      }
    }

    class ClassSourceCondition implements Condition<PsiMethod> {
      private final String myClassSource;

      public ClassSourceCondition(String classSource) {
        myClassSource = classSource;
      }

      @Override
      public boolean value(PsiMethod method) {
        return DynamicMemberUtils.isDynamicElement(method, myClassSource);
      }
    }
  }
}
