// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api;

import com.intellij.psi.PsiClass;
import com.intellij.psi.SyntheticElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

public interface GrGspClass extends PsiClass, SyntheticElement, GroovyPsiElement {

  @Override
  GrField @NotNull [] getFields();

  @Override
  GrMethod @NotNull [] getMethods();

  @NotNull
  GrGspRunMethod getRunMethod();
}
