package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.junit.GwtJUnitConstants;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class ClientClassFromNonInheritedModuleUsageInspection extends AbstractClientCodeReferencesInspection {
  @Override
  protected void checkClassReference(@NotNull PsiJavaCodeReferenceElement reference, @NotNull PsiClass referencedClass,
                                     @NotNull String className, @NotNull VirtualFile virtualFile, @NotNull GwtFacet gwtFacet,
                                     List<GwtModule> gwtModules,
                                     @NotNull GwtModulesManager gwtModulesManager,
                                     @NotNull InspectionManager manager,
                                     boolean isOnTheFly,
                                     List<ProblemDescriptor> problems) {
    List<GwtModule> referencedModules = gwtModulesManager.findGwtModulesByClientSourceFile(virtualFile);
    if (referencedModules.isEmpty()) {
      referencedModules = gwtModulesManager.findModulesByClass(reference, referencedClass.getQualifiedName());
    }
    if (referencedModules.isEmpty()) {
      //todo perhaps we should report error here but currently it'll lead to many false reports because we doesn't support super-source elements
      return;
    }

    for (GwtModule gwtModule : gwtModules) {
      if (!gwtModulesManager.isLibraryModule(gwtModule) && !gwtModulesManager.isInheritedOrSelf(gwtModule, referencedModules)
          && !isImplicitlyInheritedInTest(reference, referencedModules, gwtModulesManager)) {
        GwtModule referencedModule = referencedModules.get(0);
        final String message = GwtBundle.message("problem.description.class.0.is.defined.in.module.1.which.is.not.inherited.in.module.2",
                                                 className, referencedModule.getQualifiedName(), gwtModule.getQualifiedName());
        problems.add(manager.createProblemDescriptor(reference, message, new InheritModuleQuickFix(gwtModule, referencedModule),
                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
        return;
      }
    }
  }

  private static boolean isImplicitlyInheritedInTest(PsiJavaCodeReferenceElement reference,
                                                     List<GwtModule> referencesModules,
                                                     GwtModulesManager gwtModulesManager) {
    if (!isInGwtTestCase(reference)) return false;
    final GwtModule junitModule = gwtModulesManager.findGwtModuleByQualifiedName(GwtJUnitConstants.JUNIT_MODULE_NAME, reference.getResolveScope());
    return junitModule != null && gwtModulesManager.isInheritedOrSelf(junitModule, referencesModules);
  }

  private static boolean isInGwtTestCase(PsiJavaCodeReferenceElement reference) {
    final PsiFile containingFile = reference.getContainingFile();
    final VirtualFile file = containingFile.getVirtualFile();
    final Project project = reference.getProject();
    if (file == null || !ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(file)) return false;

    if (!(containingFile instanceof PsiJavaFile)) return false;
    final PsiClass[] classes = ((PsiJavaFile)containingFile).getClasses();
    if (classes.length != 1) return false;

    PsiClass psiClass = classes[0];
    return InheritanceUtil.isInheritor(psiClass, true, GwtJUnitConstants.GWT_TEST_CASE_CLASS);
  }

  private static class InheritModuleQuickFix extends BaseGwtLocalQuickFix {
    private final GwtModule myGwtModule;
    private final GwtModule myReferencedModule;

    InheritModuleQuickFix(final GwtModule gwtModule, final GwtModule referencedModule) {
      super(GwtBundle.message("quickfix.name.inherit.module.0.from.1", gwtModule.getQualifiedName(), referencedModule.getQualifiedName()));
      myGwtModule = gwtModule;
      myReferencedModule = referencedModule;
    }

    @Override
    public @NotNull String getFamilyName() {
      return ClientClassFromNonInheritedModuleUsageInspection.getFamilyName();
    }

    @Override
    public void applyFix(final @NotNull Project project, final @NotNull ProblemDescriptor problemDescriptor) {
      if (!ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Collections.singletonList(myGwtModule.getModuleFile())).hasReadonlyFiles()) {
        myGwtModule.addInherits().getName().setValue(myReferencedModule.getQualifiedName());
      }
    }
  }

  private static @IntentionFamilyName String getFamilyName() {
    return GwtBundle.message("quickfix.family.name.inherit.module");
  }
}
