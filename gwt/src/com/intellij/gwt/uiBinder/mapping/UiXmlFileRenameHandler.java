package com.intellij.gwt.uiBinder.mapping;

import com.intellij.gwt.refactorings.rename.GwtAssociatedElementRenameHandler;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UiXmlFileRenameHandler extends GwtAssociatedElementRenameHandler<XmlFile> {
  public UiXmlFileRenameHandler() {
    super(XmlFile.class);
  }

  @Override
  protected @NotNull Collection<? extends PsiElement> findAssociatedElements(@NotNull XmlFile baseElement) {
    final List<PsiClass> classes = UiBinderMappingService.getBoundClassesForFile(baseElement);
    if (classes.size() == 1) {
      final PsiClass psiClass = classes.get(0);
      if (isBoundWithoutAnnotation(psiClass, baseElement)) {
        return Collections.singletonList(psiClass);
      }
    }
    return Collections.emptyList();
  }

  public static boolean isBoundWithoutAnnotation(@NotNull PsiClass psiClass, @NotNull XmlFile xmlFile) {
    final XmlFile file = UiBinderMappingService.getDefaultTemplateFile(psiClass);
    return xmlFile.getManager().areElementsEquivalent(xmlFile, file);
  }

  @Override
  public String getNewAssociatedElementName(String newBaseElementName) {
    return StringUtil.trimEnd(newBaseElementName, UiBinderUtil.UI_XML_SUFFIX);
  }
}
