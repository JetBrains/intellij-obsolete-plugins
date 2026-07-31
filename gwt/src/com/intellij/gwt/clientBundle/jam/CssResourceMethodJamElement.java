package com.intellij.gwt.clientBundle.jam;

import com.intellij.gwt.clientBundle.ClientBundleUtil;
import com.intellij.gwt.clientBundle.css.GwtCssDeclarationsManager;
import com.intellij.gwt.clientBundle.css.language.psi.GwtCssDef;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.CssElement;
import com.intellij.psi.css.StylesheetFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.intellij.jam.reflect.JamAttributeMeta.singleString;

public final class CssResourceMethodJamElement extends JamBaseElement<PsiMethod> {
  private static final JamStringAttributeMeta.Single<String> CLASS_NAME_META = singleString("value", new CssClassNameConverter());
  private static final JamAnnotationMeta CSS_CLASS_META = new JamAnnotationMeta(ClientBundleUtil.CLASS_NAME_ANNOTATION)
    .addAttribute(CLASS_NAME_META);
  public static final JamMethodMeta<CssResourceMethodJamElement> META =
    new JamMethodMeta<>(CssResourceMethodJamElement.class, CssResourceMethodJamElement::new).addAnnotation(CSS_CLASS_META);

  private CssResourceMethodJamElement(PsiElementRef<?> ref) {
    super(ref);
  }

  public @NotNull List<CssElement> findCssElements() {
    final PsiClass psiClass = getPsiElement().getContainingClass();
    if (psiClass == null) return Collections.emptyList();

    final Set<StylesheetFile> files = ClientBundleUtil.getStylesheetFiles(psiClass, true, true);
    if (files.isEmpty()) return Collections.emptyList();

    final String cssClassName = getCssClassName();
    String defName = getPsiElement().getName();
    List<CssElement> result = new ArrayList<>();
    for (StylesheetFile file : files) {
      result.addAll(GwtCssDeclarationsManager.findDeclarations(file, cssClassName, CssClass.class));
      result.addAll(GwtCssDeclarationsManager.findDeclarations(file, defName, GwtCssDef.class));
    }
    return result;
  }

  public String getCssClassName() {
    final String value = getClassNameAttributeElement().getValue();
    if (value != null) {
      return value;
    }
    return getPsiElement().getName();
  }

  public @NotNull JamStringAttributeElement<String> getClassNameAttributeElement() {
    return CSS_CLASS_META.getAttribute(getPsiElement(), CLASS_NAME_META);
  }

  public static @Nullable CssResourceMethodJamElement getJamElement(PsiMethod method) {
    return JamService.getJamService(method.getProject()).getJamElement(META.getJamKey(), method);
  }
}
