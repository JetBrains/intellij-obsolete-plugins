// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.util.SafeReference;
import org.jetbrains.plugins.groovy.lang.completion.CompleteReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GspFieldValueTagSupport extends TagAttributeReferenceProvider {
  private static final Key<Pair<PsiReference[], Long>> ourCacheKey = Key.create(GspFieldValueTagSupport.class.getName());

  public GspFieldValueTagSupport() {
    super("field", "g", new String[]{"fieldValue", "fieldError", "hasErrors", "eachError"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    PsiFile file = element.getContainingFile();
    long modCount = file.getManager().getModificationTracker().getModificationCount() +
                    (file.isPhysical() ? 0 : file.getModificationStamp());

    // we need to return same references on several invocations,
    // because Find Usages queries them separately for each text occurrence inside the attribute,
    // and the references know about each other and update offsets on rename
    Pair<PsiReference[], Long> cached = element.getUserData(ourCacheKey);
    if (cached == null || cached.second.longValue() != modCount) {
      cached = ((UserDataHolderEx)element).putUserDataIfAbsent(
        ourCacheKey, Pair.create(createFieldReferences(element, text, offset, gspTagWrapper), modCount));
    }

    return cached.first.clone();
  }

  private static PsiReference[] createFieldReferences(PsiElement element, String text, int offset, GspTagWrapper gspTagWrapper) {
    PsiType beanType = gspTagWrapper.getAttributeValueType("bean");
    if (beanType == null) return PsiReference.EMPTY_ARRAY;

    PsiType type = beanType;

    List<PsiReference> res = new ArrayList<>();

    int i = 0;
    do {
      PsiClass aClass = PsiTypesUtil.getPsiClass(type);
      if (aClass == null) break;

      int i2 = text.indexOf('.', i);
      if (i2 == -1) i2 = text.length();

      String fieldName = text.substring(i, i2);

      PsiMethod getter = GroovyPropertyUtils.findPropertyGetter(aClass, fieldName, null, true);

      FieldReference fieldRef = new FieldReference(element, TextRange.from(offset + i, i2 - i), aClass, getter);
      res.add(fieldRef);

      if (getter instanceof GrMethod) {
        type = ((GrMethod)getter).getInferredReturnType();
      }
      else if (getter != null) {
        type = getter.getReturnType();
      }
      else {
        break;
      }

      if (i2 == text.length()) break;

      i = i2 + 1;
    } while (true);

    PsiReference[] resArray = res.toArray(PsiReference.EMPTY_ARRAY);

    SafeReference.makeReferencesSafe(resArray);

    return resArray;
  }

  private static class FieldReference extends PsiReferenceBase<PsiElement> {
    private final PsiClass myBeanClass;
    private final PsiMethod myGetter;

    protected FieldReference(PsiElement element, TextRange range, @NotNull PsiClass beanClass, @Nullable PsiMethod getter) {
      super(element, range, true);
      myBeanClass = beanClass;
      myGetter = getter;
    }

    @Override
    public PsiElement resolve() {
      return myGetter;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
      if (!(myGetter instanceof GrAccessorMethod)) {
        String s = GroovyPropertyUtils.getPropertyNameByGetterName(newElementName, PsiTypes.booleanType().equals(myGetter.getReturnType()));
        if (s == null) return getElement();
        newElementName = s;
      }

      TextRange rangeBefore = getRangeInElement();
      PsiElement result = super.handleElementRename(newElementName);
      //todo move this to PsiReferenceBase
      setRangeInElement(TextRange.from(rangeBefore.getStartOffset(), newElementName.length()));
      return result;
    }

    @Override
    public Object @NotNull [] getVariants() {
      Map<String, PsiMethod> propertyGetters = PropertyUtilBase.getAllProperties(myBeanClass, false, true);

      Object[] res = new Object[propertyGetters.size()];

      int i = 0;
      for (Map.Entry<String, PsiMethod> entry : propertyGetters.entrySet()) {
        PsiMethod getter = entry.getValue();
        res[i++] = CompleteReferenceExpression.createPropertyLookupElement(getter, null, null);
      }

      return res;
    }
  }

}
