package com.intellij.dmserver.osmorc;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.DMServerInstallationManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.settings.ApplicationSettings;
import org.osmorc.settings.ProjectSettings;

public final class FrameworkUtils {
  private static final FrameworkUtils ourInstance = new FrameworkUtils();

  private FrameworkUtils() { }

  public static FrameworkUtils getInstance() {
    return ourInstance;
  }

  @Nullable
  public FrameworkInstanceDefinition getActiveFrameworkInstance(Project project) {
    ApplicationSettings applicationSettings = ApplicationManager.getApplication().getService(ApplicationSettings.class);
    ProjectSettings projectSettings = project.getService(ProjectSettings.class);
    return applicationSettings.getFrameworkInstance(projectSettings.getFrameworkInstanceName());
  }

  @Nullable
  public DMServerInstallation getActiveDMServerInstallation(Project project) {
    return getDMServerInstallation(getInstance().getActiveFrameworkInstance(project));
  }

  @Nullable
  public DMServerInstallation getDMServerInstallation(FrameworkInstanceDefinition framework) {
    if (!DMServerFrameworkIntegrator.isCompatibleInstance(framework)) {
      return null;
    }
    DMServerInstallationManager installationManager = DMServerInstallationManager.getInstance();
    DMServerInstallation result = installationManager.findInstallation(framework);
    return result != null && result.isValid() ? result : null;
  }

  public void setActiveFrameworkInstance(Project project, FrameworkInstanceDefinition framework) {
    ProjectSettings projectSettings = project.getService(ProjectSettings.class);
    projectSettings.setFrameworkInstanceName(framework == null ? null : framework.getName());
  }
}
