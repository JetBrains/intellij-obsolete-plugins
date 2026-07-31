package com.intellij.gwt.i18n;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.references.PropertyReferenceBase;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class GwtPropertyReference extends PropertyReferenceBase {
  private final GwtI18nManager myGwtI18nManager;
  private final PsiClass myPropertiesInterface;

  public GwtPropertyReference(final @NotNull String key, final @NotNull PsiElement element, @NotNull PsiClass propertiesInterface) {
    super(key, false, element);
    myPropertiesInterface = propertiesInterface;
    myGwtI18nManager = GwtI18nManager.getInstance(propertiesInterface.getProject());
  }

  @Override
  protected List<PropertiesFile> getPropertiesFiles() {
    return Arrays.asList(myGwtI18nManager.getPropertiesFiles(myPropertiesInterface));
  }
}
