package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.PuppetPsiUtil;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetResourceInstanceStubsIndex;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PuppetResourceInstanceReference extends PuppetPolyVariantCachingReferenceBase<PsiElement> {
  private final @NotNull String myTypeName;

  public PuppetResourceInstanceReference(PsiElement element, @NotNull String typeName) {
    super(element);
    myTypeName = typeName;
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    return PuppetPsiUtil.renameResourceInstanceIdentifier(myElement, newElementName);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {
    String resourceName = ElementManipulators.getValueText(myElement);

    List<ResolveResult> result = new ArrayList<>();

    for (PuppetResourceInstanceDeclaration declaration : PuppetResourceInstanceStubsIndex.find(resourceName, myTypeName, myElement)) {
      PuppetDelegatingLightNamedElement delegate = declaration.getLightElementByName(resourceName);
      if (delegate != null) {
        result.add(new PsiElementResolveResult(delegate));
      }
    }

    return result.toArray(ResolveResult.EMPTY_ARRAY);
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.resource_instance");
  }
}

