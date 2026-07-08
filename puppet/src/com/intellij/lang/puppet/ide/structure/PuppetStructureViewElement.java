package com.intellij.lang.puppet.ide.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.puppet.ide.navigation.PuppetItemPresentation;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.PuppetPolyNamedPsiElement;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.lang.puppet.ide.structure.PuppetStructureViewModel.NODE_ELEMENTS;

public class PuppetStructureViewElement implements StructureViewTreeElement, SortableTreeElement {

  private final @NotNull NavigatablePsiElement myElement;
  private final @NotNull String myName;

  public PuppetStructureViewElement(@NotNull NavigatablePsiElement element, @NotNull String name) {
    myElement = element;
    myName = name;
  }

  @Override
  public void navigate(boolean requestFocus) {
    myElement.navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return myElement.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myElement.canNavigateToSource();
  }

  @Override
  public @NotNull ItemPresentation getPresentation() {
    String location = "";
    if (myElement instanceof PuppetResourceInstanceDeclaration) {
      location =
        "[" + PuppetQualifiedNamesUtil.capitalizePuppetName(((PuppetResourceInstanceDeclaration)myElement).getEffectiveTypeName()) + "]";
    }
    return PuppetItemPresentation.create(myName, location, myElement);
  }

  @Override
  public TreeElement @NotNull [] getChildren() {
    if (!myElement.isValid()) {
      return TreeElement.EMPTY_ARRAY;
    }
    final List<TreeElement> result = new ArrayList<>();

    PuppetResolveUtil
      .processStubBasedChildrenWithSmartStop(
        myElement,
        NODE_ELEMENTS,
        element -> {
          if (element instanceof PuppetPolyNamedPsiElement) {
            for (PuppetDelegatingLightNamedElement lightNamedElement : ((PuppetPolyNamedPsiElement)element).getLightElementsList()) {
              String name = lightNamedElement.getName();
              if (StringUtil.isNotEmpty(name)) {
                result.add(new PuppetStructureViewElement(lightNamedElement, name));
              }
            }
          }
          else {
            String elementName = element.getName();
            if (StringUtil.isNotEmpty(elementName)) {
              result.add(new PuppetStructureViewElement(element, elementName));
            }
          }
          return true;
        });

    return result.toArray(TreeElement.EMPTY_ARRAY);
  }

  @Override
  public @NotNull String getAlphaSortKey() {
    return myName;
  }

  @Override
  public Object getValue() {
    return myElement.isValid() ? myElement : null;
  }
}
