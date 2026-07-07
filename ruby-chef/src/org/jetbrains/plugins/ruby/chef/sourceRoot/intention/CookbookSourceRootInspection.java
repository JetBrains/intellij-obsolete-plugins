package org.jetbrains.plugins.ruby.chef.sourceRoot.intention;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.projectView.actions.MarkRootsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.chef.ChefBundle;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.chef.sourceRoot.ChefTopics;
import org.jetbrains.plugins.ruby.chef.sourceRoot.CookbooksRootType;
import org.jetbrains.plugins.ruby.ruby.inspections.RubyInspectionVisitor;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RFile;
import org.jetbrains.plugins.ruby.testing.rspec.RSpecUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CookbookSourceRootInspection extends LocalInspectionTool {
  private static final String RECIPES_DIR = "recipes";
  private static final String TEST_DIRECTORY = "test";

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    return new CookbookVisitor(holder);
  }

  private static final class CookbookVisitor extends RubyInspectionVisitor {

    CookbookVisitor(@NotNull ProblemsHolder holder) {
      super(holder);
    }

    @Override
    public void visitRFile(final @NotNull RFile rFile) {
      super.visitRFile(rFile);

      final Project project = rFile.getProject();
      final Set<PsiDirectory> cookbooks = new HashSet<>();
      for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
        VfsUtilCore.processFilesRecursively(root, file -> {
          final PsiDirectory directory;
          final PsiDirectory cookbook;
          if ((directory = PsiManager.getInstance(project).findDirectory(file)) == null || (cookbook = directory.getParent()) == null)
            return true;

          if (RECIPES_DIR.equals(directory.getName()) &&
              PsiTreeUtil.findChildOfType(directory, RFile.class) != null &&
              !ChefUtil.isCookbook(cookbook) && !isUnderExcludedDirectory(directory)) {
            cookbooks.add(cookbook);
          }

          return true;
        });
      }

      String cookbookNames = "";
      for (PsiDirectory cookbook : cookbooks) {
        if (!cookbookNames.isEmpty()) cookbookNames += ", ";

        cookbookNames += cookbook.getName();
      }

      if (!cookbooks.isEmpty()) {
        registerProblem(rFile, ChefBundle.message("cookbooks.source.root.inspection.detection.text", cookbookNames),
                        new ConfigureCookbookSourceRootFix(new ArrayList<>(cookbooks)));
      }
    }

    private static boolean isUnderExcludedDirectory(final @NotNull PsiDirectory directory) {
      return PsiTreeUtil.findFirstParent(directory, directory1 -> {
        if (!(directory1 instanceof PsiDirectory)) return false;

        final String dirName = ((PsiDirectory)directory1).getName();
        return (RSpecUtil.SPECS_FOLDER.equals(dirName) || TEST_DIRECTORY.equals(dirName)) && directory1.getManager().isInProject(directory1);
      }) != null;
    }
  }

  private static final class ConfigureCookbookSourceRootFix implements LocalQuickFix {
    private final List<PsiDirectory> myCookbooks;

    ConfigureCookbookSourceRootFix(List<PsiDirectory> cookbooks) {
      myCookbooks = cookbooks;
    }

    @Override
    public @NotNull String getName() {
      return ChefBundle.message("cookbooks.source.root.inspection.name");
    }

    @Override
    public @NotNull String getFamilyName() {
      return ChefBundle.message("cookbooks.source.root.inspection.family.name");
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
      return new IntentionPreviewInfo.Html(ChefBundle.message("cookbooks.source.root.inspection.preview"));
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }

    @Override
    public void applyFix(final @NotNull Project project, final @NotNull ProblemDescriptor descriptor) {
      ApplicationManager.getApplication().invokeLater(() -> {
        CookbooksChooserDialog cookbooksChooserDialog = new CookbooksChooserDialog(project, myCookbooks);
        cookbooksChooserDialog.show();

        if (cookbooksChooserDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
          addCookbooks(cookbooksChooserDialog.getMarkedElements());
          project.getMessageBus().syncPublisher(ChefTopics.COOKBOOK).cookbookAdded();
        }
      });
    }

    private static void addCookbooks(List<PsiDirectory> directories) {
      for (PsiDirectory psiDirectory : directories) {
        final Module module = ModuleUtilCore.findModuleForPsiElement(psiDirectory);
        if (module == null) continue;

        final VirtualFile psiDirectoryVirtualFile = psiDirectory.getVirtualFile();
        final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
        ContentEntry entry = MarkRootsManager.findContentEntry(model, psiDirectoryVirtualFile);
        if (entry != null) {
          final SourceFolder[] sourceFolders = entry.getSourceFolders();
          for (SourceFolder sourceFolder : sourceFolders) {
            if (Comparing.equal(sourceFolder.getFile(), psiDirectoryVirtualFile)) {
              entry.removeSourceFolder(sourceFolder);
              break;
            }
          }
          entry.addSourceFolder(psiDirectoryVirtualFile, CookbooksRootType.COOKBOOKS);

          ApplicationManager.getApplication().runWriteAction(() -> model.commit());
        }
      }
    }
  }
}
