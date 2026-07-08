package com.intellij.lang.puppet.ide.structure;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.PuppetTokenTypes.CLASS_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.FUNCTION_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.NODE_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.RESOURCE_INSTANCE_DECLARATION;
import static com.intellij.lang.puppet.PuppetTokenTypes.TYPE_DEFINITION;
import static com.intellij.lang.puppet.PuppetTokenTypes.VAR_WRAPPER;

public class PuppetStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {

  public static final TokenSet COMPOSITE_ELEMENTS = TokenSet.create(
    TYPE_DEFINITION,
    FUNCTION_DEFINITION,
    NODE_DEFINITION,
    CLASS_DEFINITION
  );

  public static final TokenSet LEAF_ELEMENTS = TokenSet.create(
    RESOURCE_INSTANCE_DECLARATION,
    VAR_WRAPPER
  );

  public static final TokenSet NODE_ELEMENTS = TokenSet.orSet(
    COMPOSITE_ELEMENTS, LEAF_ELEMENTS
  );


  public PuppetStructureViewModel(@NotNull PsiFile psiFile,
                                  @Nullable Editor editor) {
    super(psiFile, editor, new PuppetStructureViewElement(psiFile, psiFile.getName()));
  }


  @Override
  public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
    return false;
  }

  @Override
  public boolean isAlwaysLeaf(StructureViewTreeElement element) {
    return LEAF_ELEMENTS.contains(PsiUtilCore.getElementType((PsiElement)element.getValue()));
  }
}
