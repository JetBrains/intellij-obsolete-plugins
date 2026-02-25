// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyMapContentProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

import java.util.Collection;
import java.util.Collections;

final class GrailsFlashMapContentProvider extends GroovyMapContentProvider {
  @Override
  protected Collection<String> getKeyVariants(@NotNull GrExpression qualifier, @Nullable PsiElement resolve) {
    if (InheritanceUtil.isInheritor(qualifier.getType(), "org.codehaus.groovy.grails.web.servlet.FlashScope")) {
      return Collections.singletonList("message");
    }

    return Collections.emptyList();
  }
}
