package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import com.intellij.codeInspection.ExternalAnnotatorInspectionVisitor;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefBundle;

public final class FoodcriticInspection extends LocalInspectionTool {
  public static final String FOODCRITIC_INSPECTION_SHORT_NAME = "FoodcriticInspection";

  @Override
  public @NonNls @NotNull String getShortName() {
    return FOODCRITIC_INSPECTION_SHORT_NAME;
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                                 boolean isOnTheFly,
                                                 @NotNull LocalInspectionToolSession session) {
    return new ExternalAnnotatorInspectionVisitor(holder, new FoodcriticAnnotator(), isOnTheFly);
  }

  @Override
  public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    return ExternalAnnotatorInspectionVisitor.checkFileWithExternalAnnotator(file, manager, isOnTheFly, new FoodcriticAnnotator());
  }

  public static @IntentionFamilyName String getFoodcriticInspectionDisplayName() {
    return ChefBundle.message("chef.foodcritic.inspection.display.name");
  }
}
