package com.intellij.seam.model.gotosymbol;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.seam.utils.beans.ContextVariable;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class GotoSeamComponentsProvider extends GoToSymbolProvider {

  @Override
  protected void addNames(@NotNull final Module module, final Set<String> result) {
    result.addAll(SeamCommonUtils.getSeamContextVariableNames(module));
  }

  @Override
  protected void addItems(@NotNull final Module module, final String name, final List<NavigationItem> result) {
    for (ContextVariable variable : SeamCommonUtils.getSeamContextVariablesWithDependencies(module, true, false)) {
      if (name.equals(variable.getName())) {
        final PsiElement psiElement = variable.getModelElement().getIdentifyingPsiElement();

        if (psiElement != null) {
          final NavigationItem navigationItem = createNavigationItem(psiElement, name, SeamIcons.Seam);
          if (!result.contains(navigationItem)) {
            result.add(navigationItem);
          }
        }
      }
    }
  }

  @Override
  protected boolean acceptModule(final Module module) {
    return SeamCommonUtils.isSeamFacetDefined(module);
  }
}
