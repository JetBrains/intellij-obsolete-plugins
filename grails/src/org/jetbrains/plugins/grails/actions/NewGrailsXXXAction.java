// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.WeighingNewActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import java.util.ArrayList;
import java.util.List;

public abstract class NewGrailsXXXAction extends AnAction implements DumbAware {

  private final @NotNull String myPopupTextKey;

  protected NewGrailsXXXAction(@NotNull @PropertyKey(resourceBundle = GrailsBundle.BUNDLE) String projectViewTextKey) {
    this.myPopupTextKey = projectViewTextKey;
  }

  protected static String canonicalize(String name) {
    if (name == null || name.isEmpty()) return "";
    final int i = name.lastIndexOf(".");
    if (i > 0 && i < name.length() - 1) {
      final String tail = name.substring(i + 1);
      final String head = name.substring(0, i);
      return (head + "/" + StringUtil.capitalize(tail)).replace('.', '/');
    }
    name = name.replace('.', '/');
    return StringUtil.capitalize(name);
  }

  @Override
  public void actionPerformed(final @NotNull AnActionEvent e) {
    final GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(e.getDataContext());
    if (application == null) return;

    final Project project = application.getProject();

    final DataContext dataContext = e.getDataContext();

    final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (view == null) return;


    String name = getArtefactName(e, project);
    if (name == null) return;

    String packageName = "";

    final PsiDirectory[] dirs = view.getDirectories();
    final PsiDirectory dir = dirs.length == 1 ? dirs[0] : null;
    if (dir != null) {
      PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(dir);
      if (aPackage != null) {
        packageName = aPackage.getQualifiedName();
      }
      else {
        VirtualFile confDirectory = GrailsUtils.findConfDirectory(application);
        if (confDirectory != null) {
          VirtualFile vDir = dir.getVirtualFile();
          if (VfsUtilCore.isAncestor(confDirectory, vDir, true)) {
            packageName = VfsUtilCore.getRelativePath(vDir, confDirectory, '.');
          }
        }
      }
    }
    else {
      String aPackage = GrailsActionUtilKt.getArtefactPackage(e.getDataContext());
      if (aPackage != null) packageName = aPackage;
    }

    String artefactFqn = StringUtil.getQualifiedName(packageName, name);
    if (checkExisting(application, artefactFqn)) return;

    doAction(application, artefactFqn);
  }

  protected String getArtefactName(@NotNull AnActionEvent e, @NotNull Project project) {
    if (!e.isFromActionToolbar()) {
      final String inputTitle = GrailsBundle.message("new.artifact.action.title", e.getPresentation().getText());
      return Messages.showInputDialog(
        project, GrailsBundle.message("dialog.message.name"), inputTitle, Messages.getQuestionIcon(), "", new MyInputValidator(project, inputTitle)
      );
    }
    else {
      ArtefactData artefactData = GrailsActionUtilKt.getArtefactData(e.getDataContext());
      return artefactData == null ? null : artefactData.getArtefactName();
    }
  }

  protected void doAction(@NotNull GrailsApplication application, @NotNull String name) {
    final MvcCommand mvcCommand = new MvcCommand(getCommand(application), name);
    GrailsCommandExecutorUtil.execute(application, mvcCommand, () -> afterCommand(application, name), true);
  }

  private void afterCommand(@NotNull GrailsApplication application, @NotNull String name) {
    final VirtualFile targetDirectory = getTargetDirectory(application);
    if (targetDirectory == null) {
      return;
    }
    VfsUtil.markDirty(true, false, targetDirectory);
    targetDirectory.refresh(true, true, () -> {
      if (!application.isValid()) {
        return;
      }
      List<VirtualFile> files = findExistingGeneratedFiles(application, name);
      if (files.isEmpty()) {
        return;
      }
      final Project project = application.getProject();
      FileEditorManager.getInstance(project).openFile(files.get(0), true);
    });
  }

  protected boolean isEnabled(AnActionEvent e) {
    final GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(e.getDataContext());
    if (application == null) return false;
    if (e.getData(LangDataKeys.IDE_VIEW) == null) return false;

    final VirtualFile targetDirectory = getTargetDirectory(application);
    if (targetDirectory == null) {
      return false;
    }

    if (!e.isFromActionToolbar()) {
      GrailsArtefactHandler expectedHandler = getArtefactHandler();
      if (expectedHandler == null || GrailsActionUtilKt.getArtefactHandler(e.getDataContext()) != expectedHandler) {
        VirtualFile vfile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (vfile == null) return false;
        if (!vfile.isDirectory()) {
          vfile = vfile.getParent();
          if (vfile == null) {
            return false;
          }
        }

        if (!VfsUtilCore.isAncestor(targetDirectory, vfile, false) && !VfsUtilCore.isAncestor(vfile, targetDirectory, false)) {
          return false;
        }
      }
    }
    else {
      if (GrailsActionUtilKt.getArtefactData(e.getDataContext()) == null) return false;
    }

    return isEnabled(application);
  }

  @Override
  public final void update(@NotNull AnActionEvent e) {
    final boolean enabled = isEnabled(e);
    final Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(enabled);
    if (enabled && !e.isFromActionToolbar()) {
      presentation.setText(GrailsBundle.messagePointer(myPopupTextKey));
    }
    presentation.putClientProperty(WeighingNewActionGroup.WEIGHT_KEY, WeighingNewActionGroup.HIGHER_WEIGHT);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  private final class MyInputValidator implements InputValidator {
    private final Project myProject;
    private final @DialogTitle String myInputTitle;

    private MyInputValidator(Project project, @DialogTitle String inputTitle) {
      myProject = project;
      myInputTitle = inputTitle;
    }

    @Override
    public boolean canClose(String inputString) {
      if (!checkInput(inputString)) return false;
      String errorMessage = isValidIdentifier(inputString, myProject);
      if (errorMessage != null) {
        Messages.showErrorDialog(myProject, errorMessage, myInputTitle);
        return false;
      }
      return true;
    }

    @Override
    public boolean checkInput(String inputString) {
      return !inputString.isEmpty();
    }
  }

  protected @Nullable @NlsContexts.DialogMessage String isValidIdentifier(final String inputString, final Project project) {
    if (PsiNameHelper.getInstance(project).isQualifiedName(inputString)) {
      return null;
    }
    return GrailsBundle.message("dialog.message.valid.class.name.check");
  }

  //return if canceled
  protected boolean checkExisting(@NotNull GrailsApplication application, @NotNull String name) {
    final List<VirtualFile> existing = findExistingGeneratedFiles(application, name);
    if (existing.isEmpty()) {
      return false;
    }
    StringBuilder paths = new StringBuilder();
    for (VirtualFile file : existing) {
      paths.append("   ").append(file.getPath()).append("\n");
    }
    String message = GrailsBundle.message("generate.dlg.exist", paths.toString());
    return Messages.showYesNoDialog(application.getProject(), message, GrailsBundle.message("dialog.title.conflict"), Messages.getQuestionIcon()) != Messages.YES;
  }

  protected List<VirtualFile> findExistingGeneratedFiles(GrailsApplication application, String name) {
    if (name.indexOf('.') == -1 && application.getGrailsVersion().isAtLeast("1.2.2")) {
      // See _GrailsCreateArtifacts.createRootPackage()
      name = StringUtil.toLowerCase(application.getRoot().getName().replace('-', '.')) + '.' + name;
    }
    final List<String> list = getGeneratedFileNames(name);
    final ArrayList<VirtualFile> files = new ArrayList<>();
    final VirtualFile dir = application.getRoot();
    for (String fileName : list) {
      VirtualFile file = dir.findFileByRelativePath(fileName);
      if (file != null) files.add(file);
    }
    return files;
  }

  private List<String> getGeneratedFileNames(String name) {
    ArrayList<String> names = new ArrayList<>();
    fillGeneratedNamesList(name, names);
    return names;
  }

  protected boolean isEnabled(@NotNull GrailsApplication application) {
    return true;
  }

  protected abstract @NotNull String getCommand(@NotNull GrailsApplication application);

  protected abstract @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application);

  protected abstract void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names);

  protected @Nullable GrailsArtefactHandler getArtefactHandler() {
    return null;
  }
}
