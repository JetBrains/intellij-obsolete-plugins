package com.intellij.seam.model.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.seam.model.metadata.SeamEventTypeFactory;
import org.jetbrains.annotations.Nullable;

public abstract class BasicEventTypeReference <T extends PsiElement>  extends PsiReferenceBase<T> {

  public BasicEventTypeReference(final T element) {
    super(element);
  }

  @Override
  public PsiElement resolve() {
    final Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module != null) {
      final String s = getEventType(getElement());
      return StringUtil.isEmptyOrSpaces(s) ? null : SeamEventTypeFactory.getInstance(module).getOrCreateEventType(s);
    }
    return null;
  }

  @Nullable
  protected abstract String getEventType(T psiElement);
}
