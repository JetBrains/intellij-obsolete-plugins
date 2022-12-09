package com.intellij.dmserver.shell;

import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public final class DmShellToolWindowFactory {
  private static final Logger LOG = Logger.getInstance(DmShellToolWindowFactory.class);

  public static void addServer(CommonModel model, ToolWindow toolWindow, DMServerInstance serverInstance) {
    DmShellToolWindowPanel component = new DmShellToolWindowPanel(model.getProject(), serverInstance);
    Content content = ContentFactory.getInstance().createContent(component, model.getApplicationServer().getName(), false);
    toolWindow.getContentManager().addContent(content);
    toolWindow.getContentManager().setSelectedContent(content);
    toolWindow.setAvailable(true);
  }

  public static void removeServer(ToolWindow toolWindow, DMServerInstance serverInstance) {
    Content serverContent = null;
    for (Content content : toolWindow.getContentManager().getContents()) {
      if (content.getComponent() instanceof DmShellToolWindowPanel) {
        if (((DmShellToolWindowPanel)content.getComponent()).getServerInstance() == serverInstance) {
          serverContent = content;
          break;
        }
      }
      else {
        // TODO: fix - should be always DmShellToolWindowPanel (?)
        LOG.error("Alien component found looking for DmShellToolWindowPanel: " + content.getComponent() + "\n"
                  + "Content : " + content);
      }
    }

    if (serverContent == null) {
      return;
    }

    toolWindow.getContentManager().removeContent(serverContent, true);
    if (toolWindow.getContentManager().getContentCount() == 0) {
      toolWindow.setAvailable(false);
    }
  }
}
