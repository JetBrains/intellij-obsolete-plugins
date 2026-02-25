// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiTarget;
import com.intellij.spring.model.jam.stereotype.CustomSpringComponent;
import com.intellij.spring.model.jam.stereotype.CustomSpringComponentPsiTarget;
import org.jetbrains.annotations.NotNull;

public class GrailsCustomSpringComponent extends CustomSpringComponent {

  private final String myBeanName;

  public GrailsCustomSpringComponent(@NotNull PsiClass psiClass, @NotNull String beanName) {
    super(psiClass);
    myBeanName = beanName;
  }

  @Override
  public String getBeanName() {
    return myBeanName;
  }

  @Override
  public PsiTarget getPsiTarget() {
    return new CustomSpringComponentPsiTarget(this);
  }
}
