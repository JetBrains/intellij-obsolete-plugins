// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GormPropertyReferenceUnique extends GormPropertyReference {

  private volatile List<String> myExistsVariants;

  public GormPropertyReferenceUnique(PsiElement element, boolean soft, PsiClass domainClass) {
    super(element, soft, domainClass);
  }

  @Override
  protected boolean isValidForCompletion(String fieldName, PsiType type, DomainDescriptor descriptor) {
    if (myExistsVariants == null) {
      PsiElement parent = getElement().getParent();
      if (parent instanceof GrListOrMap) {
        List<String> res = new ArrayList<>();

        for (GrExpression expression : ((GrListOrMap)parent).getInitializers()) {
          if (expression instanceof GrLiteralImpl) {
            Object value1 = ((GrLiteralImpl)expression).getValue();
            if (value1 instanceof String) {
              res.add((String)value1);
            }
          }
        }

        myExistsVariants = res;
      }
      else {
        myExistsVariants = Collections.emptyList();
      }
    }

    return !myExistsVariants.contains(fieldName);
  }
}
