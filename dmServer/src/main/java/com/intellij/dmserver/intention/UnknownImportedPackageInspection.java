package com.intellij.dmserver.intention;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.dmserver.manifest.HeaderValuePartDispatcher;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class UnknownImportedPackageInspection extends LocalInspectionTool {

  private static final HeaderValuePartDispatcher<ProblemsHolder, UnitResolver> ourUnitResolveDispatcher
    = new HeaderValuePartDispatcher<>(
    new PackageResolver(),
    new BundleResolver(),
    new LibraryResolver()
  );

  @Override
  @Nls
  @NotNull
  public String getGroupDisplayName() {
    return DmServerBundle.message("UnknownImportedPackageInspection.group-name.osgi");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Override
  @NotNull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Override
  @NonNls
  @NotNull
  public String getShortName() {
    return "dmserverUnknownImportedPackage";
  }

  @Override
  @NotNull
  public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {

      @Override
      public void visitElement(@NotNull PsiElement element) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null || DMBundleFacet.getInstance(module) == null) {
          return;
        }
        ourUnitResolveDispatcher.process(element, holder);
      }
    };
  }
}
