// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.criteria;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.typing.GrTypeCalculator;

final class CriteriaReturnTypeCalculator implements GrTypeCalculator<GrMethodCall> {
  @Override
  public @Nullable PsiType getType(@NotNull GrMethodCall callExpression) {
    PsiMethod method = callExpression.resolveMethod();

    GrExpression[] arguments = PsiUtil.getAllArguments(callExpression);
    if (arguments.length == 0) return null;

    int i = 0;
    if (arguments[0] == null || InheritanceUtil.isInheritor(arguments[0].getType(), CommonClassNames.JAVA_UTIL_MAP)) {
      i++;
    }

    if (i != arguments.length - 1 || !(arguments[i] instanceof GrClosableBlock closure)) return null;

    GrExpression invokedExpression = callExpression.getInvokedExpression();

    if (invokedExpression instanceof GrReferenceExpression) {
      if (method != null) {
        if (CriteriaBuilderImplicitMemberContributor.isMine(method)) {
          PsiType returnType = method.getReturnType();
          assert returnType != null;

          boolean isList = returnType.equalsToText(CommonClassNames.JAVA_UTIL_LIST);
          if (!isList && !returnType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT)) return null;

          final PsiClass domainClass;
          if (((GrReferenceExpression)invokedExpression).isImplicitCallReceiver()) {
            domainClass = CriteriaBuilderUtil.findDomainClassByBuilderExpression(invokedExpression);
          }
          else {
            domainClass = CriteriaBuilderUtil.findDomainClassByMethodCall(callExpression, true);
          }
          if (domainClass == null) return null;

          return createType(isList, domainClass, closure);
        }

        if (GrLightMethodBuilder.checkKind(method, DomainDescriptor.DOMAIN_DYNAMIC_METHOD) &&
            "withCriteria".equals(method.getName())) {
          PsiClass domainClass = ((GrLightMethodBuilder)method).getData();
          if (!GormUtils.isGormBean(domainClass)) return null;
          assert domainClass != null;

          boolean isList = true;
          for (GrNamedArgument namedArgument : PsiUtil.getFirstMapNamedArguments(callExpression)) {
            if ("uniqueResult".equals(namedArgument.getLabelName())) {
              GrExpression expression = namedArgument.getExpression();
              if (expression instanceof GrLiteralImpl && Boolean.TRUE.equals(((GrLiteralImpl)expression).getValue())) {
                isList = false;
              }
              break;
            }
          }

          return createType(isList, domainClass, closure);
        }

        return null;
      }
    }

    if (!InheritanceUtil.isInheritor(invokedExpression.getType(), CriteriaBuilderUtil.CRITERIA_BUILDER_CLASS)) return null;

    PsiClass domainClass = CriteriaBuilderUtil.findDomainClassByBuilderExpression(invokedExpression);
    if (domainClass == null) return null;

    return createType(true, domainClass, closure);
  }

  private static @Nullable PsiType createType(boolean isList, @NotNull PsiClass domainClass, @NotNull GrClosableBlock closure) {
    PsiType elementType = CriteriaBuilderUtil.getResultType(domainClass, closure);

    if (!isList) return elementType;

    JavaPsiFacade facade = JavaPsiFacade.getInstance(domainClass.getProject());

    PsiClass listClass = facade.findClass(CommonClassNames.JAVA_UTIL_LIST, domainClass.getResolveScope());
    if (listClass == null) return null;

    return facade.getElementFactory().createType(listClass, elementType);
  }
}
