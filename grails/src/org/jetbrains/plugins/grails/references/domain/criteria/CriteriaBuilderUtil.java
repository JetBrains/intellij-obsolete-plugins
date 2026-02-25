// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.criteria;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.grails.references.domain.namedQuery.NamedQueryDescriptor;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrNewExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrParenthesizedExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CriteriaBuilderUtil {

  public static final String CRITERIA_BUILDER_CLASS = "grails.orm.HibernateCriteriaBuilder";

  public static final String GET_TYPE_FROM_PROPERTY = "";

  private static final Map<String, String> PROJECTION_METHODS_RETURN_TYPES;
  static {
    Map<String, String> map = new HashMap<>();

    map.put("count", CommonClassNames.JAVA_LANG_INTEGER);
    map.put("rowCount", CommonClassNames.JAVA_LANG_INTEGER);
    map.put("countDistinct", CommonClassNames.JAVA_LANG_INTEGER);

    map.put("avg", CommonClassNames.JAVA_LANG_DOUBLE);

    map.put("max", GET_TYPE_FROM_PROPERTY);
    map.put("min", GET_TYPE_FROM_PROPERTY);
    map.put("sum", GET_TYPE_FROM_PROPERTY);
    map.put("property", GET_TYPE_FROM_PROPERTY);
    map.put("groupProperty", GET_TYPE_FROM_PROPERTY);

    PROJECTION_METHODS_RETURN_TYPES = map;
  }

  private CriteriaBuilderUtil() {
  }

  /**
   * Examples:
   *
   * Ddd.withCriteria {
   *   ...
   *   or {
   *     ...
   *     methodCall()
   *     ...
   *   }
   *   ...
   * }
   *
   * By qualifier only:
   *
   * (Ddd.createCriteria() {}).methodCall()
   *
   *
   */
  public static @Nullable PsiClass findDomainClassByMethodCall(@NotNull GrMethodCall methodCall, boolean byQualifierOnly) {
    GrExpression eInvokedExpression = methodCall.getInvokedExpression();
    if (!(eInvokedExpression instanceof GrReferenceExpression invokedExpression)) return null;

    GrExpression qualifier = invokedExpression.getQualifierExpression();

    if (qualifier != null) {
      return findDomainClassByBuilderExpression(qualifier);
    }

    if (byQualifierOnly) return null;

    return checkCriteriaClosure(PsiTreeUtil.getParentOfType(methodCall, GrClosableBlock.class));
  }

  public static boolean isCriteriaBuilderMethod(@Nullable PsiMethod method) {
    if (method == null) return false;
    if (CriteriaBuilderImplicitMemberContributor.isMine(method)) return true;

    PsiClass aClass = method.getContainingClass();
    return aClass != null && CRITERIA_BUILDER_CLASS.equals(aClass.getQualifiedName());
  }

  public static @Nullable PsiClass findDomainClassByBuilderExpression(@Nullable GrExpression expression) {
    List<GrReferenceExpression> visited = new SmartList<>();

    while (true) {
      if (expression instanceof GrReferenceExpression ref) {

        if (visited.contains(ref)) return null;
        visited.add(ref);

        PsiElement resolve = ref.resolve();
        if (resolve instanceof GrVariable) {
          GrExpression initializer = ((GrVariable)resolve).getInitializerGroovy();
          if (initializer == null) return null;
          expression = initializer;

          continue;
        }

        return null;
      }

      if (expression instanceof GrMethodCall) {
        GrExpression invokedExpression = ((GrMethodCall)expression).getInvokedExpression();
        if (!(invokedExpression instanceof GrReferenceExpression)) return null;

        String methodName = ((GrReferenceExpression)invokedExpression).getReferenceName();
        if (!"createCriteria".equals(methodName)) return null;

        PsiMethod method = ((GrMethodCall)expression).resolveMethod();
        if (!GrLightMethodBuilder.checkKind(method, DomainDescriptor.DOMAIN_DYNAMIC_METHOD)) return null;
        //noinspection ConstantConditions

        return ((GrLightMethodBuilder)method).getData();
      }

      if (expression instanceof GrNewExpression) {
        PsiType type = expression.getType();
        if (!InheritanceUtil.isInheritor(type, CRITERIA_BUILDER_CLASS)) return null;

        GrArgumentList argumentList = ((GrNewExpression)expression).getArgumentList();
        if (argumentList == null) return null;

        for (GrExpression argument : argumentList.getExpressionArguments()) {
          PsiType argumentType = argument.getType();
          if (argumentType instanceof PsiClassType) {
            PsiClass aClass = ((PsiClassType)argumentType).resolve();
            if (aClass != null && CommonClassNames.JAVA_LANG_CLASS.equals(aClass.getQualifiedName())) {
              PsiType[] parameters = ((PsiClassType)argumentType).getParameters();
              if (parameters.length == 1 && parameters[0] instanceof PsiClassType) {
                PsiClass domainClass = ((PsiClassType)parameters[0]).resolve();
                if (GormUtils.isGormBean(domainClass)) return domainClass;
              }
            }
          }
        }

        return null;
      }

      if (expression instanceof GrParenthesizedExpression) {
        expression = ((GrParenthesizedExpression)expression).getOperand();
        continue;
      }

      return null;
    }
  }

  public static @Nullable PsiClass checkCriteriaClosure(@Nullable GrClosableBlock closure) {
    while (closure != null) {
      PsiElement eMethodCall = closure.getParent();
      if (eMethodCall instanceof GrArgumentList) eMethodCall = eMethodCall.getParent();

      if (!(eMethodCall instanceof GrMethodCall methodCall)) return null;

      GrExpression ie = methodCall.getInvokedExpression();
      if (!(ie instanceof GrReferenceExpression ieRef)) {
        return findDomainClassByBuilderExpression(ie); // case: (Ddd.createCriteria()) { ... }
      }

      if (GormUtils.isNamedQueryDeclaration(ieRef)) {
        if (ieRef.resolve() == null) {
          PsiClass aClass = PsiTreeUtil.getParentOfType(ieRef, PsiClass.class);
          assert aClass != null;
          return aClass;
        }
      }

      PsiElement resolve = ieRef.resolve();
      if (!(resolve instanceof PsiMethod method)) {
        return findDomainClassByBuilderExpression(ie);
      }

      if (method instanceof GrLightMethodBuilder lightMethod) {
        Object kind = lightMethod.getMethodKind();

        if ((DomainDescriptor.DOMAIN_DYNAMIC_METHOD.equals(kind) && "withCriteria".equals(method.getName()))
            || CriteriaClosureMemberContributor.TO_MANY_RELATIONSHIP_MARKER.equals(kind)) {
          return lightMethod.getData();
        }

        if (NamedQueryDescriptor.NAMED_QUERY_METHOD_MARKER.equals(kind)) {
          return lightMethod.<NamedQueryDescriptor>getData().getDomainClass();
        }
      }

      PsiClass namedCriteriaProxyClass = method.getContainingClass();
      if (namedCriteriaProxyClass != null
          && GormUtils.NAMED_CRITERIA_PROXY_CLASS_NAME.equals(namedCriteriaProxyClass.getQualifiedName())) {
        NamedQueryDescriptor queryDescriptor = GormUtils.getQueryDescriptorByProxyMethod(methodCall);
        if (queryDescriptor != null) {
          return queryDescriptor.getDomainClass();
        }
      }

      if (!isCriteriaBuilderMethod(method)) return null;

      GrExpression qualifierExpression = ieRef.getQualifierExpression();
      if (qualifierExpression != null) {
        return findDomainClassByBuilderExpression(qualifierExpression);
      }

      closure = PsiTreeUtil.getParentOfType(closure, GrClosableBlock.class);
    }

    return null;
  }

  public static @NotNull PsiType getResultType(@NotNull PsiClass domainClass, @NotNull GrClosableBlock closure) {
    PsiType res = getResultType0(domainClass, closure);
    if (res == null) {
      res = PsiType.getJavaLangObject(domainClass.getManager(), domainClass.getResolveScope());
    }

    return res;
  }

  private static @Nullable PsiType getResultType0(PsiClass domainClass, GrClosableBlock closure) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(domainClass.getProject());
    PsiElementFactory factory = facade.getElementFactory();

    GrClosableBlock projectionsBlock = null;

    // Find projections block.
    for (PsiElement e = closure.getLastChild(); e != null; e = e.getPrevSibling()) {
      if (e instanceof GrMethodCall methodCall) {
        GrExpression ie = methodCall.getInvokedExpression();
        if (ie instanceof GrReferenceExpression refExpr) {
          if (!refExpr.isQualified() && "projections".equals(refExpr.getReferenceName())) {
            GrClosableBlock c = GrailsUtils.getClosureArgument(methodCall);
            if (c != null) {
                projectionsBlock = c;
                break;
            }
          }
        }
      }
    }

    if (projectionsBlock == null) {
      return factory.createType(domainClass);
    }

    String firstType = null;
    GrMethodCall firstMethodCall = null;

    for (PsiElement e = projectionsBlock.getFirstChild(); e != null; e = e.getNextSibling()) {
      if (e instanceof LeafPsiElement || e instanceof GrParameterList) continue;

      if (!(e instanceof GrMethodCall methodCall)) return null;

      GrExpression invokedExpression = methodCall.getInvokedExpression();
      if (!(invokedExpression instanceof GrReferenceExpression refExpr)) return null;
      if (refExpr.isQualified()) return null;

      String methodName = refExpr.getReferenceName();

      if ("distinct".equals(methodName)) {
        if (firstType != null) return null;

        for (PsiElement element = methodCall.getNextSibling(); element != null; element = element.getNextSibling()) {
          if (!(element instanceof LeafPsiElement)) return null;
        }

        GrArgumentList argumentList = methodCall.getArgumentList();
        GrExpression[] expressionArguments = argumentList.getExpressionArguments();
        if (expressionArguments.length == 0) return null;
        PsiType type = expressionArguments[0].getType();
        if (type != null && type.equalsToText(CommonClassNames.JAVA_LANG_STRING)) {
          firstType = "";
          firstMethodCall = methodCall;
          break;
        }

        return new PsiArrayType(factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT, domainClass.getResolveScope()));
      }

      String sMethodType = PROJECTION_METHODS_RETURN_TYPES.get(methodName);
      if (sMethodType == null) {
        return factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT, domainClass.getResolveScope());
      }

      if (firstType == null) {
        firstType = sMethodType;
        firstMethodCall = methodCall;
      }
      else {
        return new PsiArrayType(factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT, domainClass.getResolveScope()));
      }
    }

    if (firstType == null) {
      return factory.createType(domainClass);
    }

    if (firstType.isEmpty()) {
      GrArgumentList argumentList = firstMethodCall.getArgumentList();
      GrExpression[] arguments = argumentList.getExpressionArguments();
      if (arguments.length > 0 && arguments[0] instanceof GrLiteralImpl) {
        Object oValue = ((GrLiteralImpl)arguments[0]).getValue();
        if (oValue instanceof String value) {
          Pair<PsiType,PsiElement> pair = DomainDescriptor.getPersistentProperties(domainClass).get(value);
          if (pair != null) {
            return TypesUtil.boxPrimitiveType(pair.first, domainClass.getManager(), domainClass.getResolveScope());
          }
        }
      }

      firstType = CommonClassNames.JAVA_LANG_OBJECT;
    }

    return factory.createTypeByFQClassName(firstType, domainClass.getResolveScope());
  }

}
