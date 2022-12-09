package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.DMFacetBase;
import com.intellij.dmserver.facet.DMFacetConfigurationBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeChangeEvent;

public abstract class PsiConfigManagerBase<P extends PsiFile, C extends DMFacetConfigurationBase<C>, F extends DMFacetBase<C>> {

  public void onPsiEvent(PsiTreeChangeEvent event, F facet2update) {
    P configFile = findConfigFile(facet2update.getModule());
    if (configFile == null) {
      return;
    }

    PsiFile changedFile = event.getFile();
    if (changedFile == null) {
      return;
    }

    if (!Comparing.equal(configFile.getVirtualFile(), changedFile.getVirtualFile())) {
      return;
    }

    if (onConfigFileChanged(facet2update.getConfigurationImpl(), configFile)) {
      facet2update.getCommonPart().fireFacetChanged();
    }
  }

  protected abstract P findConfigFile(Module module);

  protected abstract boolean onConfigFileChanged(C configuration2update, P configFile);

  protected static <T> T safeValue(T value, T ifNull) {
    return value == null ? ifNull : value;
  }
}
