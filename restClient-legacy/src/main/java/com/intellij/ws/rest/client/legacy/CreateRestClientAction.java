package com.intellij.ws.rest.client.legacy;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import icons.RestClientIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public final class CreateRestClientAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    openRestClient(project);
  }

  public static RESTClient openRestClient(Project project) {
    ToolWindowManager manager = ToolWindowManager.getInstance(project);
    ToolWindowEx w = (ToolWindowEx)manager.getToolWindow(getRestClient());
    RESTClient form;
    if (w == null) {
      form = new RESTClient(project);
      w = (ToolWindowEx)manager.registerToolWindow(getRestClient(), true, ToolWindowAnchor.BOTTOM, form, true);
      w.setHelpId("reference.tool.windows.rest.client");
      final Content content = w.getContentManager().getFactory().createContent(form.getComponent(), "", false);
      content.setDisposer(form);
      content.setCloseable(false);
      w.getContentManager().addContent(content);
      w.setIcon(RestClientIcons.Rest_client_icon_small);
    }
    else {
      Content content = w.getContentManager().getContent(0);
      form = (RESTClient) content.getDisposer();
    }
    ToolWindowEx finalW = w;
    w.show(() -> {
      JComponent component = finalW.getComponent();
      int delta = component.getMinimumSize().height - component.getSize().height;
      if (delta > 0) {
        finalW.stretchHeight(delta);
      }
    });
    w.activate(null);
    return form;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    e.getPresentation().setEnabled(project != null);
  }

  public static String getRestClient() {
    return RestClientLegacyBundle.message("reference.tool.windows.rest.client");
  }
}