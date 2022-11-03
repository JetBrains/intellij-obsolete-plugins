package com.intellij.dmserver.libraries;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;

public class ServerLibrariesContext {

  private final FrameworkInstanceDefinition myFramework;
  private final Project myProject;
  private final DMServerInstallation myInstallation;

  public ServerLibrariesContext(@NotNull FrameworkInstanceDefinition framework,
                                Project project,
                                @NotNull DMServerInstallation installation) {
    myFramework = framework;
    myProject = project;
    myInstallation = installation;
  }

  public FrameworkInstanceDefinition getFramework() {
    return myFramework;
  }

  public DMServerInstallation getInstallation() {
    return myInstallation;
  }

  public Project getProject() {
    return myProject;
  }
}
