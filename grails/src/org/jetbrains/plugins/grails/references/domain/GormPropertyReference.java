// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.util.PsiFieldReference;
import org.jetbrains.plugins.groovy.lang.completion.CompleteReferenceExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GormPropertyReference extends PsiFieldReference {

  protected final PsiClass myDomainClass;

  public GormPropertyReference(PsiElement element, boolean soft, PsiClass domainClass) {
    super(element, soft);
    this.myDomainClass = domainClass;
  }

  @Override
  public PsiElement resolve() {
    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(myDomainClass);

    String propertyName = getValue();
    Pair<PsiType,PsiElement> pair = descriptor.getPersistentProperties().get(propertyName);
    return Pair.getSecond(pair);
  }

  @Override
  public Object @NotNull [] getVariants() {
    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(myDomainClass);

    Map<String,Pair<PsiType,PsiElement>> map = descriptor.getPersistentProperties();

    List res = new ArrayList();

    for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : map.entrySet()) {
      if (isValidForCompletion(entry.getKey(), entry.getValue().first, descriptor)) {
        res.add(CompleteReferenceExpression.createPropertyLookupElement(entry.getKey(), entry.getValue().first));
      }
    }

    return res.toArray();
  }

  protected boolean isValidForCompletion(String fieldName, PsiType type, DomainDescriptor descriptor) {
    return true;
  }

}
