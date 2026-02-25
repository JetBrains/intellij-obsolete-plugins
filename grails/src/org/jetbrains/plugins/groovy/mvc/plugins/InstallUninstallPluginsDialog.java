// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstallUninstallPluginsDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(InstallUninstallPluginsDialog.class);
  private final JPanel myMainPanel;

  private static final @NonNls String INSTALL_PLUGIN = "install-plugin";
  private static final @NonNls String UNINSTALL_PLUGIN = "uninstall-plugin";

  private final @NotNull OldGrailsApplication myApplication;

  private JPanel myInstallPluginsPanel;
  private JPanel myRemovePluginsPanel;

  private final Map<MvcPluginDescriptor, JComboBox> myVersionsMap;

  private final Collection<MvcPluginDescriptor> myToRemovePlugins;
  private final Collection<MvcPluginDescriptor> myToInstallCustomPlugins;
  private final Collection<MvcPluginDescriptor> myToInstallServerPlugins;

  private final Map<MvcPluginDescriptor, String> myPluginToPath;

  public InstallUninstallPluginsDialog(final Collection<MvcPluginDescriptor> toInstallServerPlugins,
                                       final Collection<MvcPluginDescriptor> toInstallCustomPlugins,
                                       final Collection<MvcPluginDescriptor> toRemovePlugins,
                                       @NotNull OldGrailsApplication application,
                                       final Map<MvcPluginDescriptor, String> pluginToPath) {
    super(application.getProject(), false);

    myToInstallServerPlugins = toInstallServerPlugins;
    myToInstallCustomPlugins = toInstallCustomPlugins;
    myToRemovePlugins = toRemovePlugins;
    myApplication = application;

    setTitle(GrailsBundle.message("install.uninstall.plugins.dialog.title"));

    myMainPanel = new JPanel();
    myMainPanel.setLayout(new BorderLayout());
    myVersionsMap = new HashMap<>();

    if (!myToRemovePlugins.isEmpty()) {
      myRemovePluginsPanel = new JPanel(new GridLayout(-1, 1));
      configureRemovePanel(toRemovePlugins);
      myMainPanel.add(myRemovePluginsPanel, BorderLayout.NORTH);
    }

    myMainPanel.add(new JLabel(" "), BorderLayout.CENTER);

    if (!myToInstallServerPlugins.isEmpty() || !myToInstallCustomPlugins.isEmpty()) {
      myInstallPluginsPanel = new JPanel(new BorderLayout());
      configureInstallPanel();
      myMainPanel.add(myInstallPluginsPanel, BorderLayout.SOUTH);
    }

    init();

    this.myPluginToPath = pluginToPath;
  }

  private void configureRemovePanel(final Collection<MvcPluginDescriptor> toRemovePlugins) {
    JLabel textLabel = new JLabel();
    textLabel.setText(GrailsBundle.message("install.uninstall.plugins.dialog.to.remove"));
    myRemovePluginsPanel.add(textLabel);

    JLabel pluginLabel;
    for (MvcPluginDescriptor toRemovePlugin : toRemovePlugins) {
      pluginLabel = createBoldLabel();
      pluginLabel.setForeground(MvcPluginUtil.COLOR_REMOVE_PLUGIN.get());
      pluginLabel.setText(toRemovePlugin.getName());
      myRemovePluginsPanel.add(pluginLabel);
    }
  }

  private void configureInstallPanel() {
    JLabel label = new JLabel();
    label.setText(GrailsBundle.message("install.uninstall.plugins.dialog.to.install"));
    myInstallPluginsPanel.add(label, BorderLayout.NORTH);
    final JPanel versionsPanel = new JPanel(new GridLayout(-1, 2));

    List<MvcPluginDescriptor> allPlugins = new ArrayList<>(myToInstallCustomPlugins.size() + myToInstallServerPlugins.size());
    allPlugins.addAll(myToInstallCustomPlugins);
    allPlugins.addAll(myToInstallServerPlugins);

    //Labels and versions here
    for (MvcPluginDescriptor toInstallPlugin : allPlugins) {
      JLabel pluginNameLabel = createBoldLabel();
      pluginNameLabel.setForeground(MvcPluginUtil.COLOR_INSTALL_PLUGIN.get());

      pluginNameLabel.setText(toInstallPlugin.getName());
      versionsPanel.add(pluginNameLabel);

      JComponent component = createVersionsComponent(toInstallPlugin);
      if (component instanceof JComboBox) {
        myVersionsMap.put(toInstallPlugin, (JComboBox)component);
      }
      versionsPanel.add(component);
    }
    myInstallPluginsPanel.add(versionsPanel, BorderLayout.CENTER);
  }

  private static JLabel createBoldLabel() {
    JLabel label = new JLabel();
    Font f = label.getFont();


    label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
    return label;
  }

  private static JComponent createVersionsComponent(MvcPluginDescriptor mvcPlugin) {
    JComboBox<MvcPluginDescriptor.Release> comboBox = new ComboBox<>();

    List<MvcPluginDescriptor.Release> versions = mvcPlugin.getReleases();

    if (versions.isEmpty()) {
      JLabel label = new JLabel();

      label.setForeground(JBColor.RED);
      label.setText(GrailsBundle.message("install.uninstall.plugins.dialog.no.version"));

      return label;
    }

    //editable and adding versions
    for (MvcPluginDescriptor.Release version : versions) {
      comboBox.addItem(version);
    }

    comboBox.setSelectedItem(mvcPlugin.getLastRelease());

    comboBox.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      if ("zip".equals(value.getType())) {
        label.setText(GrailsBundle.message("install.uninstall.plugins.dialog.zip.release.version", value.getVersion()));
      }
      else if (value == value.getPlugin().getLastRelease()) {
        label.setText(GrailsBundle.message("install.uninstall.plugins.dialog.latest.version", value.getVersion()));
      }
      else {
        label.setText(value.getVersion());
      }
    }));

    return comboBox;
  }

  public void doInstallRemove() {
    deletePlugins();
    installCustomPlugins();

    for (final MvcPluginDescriptor plugin : myToInstallServerPlugins) {
      final MvcCommand mvcCommand = new MvcCommand(INSTALL_PLUGIN, plugin.getName(), getPluginVersion(plugin));
      GrailsCommandExecutorUtil.execute(myApplication, mvcCommand, null, false);
    }
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  private void installCustomPlugins() {
    final List<VirtualFile> tempPluginPaths = new ArrayList<>();

    for (MvcPluginDescriptor mvcPlugin : myToInstallCustomPlugins) {
      final String pluginPath = MvcPluginUtil.cleanPath(myPluginToPath.get(mvcPlugin));

      final VirtualFile pluginVirtualFile = VirtualFileManager.getInstance().findFileByUrl("file://" + pluginPath);
      assert pluginVirtualFile != null;

      final VirtualFile appDirVirtualFile = myApplication.getRoot();

      try {
        WriteAction.run(() -> tempPluginPaths.add(VfsUtilCore.copyFile(this, pluginVirtualFile, appDirVirtualFile)));
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    for (final VirtualFile tempPluginPath : tempPluginPaths) {
      final MvcCommand mvcCommand = new MvcCommand(INSTALL_PLUGIN, tempPluginPath.getName());
      GrailsCommandExecutorUtil.execute(myApplication, mvcCommand, () -> {
        try {
          WriteAction.run(() -> LocalFileSystem.getInstance().deleteFile(this, tempPluginPath));
        }
        catch (IOException e) {
          LOG.error("Cannot delete file " + tempPluginPath.getPath(), e);
        }
      }, false);
    }
  }

  private String getPluginVersion(MvcPluginDescriptor plugin) {
    final JComboBox versionComponent = myVersionsMap.get(plugin);

    if (versionComponent != null) {
      return ((MvcPluginDescriptor.Release)versionComponent.getSelectedItem()).getVersion();
    }

    return "'";
  }

  private void deletePlugins() {
    if (myApplication.getGrailsVersion().isAtLeast(Version.GRAILS_1_1)) {
      for (final MvcPluginDescriptor plugin : myToRemovePlugins) {
        final MvcCommand mvcCommand = new MvcCommand(UNINSTALL_PLUGIN, plugin.getName());
        GrailsCommandExecutorUtil.execute(myApplication, mvcCommand, null, false);
      }
    }
    else {
      final Project project = myApplication.getProject();

      try {
        WriteCommandAction.writeCommandAction(project).run(() -> {
          final PropertiesFile properties = myApplication.getApplicationProperties();
          if (properties == null) {
            return;
          }

          final Collection<VirtualFile> allDirs = GrailsFramework.getInstance().getCommonPluginRoots(myApplication.getModule(), true);

          final VirtualFile propVFile = properties.getVirtualFile();
          final Document propDocument = PsiDocumentManager.getInstance(project).getDocument(properties.getContainingFile());

          propVFile.refresh(false, false);
          if (propDocument != null) {
            PsiDocumentManager.getInstance(project).commitDocument(propDocument);
          }

          for (final MvcPluginDescriptor plugin : myToRemovePlugins) {
            final VirtualFile pluginDir = ContainerUtil.find(allDirs, file -> plugin.getName().equals(file.getName()));

            if (pluginDir != null) {
              pluginDir.delete(this);
            }

            for (IProperty property : properties.findPropertiesByKey("plugins." + plugin.getName())) {
              property.getPsiElement().delete();
            }
          }

          if (propDocument != null) {
            FileDocumentManager.getInstance().saveDocument(propDocument);
          }
        });
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

}
