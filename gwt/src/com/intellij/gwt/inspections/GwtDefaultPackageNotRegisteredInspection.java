package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtRelativePath;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

import java.util.Collections;
import java.util.List;

public final class GwtDefaultPackageNotRegisteredInspection extends BaseGwtInspection {
  private static final @NonNls String SOURCE_TAG = "source";

  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!shouldCheck(file)) return null;

    if (!file.getName().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX)) return null;

    final GwtModule gwtModule = GwtModulesManager.getInstance(file.getProject()).getGwtModule(file);
    if (gwtModule == null) return null;

    List<ProblemDescriptor> problems = new SmartList<>();

    final List<GwtRelativePath> sources = gwtModule.getSources();
    final List<GwtRelativePath> superSources = gwtModule.getSuperSources();
    checkSubPackageWithDefaultNameRegistered(SOURCE_TAG, GwtModuleXmlConstants.DEFAULT_SOURCE_PATH, sources, superSources, gwtModule, file,
                                             manager, isOnTheFly, problems);

    final List<GwtRelativePath> publics = gwtModule.getPublics();
    checkSubPackageWithDefaultNameRegistered("public", GwtModuleXmlConstants.DEFAULT_PUBLIC_PATH, publics,
                                             Collections.emptyList(), gwtModule, file, manager, isOnTheFly, problems);

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static void checkSubPackageWithDefaultNameRegistered(final @NonNls String tagName,
                                                               final @NonNls String defaultPath,
                                                               List<GwtRelativePath> paths,
                                                               List<GwtRelativePath> additionalPaths, GwtModule gwtModule, PsiFile file,
                                                               InspectionManager manager,
                                                               boolean isOnTheFly,
                                                               List<ProblemDescriptor> problems) {
    if ((!paths.isEmpty() || !additionalPaths.isEmpty()) && !containsPath(paths, defaultPath) && !containsPath(additionalPaths, defaultPath)
        && hasSubPackage(file, defaultPath)) {
      final AddPathTagQuickFix fix = new AddPathTagQuickFix(gwtModule, tagName, defaultPath);
      final String message = GwtBundle.message("inspection.message.0.subpackage.is.not.registered.as.1.path.in.2", defaultPath, tagName, file.getName());
      GwtRelativePath item = ContainerUtil.getFirstItem(paths, null);
      if (item == null) {
        item = ContainerUtil.getFirstItem(additionalPaths, null);
      }
      if (item == null) return;

      problems.add(manager.createProblemDescriptor(item.getXmlTag(), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
    }
  }

  private static boolean hasSubPackage(@NotNull PsiFile file, @NotNull String name) {
    final PsiDirectory directory = file.getContainingDirectory();
    if (directory == null) return false;
    final PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
    if (aPackage == null) return false;
    final PsiPackage[] subPackages = aPackage.getSubPackages(file.getResolveScope());
    for (PsiPackage subPackage : subPackages) {
      if (name.equals(subPackage.getName())) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsPath(List<GwtRelativePath> paths, String path) {
    for (GwtRelativePath relativePath : paths) {
      if (path.equals(relativePath.getPath().getValue())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  private static final class AddPathTagQuickFix extends BaseGwtLocalQuickFix {
    private final GwtModule myGwtModule;
    private final String myTagName;
    private final String myPackageName;

    private AddPathTagQuickFix(GwtModule gwtModule, final String tagName, final String packageName) {
      super(GwtBundle.message("quickfix.name.register.0.subpackage.as.1.path", packageName, tagName));
      myGwtModule = gwtModule;
      myTagName = tagName;
      myPackageName = packageName;
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return GwtBundle.message("quickfix.family.name.register.subpackage");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      if (!CommonRefactoringUtil.checkReadOnlyStatus(myGwtModule.getModuleXmlFile())) {
        return;
      }

      final GwtRelativePath path = myTagName.equals(SOURCE_TAG) ? myGwtModule.addSource() : myGwtModule.addPublic();
      path.getPath().setStringValue(myPackageName);
    }
  }
}
