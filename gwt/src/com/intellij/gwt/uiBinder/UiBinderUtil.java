package com.intellij.gwt.uiBinder;

import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class UiBinderUtil {
  public static final @NonNls String UI_XML_SUFFIX = ".ui.xml";
  public static final @NonNls String ELEMENT_BASE_CLASS = "com.google.gwt.dom.client.Element";
  public static final @NonNls String WIDGET_BASE_CLASS = "com.google.gwt.user.client.ui.IsWidget";
  public static final @NonNls String WIDGET_CLASS = "com.google.gwt.user.client.ui.Widget";
  public static final @NonNls String URN_IMPORT_PREFIX = "urn:import:";
  public static final @NonNls String UI_BINDER_NAMESPACE = "urn:ui:com.google.gwt.uibinder";
  public static final @NonNls String UI_FIELD_ATTRIBUTE = "field";
  public static final @NonNls String UI_WITH_TAG = "with";
  public static final @NonNls String UI_TYPE_ATTRIBUTE = "type";
  public static final @NonNls String UI_CONSTRUCTOR_ANNOTATION = "com.google.gwt.uibinder.client.UiConstructor";
  public static final @NonNls String UI_FIELD_ANNOTATION = "com.google.gwt.uibinder.client.UiField";
  public static final @NonNls String UI_HANDLER_ANNOTATION = "com.google.gwt.uibinder.client.UiHandler";
  public static final @NonNls String UI_FACTORY_ANNOTATION = "com.google.gwt.uibinder.client.UiFactory";
  public static final @NonNls String UI_CHILD_ANNOTATION = "com.google.gwt.uibinder.client.UiChild";
  public static final @NonNls String RESOURCE_PROTOTYPE_CLASS = "com.google.gwt.resources.client.ResourcePrototype";
  public static final @NonNls String GWT_EVENT_CLASS = "com.google.gwt.event.shared.GwtEvent";
  public static final @NonNls String HANDLER_REGISTRATION_INTERFACE = "com.google.gwt.event.shared.HandlerRegistration";
  public static final @NonNls String UI_STYLE_TAG = "style";
  public static final @NonNls String PROVIDED_ANNOTATION_ATTRIBUTE = "provided";
  public static final @NonNls String LAZY_DOM_ELEMENT_CLASS_NAME = "com.google.gwt.uibinder.client.LazyDomElement";
  public static final @NonNls String UI_BINDER_INTERFACE = "com.google.gwt.uibinder.client.UiBinder";
  public static final @NonNls String UI_RENDERER_INTERFACE = "com.google.gwt.uibinder.client.UiRenderer";
  public static final @NonNls String UI_TEMPLATE_ANNOTATION = "com.google.gwt.uibinder.client.UiTemplate";
  public static final @NonNls String HAS_HANDLERS_CLASS = "com.google.gwt.event.shared.HasHandlers";

  public static final GlobalSearchScope UI_XML_FILES_SCOPE = new UiXmlFilesScope();

  private UiBinderUtil() {
  }

  public static boolean isUiXmlFile(XmlFile file) {
    return file.getName().endsWith(UI_XML_SUFFIX);
  }

  public static boolean isUiHandlerMethod(PsiElement element) {
    if (!(element instanceof PsiMethod)) {
      return false;
    }
    return ((PsiMethod)element).getModifierList().hasAnnotation(UI_HANDLER_ANNOTATION);
  }

  public static boolean isUiFactoryMethod(PsiElement element) {
    if (!(element instanceof PsiMethod)) {
      return false;
    }
    return ((PsiMethod)element).getModifierList().hasAnnotation(UI_FACTORY_ANNOTATION);
  }

  public static boolean isUiField(PsiField field) {
    final PsiModifierList modifierList = field.getModifierList();
    return modifierList != null && modifierList.hasAnnotation(UI_FIELD_ANNOTATION);
  }

  public static @NotNull String getComponentClassName(XmlTag tag) {
    XmlAttribute typeAttribute = tag.getAttribute(UI_TYPE_ATTRIBUTE, UI_BINDER_NAMESPACE);
    if (typeAttribute == null && tag.getNamespace().equals(UI_BINDER_NAMESPACE)) {
      typeAttribute = tag.getAttribute(UI_TYPE_ATTRIBUTE);
    }
    if (typeAttribute != null) {
      String value = typeAttribute.getValue();
      if (value != null) {
        return value;
      }
    }

    String className = null;
    final String namespace = tag.getNamespace();
    final String tagName = tag.getLocalName();
    if (namespace.startsWith(URN_IMPORT_PREFIX)) {
      String packageName = namespace.substring(URN_IMPORT_PREFIX.length());
      className = StringUtil.getQualifiedName(packageName, tagName);
    }
    else {
      final Module module = ModuleUtilCore.findModuleForPsiElement(tag);
      if (module != null) {
        className = GwtHtmlElementClassesFinder.findElementClass(tagName, module);
      }
    }

    return className != null ? className : ELEMENT_BASE_CLASS;
  }

  static boolean hasUiBinderNamespace(@NotNull XmlAttribute attribute) {
    final String namespace = attribute.getNamespace();
    final XmlTag parent = attribute.getParent();
    return namespace.equals(UI_BINDER_NAMESPACE)
           || namespace.isEmpty() && parent != null && parent.getNamespace().equals(UI_BINDER_NAMESPACE);
  }

  public static boolean isProvidedUiField(@NotNull PsiField field) {
    final PsiModifierList modifierList = field.getModifierList();
    if (modifierList == null) return false;
    final PsiAnnotation annotation = modifierList.findAnnotation(UI_FIELD_ANNOTATION);
    if (annotation == null) return false;
    final Boolean value = AnnotationModelUtil.getBooleanValue(annotation, PROVIDED_ANNOTATION_ATTRIBUTE, false).getValue();
    return Boolean.TRUE.equals(value);
  }

  public static boolean isImplementationProvidedByGwt(@NotNull PsiClass psiClass) {
    if (!psiClass.isInterface()) {
      return false;
    }
    return InheritanceUtil.isInheritor(psiClass, "com.google.gwt.uibinder.client.UiBinder")
           || InheritanceUtil.isInheritor(psiClass, "com.google.gwt.uibinder.client.UiRenderer");
  }

  public static @NotNull PsiType getUnwrappedUiFieldType(@NotNull PsiField field) {
    final PsiType fieldType = field.getType();
    if (fieldType instanceof PsiClassType classType) {
      PsiClassType.ClassResolveResult resolved = classType.resolveGenerics();
      PsiClass psiClass = resolved.getElement();
      if (psiClass != null && LAZY_DOM_ELEMENT_CLASS_NAME.equals(psiClass.getQualifiedName())) {
        PsiType[] typeParameters = classType.getParameters();
        if (typeParameters.length == 1) {
          return typeParameters[0];
        }
      }
    }
    return fieldType;
  }

  private static class UiXmlFilesScope extends GlobalSearchScope {
    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return file.getName().endsWith(UI_XML_SUFFIX);
    }

    @Override
    public boolean isSearchInModuleContent(@NotNull Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInLibraries() {
      return true;
    }
  }
}
