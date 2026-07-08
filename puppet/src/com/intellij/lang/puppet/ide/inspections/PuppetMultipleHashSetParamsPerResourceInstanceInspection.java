package com.intellij.lang.puppet.ide.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PsiPuppetHashArgument;
import com.intellij.lang.puppet.psi.PsiPuppetResourceDeclaration;
import com.intellij.lang.puppet.psi.PsiPuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.PsiPuppetVisitor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PuppetMultipleHashSetParamsPerResourceInstanceInspection extends LocalInspectionTool {
  private static final Logger LOG = Logger.getInstance(PuppetMultipleHashSetParamsPerResourceInstanceInspection.class);

  private static final Key<Map<PsiElement, PsiPuppetHashArgument>> RESOURCE_TO_PARAM_KEY = Key.create("NUM_OF_PARAMS");

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder,
                                                 boolean isOnTheFly,
                                                 final @NotNull LocalInspectionToolSession session) {

    session.putUserDataIfAbsent(RESOURCE_TO_PARAM_KEY, new ConcurrentHashMap<>(1, 0.75f, 1));

    return new PsiPuppetVisitor() {
      @Override
      public void visitHashArgument(@NotNull PsiPuppetHashArgument hashParam) {
        final PsiElement enclosingResource
          = PsiTreeUtil.getParentOfType(hashParam, PsiPuppetResourceDeclaration.class, PsiPuppetResourceInstanceDeclaration.class);
        if (enclosingResource == null) {
          LOG.error("Enclosing resource expected: " + dumpParents(hashParam));
          return;
        }

        final Map<PsiElement, PsiPuppetHashArgument> data = session.getUserData(RESOURCE_TO_PARAM_KEY);
        LOG.assertTrue(data != null);

        final PsiPuppetHashArgument oldValue = data.get(enclosingResource);
        if (oldValue != null && oldValue != hashParam) {
          register(holder, hashParam);
          register(holder, oldValue);
        }
        else {
          data.put(enclosingResource, hashParam);
        }
      }
    };
  }

  private static @NotNull String dumpParents(@NotNull PsiElement run) {
    StringBuilder sb = new StringBuilder();
    while (run != null) {
      sb.append(run.getClass().getSimpleName()).append("\n\t");
      if (run instanceof PsiFile) {
        break;
      }
      run = run.getParent();
    }
    return sb.toString();
  }

  private static void register(@NotNull ProblemsHolder holder, @NotNull PsiPuppetHashArgument hashParam) {
    holder.registerProblem(hashParam, PuppetBundle.message("inspections.multiple.hash.param.description"));
  }
}
