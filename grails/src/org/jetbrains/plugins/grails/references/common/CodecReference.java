// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Couple;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.util.CodecUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodecReference extends PsiReferenceBase<PsiElement> {

  public CodecReference(PsiElement element, boolean soft) {
    super(element, soft);
  }

  @Override
  public PsiElement resolve() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return null;

    String value = getValue();

    Couple<PsiMethod> pair = CodecUtil.getCodecMap(module).get(value);
    if (pair == null || pair.first == null) return null;

    return pair.first.getNavigationElement();
  }

  @Override
  public Object @NotNull [] getVariants() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

    List<String> res = new ArrayList<>();

    for (Map.Entry<String, Couple<PsiMethod>> entry : CodecUtil.getCodecMap(module).entrySet()) {
      if (entry.getValue().first != null) {
        res.add(entry.getKey());
      }
    }

    return res.toArray();
  }
}
