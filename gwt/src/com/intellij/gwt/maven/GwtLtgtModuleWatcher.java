package com.intellij.gwt.maven;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.ui.update.DebouncedUpdates;
import com.intellij.util.ui.update.UpdateQueue;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

public class GwtLtgtModuleWatcher implements Disposable {

  private final Project myProject;
  private final MavenProjectsManager myMavenProjectsManager;
  private final UpdateQueue<MavenProject> myUpdateQueue;
  private final GwtLtgtModuleImporter myMyLtgtModuleImporter = new GwtLtgtModuleImporter();

  public static GwtLtgtModuleWatcher getInstance(Project project) {
    return project.getService(GwtLtgtModuleWatcher.class);
  }

  protected GwtLtgtModuleWatcher(Project project, CoroutineScope scope) {
    myProject = project;
    myMavenProjectsManager = MavenProjectsManager.getInstance(project);
    int delayMillis = ApplicationManager.getApplication().isUnitTestMode() ? 0 : 1000;
    myUpdateQueue = DebouncedUpdates.<MavenProject>forScope(scope, "GwtLtgtModuleWatcher", delayMillis)
        .runBatchedDistinct(batch -> batch.forEach(this::performReimportModuleFile));

    PsiManager.getInstance(project).addPsiTreeChangeListener(new GwtLtgtModulePsiTreeChangeListener(), this);
  }

  @Override
  public void dispose() {
  }

  private void reimportModuleFile(MavenProject mavenProject) {
    myUpdateQueue.queue(mavenProject);
  }

  private void performReimportModuleFile(MavenProject mavenProject) {
    Module module = ReadAction.computeBlocking(() -> myMavenProjectsManager.findModule(mavenProject));
    if (module == null) return;

    myMavenProjectsManager.scheduleForceUpdateMavenProject(mavenProject);
  }

  private class GwtLtgtModulePsiTreeChangeListener extends PsiTreeChangeAdapter {

    private void processEvent(@NotNull PsiTreeChangeEvent event) {
      PsiFile file = event.getFile();
      if (file == null) return;

      VirtualFile virtualFile = file.getVirtualFile();
      Module module = ModuleUtilCore.findModuleForFile(virtualFile, myProject);
      if (module == null || !myMavenProjectsManager.isMavenizedModule(module)) return;

      MavenProject mavenProject = myMavenProjectsManager.findProject(module);
      if (mavenProject == null) return;

      if (!myMyLtgtModuleImporter.isApplicable(mavenProject)) return;
      if (!myMyLtgtModuleImporter.isModuleTemplate(mavenProject, virtualFile)) return;

      reimportModuleFile(mavenProject);
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }
  }
}
