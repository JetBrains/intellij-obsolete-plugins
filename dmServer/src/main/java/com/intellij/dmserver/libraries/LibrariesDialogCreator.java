package com.intellij.dmserver.libraries;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.osmorc.FrameworkUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;

public final class LibrariesDialogCreator {

  private static final LibrariesDialogCreator ourInstance = new LibrariesDialogCreator();

  private LibrariesDialogCreator() {

  }

  public static LibrariesDialogCreator getInstance() {
    return ourInstance;
  }

  public static boolean isDialogAvailable(Project project) {
    final Ref<Boolean> result = new Ref<>(false);
    new Checker() {

      @Override
      protected void onAvailable(FrameworkInstanceDefinition framework, DMServerInstallation installation) {
        result.set(true);
      }
    }.check(project);
    return result.get();
  }

  public static void showDialog(final Project project, @Nullable final String packageName) {
    new Checker() {

      @Override
      protected void onAvailable(FrameworkInstanceDefinition framework, DMServerInstallation installation) {
        LibrariesDialog dialog = createDialog(framework, project, installation);
        if (packageName != null) {
          dialog.initSearch(packageName);
        }
        dialog.show();
      }
    }.check(project);
  }

  public static LibrariesDialog createDialog(@NotNull FrameworkInstanceDefinition framework,
                                             Project project,
                                             @NotNull DMServerInstallation installation) {
    ServerLibrariesContext context = new ServerLibrariesContext(framework, project, installation);
    return new LibrariesDialog(context);
  }

  private static abstract class Checker {

    public void check(Project project) {
      FrameworkInstanceDefinition framework = FrameworkUtils.getInstance().getActiveFrameworkInstance(project);
      if (framework == null) {
        return;
      }
      DMServerInstallation installation = FrameworkUtils.getInstance().getDMServerInstallation(framework);
      if (installation == null) {
        return;
      }
      onAvailable(framework, installation);
    }

    protected abstract void onAvailable(FrameworkInstanceDefinition framework, DMServerInstallation installation);
  }
}
