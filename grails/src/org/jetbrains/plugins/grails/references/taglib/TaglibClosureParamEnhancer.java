// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.taglib;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.AbstractClosureParameterEnhancer;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;

/**
 * @author user
 */
public final class TaglibClosureParamEnhancer extends AbstractClosureParameterEnhancer {
  @Override
  protected PsiType getClosureParameterType(@NotNull GrFunctionalExpression expression, int index) {
    if (isTaglibClosure(expression)) {
      if (index == 0) {
        return JavaPsiFacade.getElementFactory(expression.getProject()).createTypeByFQClassName(CommonClassNames.JAVA_UTIL_MAP, expression.getResolveScope());
      }

      if (index == 1) {
        return JavaPsiFacade.getElementFactory(expression.getProject()).createTypeByFQClassName(GroovyCommonClassNames.GROOVY_LANG_CLOSURE, expression.getResolveScope());
      }
    }

    return null;
  }

  private static boolean isTaglibClosure(@NotNull GrFunctionalExpression expression) {
    PsiElement parent = expression.getParent();
    if (!(parent instanceof GrField field)) return false;

    PsiClass aClass = field.getContainingClass();
    if (aClass == null) return false;

    String className = aClass.getQualifiedName();
    if (className == null || !className.endsWith(GrailsArtifact.TAGLIB.suffix)) return false;

    if (className.startsWith(GspTagLibUtil.DYNAMIC_TAGLIB_PACKAGE)) {
      return true;
    }

    return GrailsArtifact.TAGLIB.isInstance(aClass);
  }
}
