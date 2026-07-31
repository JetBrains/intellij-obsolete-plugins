package com.intellij.gwt.uiBinder;

import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.gwt.uiBinder.references.GwtTagWidgetReference;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.TagNameReference;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.DefaultXmlExtension;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.Nullable;

public final class GwtUiXmlExtension extends DefaultXmlExtension {
  @Override
  public boolean isAvailable(PsiFile file) {
    return file instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)file);
  }

  @Override
  public String[][] getNamespacesFromDocument(XmlDocument parent, boolean declarationsExist) {
    return XmlUtil.getDefaultNamespaces(parent);
  }

  @Override
  public @Nullable TagNameReference createTagNameReference(ASTNode nameElement, boolean startTagFlag) {
    return new GwtTagWidgetReference(nameElement, startTagFlag);
  }

  @Override
  public boolean isRequiredAttributeImplicitlyPresent(XmlTag tag, String attrName) {
    XmlElementDescriptor descriptor = tag.getDescriptor();
    if (!(descriptor instanceof GwtUiComponentDescriptor)) return false;

    XmlAttributeDescriptor attributeDescriptor = descriptor.getAttributeDescriptor(attrName, tag);
    if (!(attributeDescriptor instanceof GwtUiParameterAttributeDescriptor)) return false;

    XmlAttribute fieldAttribute = tag.getAttribute(UiBinderUtil.UI_FIELD_ATTRIBUTE, UiBinderUtil.UI_BINDER_NAMESPACE);
    if (fieldAttribute == null) return false;

    Module module = ModuleUtilCore.findModuleForPsiElement(tag);
    if (module == null) return false;

    for (PsiClass psiClass : UiBinderMappingService.getInstance(module).getBoundClasses(tag.getContainingFile())) {
      PsiField field = psiClass.findFieldByName(fieldAttribute.getValue(), true);
      if (field != null && UiBinderUtil.isProvidedUiField(field)) {
        return true;
      }
    }
    return false;
  }
}
