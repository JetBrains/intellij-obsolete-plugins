package com.intellij.seam.highlighting.jam;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.compiler.util.InspectionValidatorWrapper;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SeamJamModelInspectionBase extends AbstractBaseJavaLocalInspectionTool {

  @Override
  @NotNull
  public String getGroupDisplayName() {
    return SeamInspectionBundle.message("model.inspection.group.name");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Override
  public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!JamCommonUtil.isPlainJavaFile(file)) return null;
    if (!isFileAccepted(file.getContainingFile())) return null;
    if (!SeamCommonUtils.isSeamFacetDefined(ModuleUtilCore.findModuleForPsiElement(file))) return null;

    final ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);
    checkJavaFile((PsiJavaFile)file, holder, isOnTheFly);
    final List<ProblemDescriptor> problemDescriptors = holder.getResults();
    if (problemDescriptors != null) return problemDescriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);

    return null;
  }

  protected void checkJavaFile(@NotNull final PsiJavaFile javaFile, @NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
    for (PsiClass psiClass : javaFile.getClasses()) {
      checkClassInternal(psiClass, holder);
    }
  }

  private  void checkClassInternal(final PsiClass aClass, final ProblemsHolder holder) {
    checkClass(aClass, holder);
    for (PsiClass psiClass : aClass.getInnerClasses()) {
      checkClass(psiClass, holder);
    }
  }

  protected  void checkClass(final PsiClass aClass, final ProblemsHolder holder) {
    SeamJamComponent jamComponent = SeamCommonUtils.getSeamJamComponent(aClass);
    if (jamComponent != null) {
      checkSeamJamComponent(jamComponent, holder);
    }
  }

  protected void checkSeamJamComponent(final SeamJamComponent jamComponent, final ProblemsHolder holder) {
  }

  protected static boolean isFileAccepted(final PsiFile file) {
    if (!InspectionValidatorWrapper.isCompilationThread()) return true;
    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null) return false;
    return true;
  }
}

