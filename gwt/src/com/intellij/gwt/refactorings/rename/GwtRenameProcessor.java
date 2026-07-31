package com.intellij.gwt.refactorings.rename;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.uiBinder.mapping.UiBinderClassRenameHandler;
import com.intellij.gwt.uiBinder.mapping.UiXmlFileRenameHandler;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class GwtRenameProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    if (GwtFacet.findFacetByPsiElement(element) == null) return false;

    for (GwtAssociatedElementRenameHandler handler : GwtRenameHandlersHolder.HANDLERS) {
      if (!handler.getAssociatedElements(element).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    for (GwtAssociatedElementRenameHandler<?> handler : GwtRenameHandlersHolder.HANDLERS) {
      for (PsiElement associatedElement : handler.getAssociatedElements(element)) {
        allRenames.put(associatedElement, handler.getNewAssociatedElementName(newName));
      }
    }
  }

  private static final class GwtRenameHandlersHolder {
    public static final GwtAssociatedElementRenameHandler[] HANDLERS = {
      new CssClassRenameHandler(),
      new MethodForCssClassRenameHandler(),
      new UiXmlFileRenameHandler(),
      new UiBinderClassRenameHandler()
    };

    private GwtRenameHandlersHolder() {
    }
  }

}
