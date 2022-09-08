package com.intellij.seam.model.jam;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.xml.CustomSeamComponent;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// This component is defined in component.xml (class with no @Name) but has seam annotations like @In, @Out, etc.
public class MergedSeamComponent extends SeamJamComponent {
  private final SeamDomComponent myDomComponent;

  public MergedSeamComponent(PsiClass psiClass, SeamDomComponent domComponent) {
    super(PsiElementRef.real(psiClass));
    myDomComponent = domComponent;
  }

  @Override
  @NotNull
  public String getComponentName() {
    if (myDomComponent == null || !myDomComponent.isValid()) return "";

    return myDomComponent instanceof CustomSeamComponent
           ? ((CustomSeamComponent)myDomComponent).getComponentName(false)
           : myDomComponent.getComponentName();
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    if (myDomComponent == null || !myDomComponent.isValid()) return super.getIdentifyingPsiElement();

    return myDomComponent.getXmlTag();
  }

  @Override
  public SeamComponentScope getComponentScope() {
    if (myDomComponent.isValid() ) {
      SeamComponentScope componentScope = myDomComponent.getComponentScope();
      if (componentScope != null) return componentScope;
    }
    return super.getComponentScope();
  }

  @Override
  @Nullable
  protected JamStringAttributeElement<String> getNamedStringAttributeElement() {
    return null;
  }
}
