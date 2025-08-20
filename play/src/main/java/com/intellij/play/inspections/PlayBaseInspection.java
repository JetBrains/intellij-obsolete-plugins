package com.intellij.play.inspections;

import com.intellij.codeInspection.*;
import com.intellij.play.utils.PlayBundle;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;


public abstract class PlayBaseInspection extends  LocalInspectionTool {
  @Override
  @NotNull
     public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
         return new PsiElementVisitor() {
             @Override
             public void visitElement(@NotNull final PsiElement element) {
                 registerProblems(element, holder);
             }
         };
     }

  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    return super.checkFile(file, manager, isOnTheFly);
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                        boolean isOnTheFly,
                                        @NotNull LocalInspectionToolSession session) {
    return super.buildVisitor(holder, isOnTheFly, session);
  }


  protected abstract void registerProblems(final PsiElement element, final ProblemsHolder holder);

    @Override
    @Nls
    @NotNull
    public String getGroupDisplayName() {
      return PlayBundle.message("play.inspections.group.name");
    }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }
}
