// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;

/**
 * @author Maxim.Medvedev
 */
public class GrailsHasManyBelongsToReferencesProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(final @NotNull PsiElement element, @NotNull ProcessingContext processingContext) {
    assert element instanceof GrArgumentLabel;

    final PsiElement context = element.getParent().getParent();
    if (!(context instanceof GrListOrMap)) return PsiReference.EMPTY_ARRAY;

    final PsiElement parent = context.getParent();

    if (!(parent instanceof GrField)) return PsiReference.EMPTY_ARRAY;

    String parentFiledName = ((GrField)parent).getName();

    if (!DomainClassRelationsInfo.MAPPED_BY.equals(parentFiledName) &&
        !DomainClassRelationsInfo.HAS_MANY_NAME.equals(parentFiledName) &&
        !DomainClassRelationsInfo.RELATES_TO_MANY_NAME.equals(parentFiledName) &&
        !DomainClassRelationsInfo.HAS_ONE_NAME.equals(parentFiledName) &&
        !DomainClassRelationsInfo.BELONGS_TO_NAME.equals(parentFiledName)) {
      return PsiReference.EMPTY_ARRAY;
    }

    if (!((GrField)parent).hasModifierProperty(PsiModifier.STATIC)) return PsiReference.EMPTY_ARRAY;

    final PsiClass psiClass = ((GrField)parent).getContainingClass();

    if (!GormUtils.isGormBean(psiClass)) return PsiReference.EMPTY_ARRAY;
    assert psiClass != null;

    PsiReference reference = new PsiReference() {
      @Override
      public @NotNull PsiElement getElement() {
        return element;
      }

      @Override
      public @NotNull TextRange getRangeInElement() {
        return new TextRange(0, element.getTextLength());
      }

      @Override
      public PsiElement resolve() {
        Pair<PsiType, PsiElement> pair = DomainDescriptor.getDescriptor(psiClass).getPersistentProperties()
          .get(((GrArgumentLabel)element).getName());
        if (pair != null) {
          PsiElement e = pair.second;
          if (!(e instanceof LightElement)) { // Don't resolve light elements, Light elements will be returned from GrailsSyntheticFieldDeclarationSearcher
            return e;
          }
        }

        return null;
      }

      @Override
      public @NotNull String getCanonicalText() {
        return ((GrArgumentLabel)element).getName();
      }

      @Override
      public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        String name = newElementName;

        String propertyName = GroovyPropertyUtils.getPropertyNameByGetterName(newElementName, false);
        if (propertyName != null && resolve() instanceof PsiMethod) {
          name = propertyName;
        }

        return ((GrArgumentLabel)element).setName(name);
      }

      @Override
      public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        return ((GrArgumentLabel)element).bindToElement(element);
      }

      @Override
      public boolean isReferenceTo(@NotNull PsiElement element) {
        return resolve() == element;
      }

      @Override
      public boolean isSoft() {
        return false;
      }
    };

    return new PsiReference[]{reference};
  }
}
