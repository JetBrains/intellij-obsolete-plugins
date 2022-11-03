package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;

public class WithModulePackagingElementPresentation extends PackagingElementPresentation {
  private final String myModuleId;

  private final String myResourceKey;
  private final Icon myIcon;

  public WithModulePackagingElementPresentation(String moduleId,
                                                @PropertyKey(resourceBundle = DmServerBundle.BUNDLE) String resourceKey,
                                                Icon icon) {
    myModuleId = moduleId;
    myResourceKey = resourceKey;
    myIcon = icon;
  }

  @Override
  public void render(@NotNull PresentationData presentationData,
                     SimpleTextAttributes mainAttributes,
                     SimpleTextAttributes commentAttributes) {
    presentationData.setIcon(myIcon);
    presentationData.addText(DmServerBundle.message(myResourceKey, getReferencedModuleName()), mainAttributes);
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.MODULE;
  }

  @Override
  public String getPresentableName() {
    return DmServerBundle.message("WithModulePackagingElementPresentation.presentable.name", getReferencedModuleName());
  }

  @NotNull
  private String getReferencedModuleName() {
    return myModuleId == null ? getUnknownModuleName() : myModuleId;
  }

  @NotNull
  private String getUnknownModuleName() {
    return DmServerBundle.message("WithModulePackagingElementPresentation.undefined.module");
  }
}
