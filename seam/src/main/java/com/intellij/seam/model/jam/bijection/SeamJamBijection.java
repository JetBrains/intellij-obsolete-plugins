package com.intellij.seam.model.jam.bijection;

import com.intellij.jam.JamCommonModelElement;
import com.intellij.jam.JamElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.seam.model.SeamComponentScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Serega.Vasiliev
 */
public abstract class SeamJamBijection<T extends PsiMember & PsiNamedElement> extends JamCommonModelElement<T>
  implements JamElement, CommonModelElement {

  public SeamJamBijection(T member) {
    super(PsiElementRef.real(member));
  }

  public static final JamStringAttributeMeta.Single<String> NAME_META = JamAttributeMeta.singleString("value");

  @Nullable
  public String getDefaultName() {
    return PropertyUtilBase.getPropertyName(getPsiElement());
  }

  @Nullable
  public abstract PsiType getType();

  @NotNull
  protected abstract JamAnnotationMeta getJamAnnotationMeta();

  @Nullable
  public String getName() {
    String nameValue = getNamedStringAttributeElement().getStringValue();

    return StringUtil.isEmptyOrSpaces(nameValue) ? getDefaultName() : nameValue;
  }

  @NotNull
  private JamStringAttributeElement<String> getNamedStringAttributeElement() {
    return getJamAnnotationMeta().getAttribute(getPsiElement(), NAME_META);
  }

  @Nullable
  public SeamComponentScope getScope() {
    return AnnotationModelUtil.getEnumValue(getIdentifyingAnnotation(), "scope", SeamComponentScope.class).getValue();
  }

  @Nullable
  public PsiAnnotation getIdentifyingAnnotation() {
    return getJamAnnotationMeta().getAnnotation(getPsiElement());
  }
}
