// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginDescriptor;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginUtil;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginsMain;

public class AddCustomPluginAction extends AnAction implements DumbAware {
  private final MvcPluginsMain myMvcPluginsMain;

  public AddCustomPluginAction(final MvcPluginsMain mvcPluginsMain) {
    super(GrailsBundle.message("mvc.plugins.action.text.add.custom.plugin"),
          GrailsBundle.message("mvc.plugins.action.description.add.custom.plugin"), IconUtil.getAddIcon());
    myMvcPluginsMain = mvcPluginsMain;
  }

  @Override
  public void actionPerformed(final @NotNull AnActionEvent e) {
    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("jar");
    final VirtualFile[] files = FileChooser.chooseFiles(descriptor, myMvcPluginsMain.getProject(), null);

    if (files.length > 0) {
      String pathToPlugin = files[0].getPath();

      MvcPluginDescriptor plugin = MvcPluginUtil.extractPluginInfo(pathToPlugin);
      if (plugin == null) {
        Messages.showErrorDialog(GrailsBundle.message("mvc.plugins.dialog.message.failed.to.read.plugin.archive"),
                                 GrailsBundle.message("mvc.plugins.dialog.title.failed.to.read.plugin.archive"));
        return;
      }

      myMvcPluginsMain.addCustomPlugin(plugin, pathToPlugin);
      myMvcPluginsMain.markInstalled(plugin.getName());

      myMvcPluginsMain.getFilter().setFilter("");
    }
  }
}
