// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.namedQuery;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public final class GormNamedQueryDeclarationSearcher extends PomDeclarationSearcher {
  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!GormUtils.isNamedQueryDeclaration(element)) return;

    PsiClass aClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    assert aClass != null;

    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(aClass);

    NamedQueryDescriptor queryDescriptor = descriptor.getNamedQueries().get(((GrReferenceExpression)element).getReferenceName());

    if (queryDescriptor == null) return;

    consumer.consume(queryDescriptor.getVariable());
  }
}
