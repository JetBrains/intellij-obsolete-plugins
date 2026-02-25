// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.refactoring.rename.RenameGrFieldProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DomainFieldRenameProcessor extends RenameGrFieldProcessor {

  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    if (!(element instanceof GrField field)) return false;

    PsiClass domainClass = field.getContainingClass();

    if (!GormUtils.isGormBean(domainClass)) return false;
    assert domainClass != null;

    return DomainDescriptor.getPersistentProperties(domainClass).containsKey(field.getName());
  }

  @Override
  public @NotNull Collection<PsiReference> findReferences(@NotNull PsiElement element,
                                                          @NotNull SearchScope searchScope,
                                                          boolean searchInCommentsAndStrings) {
    final List<PsiReference> res = new ArrayList<>(super.findReferences(element, searchScope, searchInCommentsAndStrings));

    final GrField field = (GrField)element;
    PsiClass domainClass = field.getContainingClass();
    assert domainClass != null;

    String name = field.getName();
    assert name != null;
    final String capName = StringUtil.capitalize(name);

    SearchScope useScope = domainClass.getUseScope().intersectWith(searchScope);

    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);
    if (descriptor.getHasMany().containsKey(name)) {
      for (PsiMethod method : descriptor.getAddToAndRemoveFromMethods()) {
        String methodName = method.getName();

        if (methodName.endsWith(capName)) {
          for (PsiReference reference : ReferencesSearch.search(method, useScope).findAll()) {
            if (!(reference instanceof GrReferenceExpression grRef)) continue;

            PsiElement nameElement = grRef.getReferenceNameElement();
            if (nameElement != null && methodName.equals(nameElement.getText())) {
              TextRange range = TextRange.from(nameElement.getStartOffsetInParent() + methodName.length() - capName.length(), capName.length());
              res.add(new FinderMethodFieldReference(grRef, range, field));
            }
          }
        }
      }
    }

    ReferencesSearch.search(domainClass, useScope, false).forEach(new Processor<>() {

      private void processReference(PsiReference psiReference) {
        if (!(psiReference instanceof GrReferenceExpression)) return;

        PsiElement parent = ((GrReferenceExpression)psiReference).getParent();

        if (!(parent instanceof GrReferenceExpression parentRef)) return;

        PsiElement methodCall = parent.getParent();

        if (!(methodCall instanceof GrMethodCall)) return;

        if (parentRef.getDotTokenType() != GroovyTokenTypes.mDOT) return;

        DomainClassUtils.FinderMethod finderMethod = DomainClassUtils.parseFinderMethod(parentRef.getReferenceName());
        if (finderMethod == null) return;

        boolean hasProperty = false;
        for (DomainClassUtils.Condition condition : finderMethod.getConditions()) {
          if (condition.getFieldName().equals(capName)) {
            hasProperty = true;
            break;
          }
        }
        if (!hasProperty) return;

        PsiElement method = ((GrMethodCall)methodCall).resolveMethod();
        if (!GrLightMethodBuilder.checkKind(method, DomainMembersProvider.FINDER_METHOD_MARKER)) return;

        PsiElement nameElement = parentRef.getReferenceNameElement();
        if (nameElement == null) return;

        int i = finderMethod.getPrefix().length();
        for (DomainClassUtils.Condition condition : finderMethod.getConditions()) {
          if (!condition.getFieldName().equals(capName)) continue;

          res.add(new FinderMethodFieldReference(parentRef,
                                                 TextRange.from(nameElement.getStartOffsetInParent() + i, capName.length()),
                                                 field));
          i += condition.getLength();
          if (finderMethod.getOperator() != null) {
            i += finderMethod.getOperator().length();
          }
        }
      }

      @Override
      public boolean process(PsiReference psiReference) {
        processReference(psiReference);
        return true;
      }
    });

    return res;
  }

  @Override
  public void renameElement(@NotNull PsiElement psiElement,
                            @NotNull String newName,
                            UsageInfo @NotNull [] usages,
                            @Nullable RefactoringElementListener listener) throws IncorrectOperationException {
    List<UsageInfo> usagesList = new ArrayList<>(usages.length);

    for (UsageInfo usage : usages) {
      PsiReference psiReference = usage.getReference();
      if (psiReference instanceof FinderMethodFieldReference) {
        GrReferenceExpression ref = (GrReferenceExpression)psiReference.getElement();

        PsiField field = (PsiField)psiReference.resolve();
        assert field != null;

        String oldCapName = StringUtil.capitalize(field.getName());
        String newCapName = StringUtil.capitalize(newName);

        String oldMethodName = ref.getReferenceName();
        assert oldMethodName != null;

        if (isAddToOrRemoveFromName(oldMethodName, oldCapName)) {
          ref.handleElementRename(oldMethodName.substring(0, oldMethodName.length() - oldCapName.length()) + newCapName);
        }
        else {
          DomainClassUtils.FinderMethod finderMethod = DomainClassUtils.parseFinderMethod(oldMethodName);
          if (finderMethod != null) {
            for (DomainClassUtils.Condition condition : finderMethod.getConditions()) {
              if (condition.getFieldName().equals(oldCapName)) {
                condition.setFieldName(newCapName);
              }
            }

            ref.handleElementRename(finderMethod.toString());
          }
        }
      }
      else {
        usagesList.add(usage);
      }
    }

    super.renameElement(psiElement, newName,
                        usagesList.size() == usages.length ? usages : usagesList.toArray(UsageInfo.EMPTY_ARRAY), listener);
  }

  private static class FinderMethodFieldReference extends PsiReferenceBase<PsiElement> {
    private final GrField myField;

    FinderMethodFieldReference(GrReferenceExpression refExpr, TextRange range, GrField field) {
      super(refExpr, range);
      myField = field;
    }

    @Override
    public PsiElement resolve() {
      return myField;
    }
  }
  
  private static boolean isAddToOrRemoveFromName(String methodName, String capName) {
    return stringEquals(methodName, "addTo", capName) || stringEquals(methodName, "removeFrom", capName);
  }
  
  private static boolean stringEquals(String s, String prefix, String suffix) {
    return s.length() == prefix.length() + suffix.length() && s.startsWith(prefix) && s.endsWith(suffix);
  }
}
