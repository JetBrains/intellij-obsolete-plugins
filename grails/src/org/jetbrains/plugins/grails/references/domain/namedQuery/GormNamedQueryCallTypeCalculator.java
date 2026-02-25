// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.namedQuery;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PairFunction;
import org.jetbrains.plugins.grails.references.domain.criteria.CriteriaBuilderUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class GormNamedQueryCallTypeCalculator implements PairFunction<GrMethodCall, PsiMethod, PsiType> {
  @Override
  public PsiType fun(GrMethodCall methodCall, PsiMethod method) {
    GrExpression[] allArguments = PsiUtil.getAllArguments(methodCall);

    GrClosableBlock closure = null;
    for (int i = allArguments.length; --i >= 0; ) {
      if (allArguments[i] instanceof GrClosableBlock) {
        closure = (GrClosableBlock)allArguments[i];
        break;
      }
    }

    if (closure == null) return null;

    NamedQueryDescriptor queryDescriptor = ((GrLightMethodBuilder)method).getData();

    PsiClass domainClass = queryDescriptor.getDomainClass();

    PsiType resultElementType = CriteriaBuilderUtil.getResultType(domainClass, closure);

    JavaPsiFacade facade = JavaPsiFacade.getInstance(domainClass.getProject());

    GlobalSearchScope resolveScope = domainClass.getResolveScope();
    PsiClass listClass = facade.findClass(CommonClassNames.JAVA_UTIL_LIST, resolveScope);
    if (listClass == null) {
      return facade.getElementFactory().createTypeByFQClassName(CommonClassNames.JAVA_UTIL_LIST, resolveScope);
    }

    return facade.getElementFactory().createType(listClass, resultElementType);
  }
}
