// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.util.PsiFieldReference;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.completion.CompleteReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.plugins.groovy.transformations.impl.GroovyObjectTransformationSupport.isGroovyObjectSupportMethod;

public class GormPropertiesListReferenceReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    final GrListOrMap list = (GrListOrMap)element.getParent();
    PsiField field = (PsiField)list.getParent();

    final PsiClass containingClass = field.getContainingClass();
    if (!GormUtils.isGormBean(containingClass)) return PsiReference.EMPTY_ARRAY;
    assert containingClass != null;

    return new PsiReference[]{
      new PsiFieldReference(element, false) {
        @Override
        public PsiElement resolve() {
          String value = getValue();
          PsiMethod getter = GroovyPropertyUtils.findPropertyGetter(containingClass, value, false, true);

          return GrailsUtils.toField(getter);
        }

        @Override
        public Object @NotNull [] getVariants() {
          Map<String, PsiType> fields = new HashMap<>();

          for (PsiMethod method : containingClass.getAllMethods()) {
            if (GroovyPropertyUtils.isSimplePropertyGetter(method) &&
                !isGroovyObjectSupportMethod(method) &&
                !(method.getNavigationElement() instanceof LightElement) &&
                !(method.getContainingClass() instanceof ClsClassImpl) &&
                !method.hasModifierProperty(PsiModifier.STATIC)) {
              fields.put(GroovyPropertyUtils.getPropertyName(method), PsiUtil.getSmartReturnType(method));
            }
          }

          GrailsPsiUtil.removeValuesFromList(fields.keySet(), list);

          filterFields(containingClass, fields);

          Object[] res = new Object[fields.size()];

          int i = 0;
          for (Map.Entry<String, PsiType> entry : fields.entrySet()) {
            res[i++] = CompleteReferenceExpression.createPropertyLookupElement(entry.getKey(), entry.getValue());
          }

          return res;
        }
      }
    };
  }

  protected void filterFields(PsiClass domainClass, Map<String, PsiType> fields) {

  }
}
