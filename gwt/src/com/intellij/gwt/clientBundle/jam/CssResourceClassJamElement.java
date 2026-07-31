package com.intellij.gwt.clientBundle.jam;

import com.intellij.gwt.clientBundle.ClientBundleUtil;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamChildrenQuery;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CssResourceClassJamElement extends JamBaseElement<PsiClass> {
  public static final JamClassMeta<CssResourceClassJamElement> META =
    new JamClassMeta<>(CssResourceClassJamElement.class, CssResourceClassJamElement::new);

  private static final JamChildrenQuery<CssResourceMethodJamElement> CHILDREN_QUERY = new JamChildrenQuery<>() {
    @Override
    public JamMemberMeta<?, ? extends CssResourceMethodJamElement> getMeta(@NotNull PsiModifierListOwner member) {
      return CssResourceMethodJamElement.META;
    }

    @Override
    protected List<CssResourceMethodJamElement> findChildren(@NotNull PsiMember parent) {
      final List<CssResourceMethodJamElement> result = new ArrayList<>();
      if (parent instanceof PsiClass) {
        for (PsiMethod method : ((PsiClass)parent).getAllMethods()) {
          ContainerUtil.addIfNotNull(result, CssResourceMethodJamElement.getJamElement(method));
        }
      }
      return result;
    }
  };

  static {
    META.addChildrenQuery(CHILDREN_QUERY);
  }

  private CssResourceClassJamElement(PsiElementRef<?> ref) {
    super(ref);
  }

  public @NotNull Set<StylesheetFile> findStylesheetFiles(final boolean addLocalized, final boolean processSubClasses) {
    return ClientBundleUtil.getStylesheetFiles(getPsiElement(), addLocalized, processSubClasses);
  }

  public @NotNull List<CssResourceMethodJamElement> getCssMethods() {
    return CHILDREN_QUERY.findChildren(getPsiElementRef());
  }

  public static @Nullable CssResourceClassJamElement getJamElement(@NotNull PsiClass psiClass) {
    return ReadAction.computeBlocking(() -> JamService.getJamService(psiClass.getProject()).getJamElement(META.getJamKey(), psiClass));
  }
}
