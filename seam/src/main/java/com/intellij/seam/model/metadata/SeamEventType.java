package com.intellij.seam.model.metadata;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.RenameableFakePsiElement;
import com.intellij.seam.SeamIcons;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;

public class SeamEventType extends RenameableFakePsiElement {
  private final String myEventType;

  public SeamEventType(final String eventType, final PsiFile containingFile) {
    super(containingFile);
    myEventType = eventType;
  }

  @Override
  public String getName() {
    return myEventType;
  }

  @Override
  public PsiElement getParent() {
    return getContainingFile();
  }

  @Override @NonNls
  public String getTypeName() {
    return "Event Type";
  }

  @Override
  public Icon getIcon() {
    return SeamIcons.Seam;
  }
}
