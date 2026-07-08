package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetDefaultWrapper;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.PuppetPsiUtil;
import com.intellij.lang.puppet.psi.PuppetStubBasedPolyNamedPsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetPolyNamedStubElement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedPsiElementBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PuppetStubBasedPolyNamedPsiElementBase<S extends PuppetPolyNamedStubElement> extends PuppetStubBasedPsiElementBase<S>
  implements
  PuppetStubBasedPolyNamedPsiElement<S> {

  public PuppetStubBasedPolyNamedPsiElementBase(@NotNull S stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetStubBasedPolyNamedPsiElementBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiElement getNameIdentifierByName(@NotNull String name) {
    List<PsiElement> identifiersList = getNameIdentifiersList();
    if (identifiersList.isEmpty()) {
      return null;
    }

    for (PsiElement identifier : identifiersList) {
      String identifierName = getNameFromIdentifier(identifier);
      if (StringUtil.equals(identifierName, name)) {
        return identifier;
      }
    }

    return null;
  }

  @Override
  public @NotNull List<String> getNamesList() {
    S stub = getGreenStub();
    if (stub != null) {
      //noinspection unchecked
      return stub.getNamesList();
    }

    return PuppetPsiUtil.computeResourceLikeNamesList(this);
  }

  // we need a list here to be able to check similar names in inspections
  @Override
  public @NotNull List<PuppetDelegatingLightNamedElement> getLightElementsList() {
    return CachedValuesManager.getCachedValue(this, () -> {
      List<PuppetDelegatingLightNamedElement> newDelegates = new ArrayList<>();
      for (String name : getNamesList()) {
        if (!StringUtil.equals(name, PuppetDefaultWrapper.DEFAULT_NAME)) {
          newDelegates.add(createDelegatingElement(name));
        }
      }
      return CachedValueProvider.Result.create(newDelegates, this);
    });
  }

  protected @NotNull PuppetDelegatingLightNamedElement createDelegatingElement(@NotNull String name) {
    return new PuppetDelegatingLightNamedElement(this, name);
  }

  @Override
  public @Nullable PuppetDelegatingLightNamedElement getLightElementByName(@NotNull String name) {
    for (PuppetDelegatingLightNamedElement element : getLightElementsList()) {
      if (StringUtil.equals(element.getName(), name)) {
        return element;
      }
    }
    return null;
  }
}
