/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.property;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.core.PsiBeanProperty;
import com.intellij.struts.core.PsiBeanPropertyCache;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitry Avdeev
 */
public class ValidatorFormPropertyReferenceProvider extends FormPropertyReferenceProvider {

  @Override
  protected PropertyReference createReference(PropertyReferenceSet set, int index, TextRange range) {
    return new ValidatorFormPropertyReference(set, index, range, this) {

      @Override
      @NotNull
      protected PsiBeanProperty[] getPropertiesForTag(final boolean forVariants) {
        setSoft(false);
        final PsiElement tag = myValue.getParent().getParent();
        if (tag instanceof XmlTag) {
          final XmlAttribute attribute = ((XmlTag)tag).getAttribute("indexedListProperty", null);
          if (attribute != null) {
            final XmlAttributeValue xmlAttributeValue = attribute.getValueElement();
            if (xmlAttributeValue != null) {
              final PsiReference reference = xmlAttributeValue.getReference();
              if (reference instanceof PropertyReference) {
                final ResolveResult[] resolveResults = ((PropertyReference)reference).multiResolve(false);
                for (ResolveResult result: resolveResults) {
                  final PsiElement element = result.getElement();
                  if (element instanceof PsiMethod) {
                    final PsiType returnType = ((PsiMethod)element).getReturnType();
                    PsiClass beanClass = null;
                    if (returnType instanceof PsiClassType) {
                      final PsiType[] psiTypes = ((PsiClassType)returnType).getParameters();
                      if (psiTypes.length == 1 && psiTypes[0] instanceof PsiClassType) {
                          beanClass = ((PsiClassType)psiTypes[0]).resolve();
                      }
                    } else if (returnType instanceof PsiArrayType) {
                      final PsiType componentType = ((PsiArrayType)returnType).getComponentType();
                      if (componentType instanceof PsiClassType) {
                        beanClass = ((PsiClassType)componentType).resolve();
                      }
                    }
                    if (beanClass != null) {
                      return PsiBeanPropertyCache.getInstance(beanClass.getProject()).getBeanProperties(beanClass);
                    }
                  }
                  setSoft(true);
                }
              }
            }
            return PsiBeanProperty.EMPTY_ARRAY;
          }
        }
        return super.getPropertiesForTag(forVariants);
      }
    };
  }
}
