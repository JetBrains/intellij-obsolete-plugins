// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.util.PsiFieldReference;
import org.jetbrains.plugins.grails.util.DelegateReference;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.completion.CompleteReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrMapType;

import java.util.Map;
import java.util.Set;

public class GspSelectTagSupport extends TagAttributeReferenceProvider {

  protected GspSelectTagSupport(String attributeName) {
    super(attributeName, "g", new String[]{"select"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         final @NotNull GspTagWrapper gspTagWrapper) {
    return new PsiReference[]{new DelegateReference(element) {
      @Override
      protected PsiReference createDelegate() {
        return createDelegateReference(myElement, gspTagWrapper);
      }
    }};
  }

  private static @Nullable PsiReference createDelegateReference(@NotNull PsiElement element, @NotNull GspTagWrapper gspTagWrapper) {
    PsiType collectionType = gspTagWrapper.getAttributeValueType("from");
    if (collectionType == null) return null;

    final PsiType type = GrailsPsiUtil.getElementTypeByCollectionType(collectionType, element.getProject(), element.getResolveScope());

    final PsiClass aClass = PsiTypesUtil.getPsiClass(type);
    if (aClass == null) return null;

    if (InheritanceUtil.isInheritor(aClass, CommonClassNames.JAVA_UTIL_MAP)) {
      if (type instanceof GrMapType) {
        return new PsiReferenceBase<>(element, true) {
          @Override
          public PsiElement resolve() {
            return null;
          }

          @Override
          public Object @NotNull [] getVariants() {
            Set<String> keys = ((GrMapType)type).getStringKeys();
            return ArrayUtilRt.toStringArray(keys);
          }
        };
      }

      return null;
    }

    return new PsiFieldReference(element, false) {
      @Override
      public PsiElement resolve() {
        String value = getValue();
        PsiElement accessor = PropertyUtilBase.findPropertyGetter(aClass, value, false, true);
        if (accessor == null) {
          accessor = PropertyUtilBase.findPropertyField(aClass, value, false);
        }

        return accessor;
      }

      @Override
      public Object @NotNull [] getVariants() {
        Map<String, PsiMethod> propertyGetters = PropertyUtilBase.getAllProperties(aClass, false, true);

        Object[] res = new Object[propertyGetters.size()];

        int i = 0;
        for (Map.Entry<String, PsiMethod> entry : propertyGetters.entrySet()) {
          PsiMethod getter = entry.getValue();
          res[i++] = CompleteReferenceExpression.createPropertyLookupElement(getter, null, null);
        }

        return res;
      }
    };
  }

}
