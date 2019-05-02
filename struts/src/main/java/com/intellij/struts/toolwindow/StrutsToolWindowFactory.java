package com.intellij.struts.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.struts.StrutsView;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yann C&eacute;bron
 */
public class StrutsToolWindowFactory implements ToolWindowFactory {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    final ContentManager contentManager = toolWindow.getContentManager();

    StrutsView strutsView = new StrutsView(project);
    final Content content = contentManager.getFactory().createContent(strutsView.getComponent(), null, false);
    content.setDisposer(strutsView);
    contentManager.addContent(content);

    strutsView.openDefault();
  }
}
