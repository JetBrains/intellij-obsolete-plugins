package com.intellij.gwt.uiBinder.mapping;

import com.intellij.gwt.refactorings.rename.GwtAssociatedElementRenameHandler;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UiBinderClassRenameHandler extends GwtAssociatedElementRenameHandler<PsiClass> {
  public UiBinderClassRenameHandler() {
    super(PsiClass.class);
  }

  @Override
  protected @NotNull Collection<? extends PsiElement> findAssociatedElements(@NotNull PsiClass baseElement) {
    final List<XmlFile> xmlFiles = UiBinderMappingService.getUiXmlFilesForClass(baseElement);
    if (xmlFiles.size() == 1) {
      final XmlFile xmlFile = xmlFiles.get(0);
      if (UiXmlFileRenameHandler.isBoundWithoutAnnotation(baseElement, xmlFile)) {
        return Collections.singletonList(xmlFile);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public String getNewAssociatedElementName(String newBaseElementName) {
    return newBaseElementName + UiBinderUtil.UI_XML_SUFFIX;
  }
}
