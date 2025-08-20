package com.intellij.play.references;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.references.CreatePropertyFix;
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PlayPropertyReference extends PropertyReference {
  public PlayPropertyReference(@NotNull String value, @NotNull PsiElement element) {
    super(value, element, null, false);
  }

  @Override
  protected List<PropertiesFile> getPropertiesFiles() {
    List<PropertiesFile> propertiesFiles = new ArrayList<>();
    final Module module = ModuleUtilCore.findModuleForPsiElement(getElement());

    if (module != null) {
      final Set<PsiDirectory> directories = PlayPathUtils.getConfigDirectories(module);
      for (PsiDirectory directory : directories) {
        final PsiFile[] files = directory.getFiles();
        for (PsiFile file : files) {
          if (file instanceof  PropertiesFile && file.getName().startsWith("message") ) {
               propertiesFiles.add((PropertiesFile)file);
          }
        }
      }
    }

    return propertiesFiles;
  }

  @Override
  public @NotNull LocalQuickFix @Nullable [] getQuickFixes() {
    CreatePropertyFix fix = new CreatePropertyFix(myElement, myKey, getPropertiesFiles());
    return new LocalQuickFix[] {fix};
  }


}
