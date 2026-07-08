package com.intellij.lang.puppet.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;

class PuppetDumbModeListener implements DumbService.DumbModeListener {
  private static final Logger LOG = Logger.getInstance(PuppetDumbModeListener.class);

  private final Project myProject;

  PuppetDumbModeListener(Project project) {
    myProject = project;
  }

  @Override
  public void exitDumbMode() {
    LOG.debug("Left dumb mode");
    PuppetProjectManager.getInstance(myProject).queueRescanProjectStructure();
  }
}
