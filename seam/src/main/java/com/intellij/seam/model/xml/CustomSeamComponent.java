package com.intellij.seam.model.xml;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.seam.impl.model.xml.components.SeamDomComponentImpl;
import com.intellij.seam.model.CommonSeamComponent;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.xml.NameValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CustomSeamComponent extends SeamDomComponentImpl implements SeamDomComponent, CommonSeamComponent {

  public static Key<PsiClass> COMPONENT_TYPE = new Key<>("COMPONENT_TYPE");

  @Override
  @NotNull
  @NameValue
  public String getComponentName() {
      return isValid() ? getComponentName(true) : "";
  }

  private static PsiType getComponentType(final PsiClass psiClass) {
    final PsiType unwrapType = SeamCommonUtils.getUnwrapType(psiClass);

    return unwrapType == null ? JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory().createType(psiClass) : unwrapType;
  }

  @NotNull
  public String getComponentName(boolean checkJamComponent) {
    String name = super.getComponentName();

    return StringUtil.isEmptyOrSpaces(name) ? getComponentName(getPsiClass(), checkJamComponent) : name;
  }

  @NotNull
  private static String getComponentName(@Nullable final PsiClass psiClass, boolean checkJamComponent) {
    if (psiClass == null) return "";

    if (checkJamComponent) {
      SeamJamComponent jamComponent = SeamCommonUtils.getSeamJamComponent(psiClass);
      if (jamComponent != null) {
        return jamComponent.getComponentName();
      }
    }
    return StringUtil.decapitalize(StringUtil.notNullize(psiClass.getName()));
  }

  @Override
  @Nullable
  public SeamComponentScope getComponentScope() {
    return null;
  }

  @Override
  @Nullable
  public PsiType getComponentType() {
    final PsiClass psiClass = getPsiClass();
    return psiClass == null ? null : getComponentType(psiClass);
  }

  @Nullable
  private PsiClass getPsiClass() {
    return getChildDescription().getUserData(COMPONENT_TYPE);
  }
}
