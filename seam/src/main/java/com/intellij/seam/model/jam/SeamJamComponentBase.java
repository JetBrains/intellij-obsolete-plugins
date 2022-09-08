package com.intellij.seam.model.jam;

import com.intellij.jam.JamCommonModelElement;
import com.intellij.jam.JamElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.seam.model.CommonSeamComponent;

/**
 * @author Serega.Vasiliev
 */
public abstract class SeamJamComponentBase<T extends PsiElement> extends JamCommonModelElement<T> implements JamElement, CommonSeamComponent {
  protected SeamJamComponentBase(PsiElementRef<?> ref) {
    super(ref);
  }
}
