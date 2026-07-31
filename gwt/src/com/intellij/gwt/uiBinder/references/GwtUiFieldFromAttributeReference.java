package com.intellij.gwt.uiBinder.references;

import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class GwtUiFieldFromAttributeReference extends GwtUiFieldReferenceBase<XmlAttributeValue> {
  public GwtUiFieldFromAttributeReference(@NotNull XmlAttributeValue element) {
    super(element, false);
  }

  @Override
  protected String getFieldName() {
    return myElement.getValue();
  }

  @Override
  protected @NotNull List<PsiClass> findUiBinderClasses() {
    final PsiFile containingFile = myElement.getContainingFile();
    if (containingFile == null) return null;

    return UiBinderMappingService.getBoundClassesForFile(containingFile.getOriginalFile());
  }

  @Override
  protected @NotNull List<XmlFile> findUiXmlFiles() {
    final PsiFile file = myElement.getContainingFile();
    return file instanceof XmlFile ? Collections.singletonList((XmlFile)file) : Collections.emptyList();
  }
}
