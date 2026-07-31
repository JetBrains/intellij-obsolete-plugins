package com.intellij.gwt.uiBinder.references;

import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class GwtUiFieldFromHandlerReference extends GwtUiFieldReferenceBase<PsiLiteralExpression> {
  public GwtUiFieldFromHandlerReference(PsiLiteralExpression element) {
    super(element, true);
  }

  @Override
  protected @NotNull List<PsiClass> findUiBinderClasses() {
    return ContainerUtil.createMaybeSingletonList(getBinderClass());
  }

  private @Nullable PsiClass getBinderClass() {
    return PsiTreeUtil.getParentOfType(myElement, PsiClass.class);
  }

  @Override
  protected @NotNull List<XmlFile> findUiXmlFiles() {
    final PsiClass binderClass = getBinderClass();
    return binderClass != null ? UiBinderMappingService.getUiXmlFilesForClass(binderClass) : Collections.emptyList();
  }

  @Override
  protected String getFieldName() {
    final Object value = myElement.getValue();
    return value instanceof String ? (String)value : "";
  }
}
