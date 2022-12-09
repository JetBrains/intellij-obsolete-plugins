package com.intellij.dmserver.libraries;

import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.osmorc.facet.OsmorcFacet;

public class ManageServerLibrariesAction extends AnAction {
  @NonNls
  public static final String ACTION_ID = "dmServer.AddFromOrb";

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    LibrariesDialogCreator.showDialog(project, null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    boolean visible = false;
    if (module != null) {
      DMBundleFacet dmBundleFacet = FacetManager.getInstance(module).getFacetByType(DMBundleFacet.ID);
      OsmorcFacet osmorcFacet = OsmorcFacet.getInstance(module);
      visible = dmBundleFacet != null &&
                osmorcFacet != null &&
                LibrariesDialogCreator.isDialogAvailable(module.getProject());
    }
    e.getPresentation().setVisible(visible);
    e.getPresentation().setText(DmServerBundle.messagePointer("ManageServerLibrariesAction.title"));
  }
}