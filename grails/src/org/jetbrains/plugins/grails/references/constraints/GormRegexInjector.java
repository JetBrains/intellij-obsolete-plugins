// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collections;
import java.util.List;

public final class GormRegexInjector implements MultiHostInjector {

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    GrNamedArgument namedArgument = (GrNamedArgument)context;

    if (!"matches".equals(namedArgument.getLabelName())) return;

    PsiField field = PsiTreeUtil.getParentOfType(namedArgument, PsiField.class);

    if (field == null || !"constraints".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) return;

    GrExpression expression = namedArgument.getExpression();
    if (!(expression instanceof GrLiteral) || !(((GrLiteral)expression).isString())) return;

    GrCall call = PsiUtil.getCallByNamedParameter(namedArgument);
    if (call == null) return;

    if (!GrailsConstraintsUtil.isConstraintsMethod(call.resolveMethod())) return;

    TextRange range = ElementManipulators.getValueTextRange(expression);

    registrar.startInjecting(RegExpLanguage.INSTANCE).addPlace(null, null, (GrLiteralImpl)expression, range).doneInjecting();
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(GrNamedArgument.class);
  }
}
