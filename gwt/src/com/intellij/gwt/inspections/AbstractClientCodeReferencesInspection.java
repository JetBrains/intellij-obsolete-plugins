package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractClientCodeReferencesInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!shouldCheck(file)) return null;

    final GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(file.getProject());
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;

    final List<GwtModule> gwtModules = gwtModulesManager.findGwtModulesByClientSourceFile(virtualFile);
    if (gwtModules.isEmpty()) return null;

    final GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(file.getProject(), virtualFile);
    if (gwtFacet == null || !gwtFacet.getConfiguration().getSdk().isValid()) return null;

    final List<ProblemDescriptor> problems = new ArrayList<>();

    file.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitDocComment(final @NotNull PsiDocComment comment) {
      }

      @Override
      public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
        final PsiElement resolved = reference.resolve();
        if (resolved instanceof PsiClass referencedClass) {
          String className = referencedClass.getQualifiedName();
          if (referencedClass.isAnnotationType() || className == null) return;

          final PsiFile psiFile = referencedClass.getContainingFile();
          if (psiFile == null) return;

          final VirtualFile vFile = psiFile.getVirtualFile();
          if (vFile == null) return;

          checkClassReference(reference, referencedClass, className, vFile, gwtFacet,
                              gwtModules, gwtModulesManager, manager, isOnTheFly, problems);
        }
        else if (resolved instanceof PsiMethod method) {
          checkMethodReference(reference, method, gwtFacet, gwtModules, gwtModulesManager, manager, isOnTheFly, problems);
        }
        super.visitReferenceElement(reference);
      }
    });

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  protected void checkMethodReference(PsiJavaCodeReferenceElement reference,
                                      PsiMethod method,
                                      GwtFacet facet,
                                      List<GwtModule> gwtModules, GwtModulesManager gwtModulesManager,
                                      InspectionManager manager,
                                      boolean isOnTheFly,
                                      List<ProblemDescriptor> problems) {
  }

  protected abstract void checkClassReference(@NotNull PsiJavaCodeReferenceElement reference, @NotNull PsiClass referencedClass,
                                              @NotNull String className, @NotNull VirtualFile virtualFile, @NotNull GwtFacet gwtFacet,
                                              List<GwtModule> gwtModules,
                                              @NotNull GwtModulesManager gwtModulesManager, @NotNull InspectionManager manager,
                                              boolean isOnTheFly,
                                              List<ProblemDescriptor> problems);
}
