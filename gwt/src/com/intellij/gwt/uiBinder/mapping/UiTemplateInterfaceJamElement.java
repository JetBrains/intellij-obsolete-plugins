package com.intellij.gwt.uiBinder.mapping;

import com.intellij.gwt.clientBundle.jam.SourceFileJamConverter;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.Nullable;

public final class UiTemplateInterfaceJamElement extends JamBaseElement<PsiClass> {
  private static final Condition<PsiFileSystemItem> COMPLETION_FILTER =
    item -> item instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)item);

  private static final JamStringAttributeMeta.Single<PsiFile> TEMPLATE_PATH_META =
    JamAttributeMeta.singleString("value", new SourceFileJamConverter(COMPLETION_FILTER));

  private static final JamAnnotationMeta UI_TEMPLATE_META = new JamAnnotationMeta(UiBinderUtil.UI_TEMPLATE_ANNOTATION)
    .addAttribute(TEMPLATE_PATH_META);

  public static final JamClassMeta<UiTemplateInterfaceJamElement> META =
    new JamClassMeta<>(UiTemplateInterfaceJamElement.class, UiTemplateInterfaceJamElement::new)
      .addAnnotation(UI_TEMPLATE_META);

  private UiTemplateInterfaceJamElement(PsiElementRef<?> ref) {
    super(ref);
  }

  public @Nullable String getUiTemplateValue() {
    return UI_TEMPLATE_META.getAttribute(getPsiElement(), TEMPLATE_PATH_META).getStringValue();
  }

  public static @Nullable UiTemplateInterfaceJamElement getElement(PsiClass aClass) {
    return JamService.getJamService(aClass.getProject()).getJamElement(META.getJamKey(), aClass);
  }
}
