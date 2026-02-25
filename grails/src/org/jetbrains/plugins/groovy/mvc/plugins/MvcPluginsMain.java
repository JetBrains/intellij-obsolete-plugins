// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ClickListener;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.ui.TableUtil;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.HttpProxyConfigurable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.runner.GrailsConsole;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.groovy.mvc.plugins.actions.AddCustomPluginAction;
import org.jetbrains.plugins.groovy.mvc.plugins.actions.ReloadMvcPluginListAction;
import org.jdom.Element;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MvcPluginsMain {
  private JPanel main;
  private JLabel myAuthorEmailLabel;
  private JLabel myAuthorLabel;
  private JEditorPane myDescriptionTextArea;
  private JLabel myPluginUrlLabel;
  private JLabel myVersionLabel;
  private JPanel myTablePanel;
  private JPanel myToolbarPanel;
  private JButton myHttpProxySettingsButton;
  private JPanel myPanelForTable;

  private final AvailablePluginsModel myAvailablePluginsModel;
  private final MvcPluginsTable myAvailablePluginsTable;
  private final ActionToolbar myActionToolbar;

  private final MyPluginsFilter myFilter = new MyPluginsFilter();
  private final @NotNull OldGrailsApplication myApplication;
  private final DialogBuilder myDialogBuilder;

  public static final @NonNls String TEXT_PREFIX = "<html><body style=\"font-family: Arial; font-size: 12pt;\">";
  public static final @NonNls String TEXT_SUFFIX = "</body></html>";

  private static final @NonNls String HTML_PREFIX = "<html><body><a href=\"\">";
  private static final @NonNls String HTML_SUFFIX = "</a></body></html>";

  private final Map<MvcPluginDescriptor, String> myCustomPlugins = new HashMap<>();
  private final Map<String, MvcPluginDescriptor> myPluginDescriptions = new HashMap<>();

  public MvcPluginsMain(@NotNull OldGrailsApplication application, @NotNull DialogBuilder dialogBuilder) {
    myApplication = application;
    myDialogBuilder = dialogBuilder;

    myDescriptionTextArea.addHyperlinkListener(new PluginManagerMain.MyHyperlinkListener());

    main.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(final @NotNull ActionEvent e) {
        IdeFocusManager.getInstance(myApplication.getProject()).requestFocus(myFilter, true);
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    Set<String> installedPlugins = loadPluginInfo();

    myAvailablePluginsModel = new AvailablePluginsModel(installedPlugins, myPluginDescriptions.values());
    myAvailablePluginsTable = new MvcPluginsTable(myAvailablePluginsModel);
    //  Downloads
    JScrollPane availableScrollPane = ScrollPaneFactory.createScrollPane(myAvailablePluginsTable);

    final ActionGroup actionGroup = getActionGroup();
    installTableActions(myAvailablePluginsTable, actionGroup);

    myHttpProxySettingsButton.addActionListener(new ActionListener() {
      @SuppressWarnings("deprecation")
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        HttpConfigurable cfg;

        Element serializedCfg = new Element("root");
        try {
          HttpConfigurable.getInstance().writeExternal(serializedCfg);
          cfg = new HttpConfigurable();
          cfg.readExternal(serializedCfg);
        }
        catch (Exception e1) {
          throw new RuntimeException(e1);
        }

        doEditProxySettings(cfg);
      }
    });

    myTablePanel.setMinimumSize(new Dimension(350, -1));
    myDescriptionTextArea.setPreferredSize(new Dimension(-1, 300));

    myPanelForTable.setLayout(new BorderLayout());
    myPanelForTable.setBorder(IdeBorderFactory.createTitledBorder(GrailsBundle.message("mvc.plugins.dialog.title"), false));

    myPanelForTable.add(availableScrollPane, BorderLayout.CENTER);

    myToolbarPanel.setLayout(new BorderLayout());

    myActionToolbar = ActionManager.getInstance().createActionToolbar("MvcPluginManager", actionGroup, true);
    myToolbarPanel.add(myActionToolbar.getComponent(), BorderLayout.WEST);
    myToolbarPanel.add(myFilter, BorderLayout.EAST);
    final MvcPluginsTable pluginTable = getPluginTable();
    TableUtil.ensureSelectionExists(pluginTable);
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!StringUtil.isEmpty(myFilter.getFilter())) {
        myFilter.filter();
      }
    });
    dialogBuilder.setPreferredFocusComponent(myAvailablePluginsTable);

    GuiUtils.replaceJSplitPaneWithIDEASplitter(main);

    tableSelectionChanged(pluginTable);
  }

  private void doEditProxySettings(final HttpConfigurable cfg) {
    if (!ShowSettingsUtil.getInstance().editConfigurable(main, new HttpProxyConfigurable(cfg))) {
      return;
    }

    if (GrailsConsole.getInstance(getProject()).isExecuting()) {
      Messages.showErrorDialog(
              getProject(),
              GrailsBundle.message("mvc.plugins.dialog.message.failed.to.set.proxy"),
              GrailsBundle.message("mvc.plugins.dialog.title.failed.to.execute.command")
      );

      // Return to editing proxy setting. We have to use invokeLater to releasing EDT, it's needing for releasing MvcConsole when process will done.
      ApplicationManager.getApplication().invokeLater(() -> {
        if (myDialogBuilder.getWindow().isShowing()) {
          doEditProxySettings(cfg);
        }
      });

      return;
    }

    MvcPluginUtil.setFrameworkProxy(cfg, myApplication);
  }

  public void addCustomPlugin(MvcPluginDescriptor plugin, String path) {
    myCustomPlugins.put(plugin, path);
    myPluginDescriptions.put(plugin.getName(), plugin);

    myFilter.clear();
    myAvailablePluginsTable.getModel().setData(myPluginDescriptions.values());
  }

  public Map<MvcPluginDescriptor, String> getCustomPlugins() {
    return myCustomPlugins;
  }

  private Set<String> loadPluginInfo() {
    Set<String> installedPlugins = new HashSet<>();

    myPluginDescriptions.clear();

    for (MvcPluginDescriptor plugin : MvcPluginUtil.refreshAndLoadPluginList(myApplication)) {
      myPluginDescriptions.put(plugin.getName(), plugin);
    }

    for (VirtualFile pluginDir : GrailsFramework.getInstance().getAllPluginRoots(myApplication.getModule(), true)) {
      VirtualFile pluginXML = pluginDir.findChild("plugin.xml");
      if (pluginXML != null) {
        String dirName = pluginDir.getName();
        String pluginName = null;

        int index = dirName.lastIndexOf('-');

        while (index != -1) {
          String pluginNameCandidate = dirName.substring(0, index);
          if (myPluginDescriptions.containsKey(pluginNameCandidate)) {
            pluginName = pluginNameCandidate;
            break;
          }

          index = dirName.lastIndexOf('-', index - 1);
        }

        if (pluginName == null) {
          MvcPluginDescriptor plugin = MvcPluginUtil.parsePluginXml(pluginXML);
          if (plugin != null) {
            myPluginDescriptions.put(plugin.getName(), plugin);
            installedPlugins.add(plugin.getName());
          }
        }
        else {
          installedPlugins.add(pluginName);
        }
      }
    }

    for (MvcPluginDescriptor plugin : myCustomPlugins.keySet()) {
      myPluginDescriptions.put(plugin.getName(), plugin);
    }

    return installedPlugins;
  }

  private ActionGroup getActionGroup() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new AddCustomPluginAction(this));
    group.add(new ReloadMvcPluginListAction(this));
    return group;
  }

  public JComponent getMainPanel() {
    return main;
  }

  private class MyPluginsFilter extends FilterComponent {
    private final List<MvcPluginDescriptor> myFilteredAvailable = new ArrayList<>();

    MyPluginsFilter() {
      super("PLUGIN_FILTER", 5);
    }

    @Override
    public void filter() {
      filter(myAvailablePluginsModel, myFilteredAvailable);
    }

    private void filter(AvailablePluginsModel model, final List<MvcPluginDescriptor> filtered) {
      final String filter = StringUtil.toLowerCase(getFilter());
      final SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
      final Set<String> search = optionsRegistrar.getProcessedWords(filter);
      final ArrayList<MvcPluginDescriptor> current = new ArrayList<>();

      final LinkedHashSet<MvcPluginDescriptor> toBeProcessed = new LinkedHashSet<>(model.getAvailablePlugins());
      toBeProcessed.addAll(filtered);
      filtered.clear();
      for (MvcPluginDescriptor mvcPlugin : toBeProcessed) {
        if (StringUtil.containsIgnoreCase(mvcPlugin.getName(), filter)) {
          current.add(mvcPlugin);
          continue;
        }

        if (isAccepted(search, current, mvcPlugin, mvcPlugin.getName())) {
          continue;
        }

        MvcPluginDescriptor.Release release = mvcPlugin.getLastRelease();
        if (release != null) {
          if (isAccepted(search, current, mvcPlugin, release.getTitle())
             || isAccepted(search, current, mvcPlugin, release.getDescription())) {
            continue;
          }
        }

        filtered.add(mvcPlugin);
      }

      model.setData(current);
    }

    private static boolean isAccepted(final Set<String> search,
                                      final ArrayList<MvcPluginDescriptor> current,
                                      final MvcPluginDescriptor descriptor,
                                      final String description) {
      if (description == null) {
        return false;
      }
      final SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
      final HashSet<String> descriptionSet = new HashSet<>(search);
      descriptionSet.removeAll(optionsRegistrar.getProcessedWords(description));
      if (descriptionSet.isEmpty()) {
        current.add(descriptor);
        return true;
      }
      return false;
    }

    @Override
    public void dispose() {
      super.dispose();
      myFilteredAvailable.clear();
    }

    public void clear() {
      myFilteredAvailable.clear();
      setFilter("");
    }
  }

  private void installTableActions(final MvcPluginsTable pluginTable, final ActionGroup actionGroup) {
    myAvailablePluginsModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(final @NotNull TableModelEvent e) {
        if (TableModelEvent.UPDATE == e.getType()) {
          final MvcPluginIsInstalledColumnInfo pluginIsInstalledColumnInfo =
                (MvcPluginIsInstalledColumnInfo)myAvailablePluginsModel.getColumnInfos()[AvailablePluginsModel.COLUMN_IS_INSTALLED];

          myDialogBuilder.setOkActionEnabled(!pluginIsInstalledColumnInfo.getToInstallPlugins().isEmpty() ||
                                             !pluginIsInstalledColumnInfo.getToRemovePlugins().isEmpty());
        }
      }
    });

    pluginTable.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        final int[] selectedObjects = pluginTable.getSelectedRows();

        for (int selectedRowNum : selectedObjects) {
          final Object boolVal = myAvailablePluginsTable.getValueAt(selectedRowNum, AvailablePluginsModel.COLUMN_IS_INSTALLED);
          assert boolVal instanceof Boolean;

          myAvailablePluginsTable.setValueAt(!(Boolean)boolVal, selectedRowNum, AvailablePluginsModel.COLUMN_IS_INSTALLED);
          pluginTable.getSelectionModel().setSelectionInterval(selectedRowNum, selectedRowNum);
        }
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);

    pluginTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(@NotNull ListSelectionEvent e) {
        tableSelectionChanged(pluginTable);
      }
    });

    PopupHandler.installPopupMenu(pluginTable, actionGroup, "GroovyMvcPluginsPopup");

    myAuthorEmailLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        MvcPluginDescriptor plugin = getPluginTable().getSelectedObject();
        if (plugin != null && plugin.getLastRelease() != null) {
          launchBrowserAction(plugin.getLastRelease().getEmail(), "mailto:");
          return true;
        }
        return false;
      }
    }.installOn(myAuthorLabel);

    myPluginUrlLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        MvcPluginDescriptor plugin = getPluginTable().getSelectedObject();
        if (plugin != null && plugin.getLastRelease() != null) {
          launchBrowserAction(plugin.getLastRelease().getDocumentation(), "");
          return true;
        }
        return false;
      }
    }.installOn(myPluginUrlLabel);

    MySpeedSearchBar.installOn(pluginTable);
  }

  private void tableSelectionChanged(MvcPluginsTable pluginTable) {
    final MvcPluginDescriptor[] plugins = pluginTable.getSelectedObjects();
    MvcPluginDescriptor mvcPlugin = plugins.length == 1 ? plugins[0] : null;

    pluginInfoUpdate(mvcPlugin);

    ApplicationManager.getApplication().invokeLater(myActionToolbar::updateActionsImmediately);
  }

  public MvcPluginsTable getPluginTable() {
    return myAvailablePluginsTable;
  }

  private static void launchBrowserAction(String cmd, String prefix) {
    if (cmd != null && !cmd.trim().isEmpty()) {
      try {
        BrowserUtil.browse(prefix + cmd.trim());
      }
      catch (IllegalThreadStateException ex) {
        /* not a problem */
      }
    }
  }

  public @NotNull Project getProject() {
    return myApplication.getProject();
  }

  public @NotNull OldGrailsApplication getApplication() {
    return myApplication;
  }

  public void pluginInfoUpdate(MvcPluginDescriptor plugin) {
    if (plugin != null && plugin.getLastRelease() != null) {
      MvcPluginDescriptor.Release release = plugin.getLastRelease();

      myAuthorLabel.setText(release.getAuthor());
      final String fullDescription = release.getDescription();
      final @NlsSafe String descriptionWithMarkup =
        fullDescription != null ? SearchUtil.markup(fullDescription, myFilter.getFilter()) : null;
      setTextValue(descriptionWithMarkup, myDescriptionTextArea);
      setHtmlValue(release.getEmail(), myAuthorEmailLabel);
      setHtmlValue(release.getDocumentation(), myPluginUrlLabel);
      setTextValue(release.getVersion(), myVersionLabel);
    }
    else {
      myAuthorLabel.setText("");
      setTextValue(null, myDescriptionTextArea);
      myAuthorEmailLabel.setText("");
      myPluginUrlLabel.setText("");
      myVersionLabel.setText("");
    }
  }

  private static void setTextValue(@Nls String val, JEditorPane pane) {
    if (val != null) {
      val = val.trim();
      val = StringUtil.trimStart(val, "\\");
      val = val.trim().replace("\n", "<br>");
      @NlsSafe String text = TEXT_PREFIX + val + TEXT_SUFFIX;
      pane.setText(text);
      pane.setCaretPosition(0);
    }
    else {
      @NlsSafe String text = TEXT_PREFIX + TEXT_SUFFIX;
      pane.setText(text);
    }
  }

  private static void setTextValue(@NlsContexts.Label String val, JLabel label) {
    label.setText((val != null) ? val : "");
  }

  private static void setHtmlValue(final @Nls String val, JLabel label) {
    boolean isValid = (val != null && !val.trim().isEmpty());
    @NlsSafe String setVal = isValid ? HTML_PREFIX + val.trim() + HTML_SUFFIX :
                    GrailsBundle.message("mvc.plugins.label.value.not.specified");

    label.setText(setVal);
    label.setCursor(Cursor.getPredefinedCursor(isValid ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
  }

  public FilterComponent getFilter() {
    return myFilter;
  }

  public Map<String, MvcPluginDescriptor> getPluginDescriptions() {
    return myPluginDescriptions;
  }

  public void markInstalled(String pluginName) {
    final MvcPluginIsInstalledColumnInfo pluginIsInstalledColumnInfo =
      (MvcPluginIsInstalledColumnInfo) myAvailablePluginsModel.getColumnInfos()[AvailablePluginsModel.COLUMN_IS_INSTALLED];

    pluginIsInstalledColumnInfo.getToInstallPlugins().add(pluginName);
    myDialogBuilder.setOkActionEnabled(true);
  }

  public void reloadPlugins() {
    loadPluginInfo();

    myFilter.clear();
    myAvailablePluginsTable.getModel().setData(myPluginDescriptions.values());
  }

  private static class MySpeedSearchBar extends SpeedSearchBase<MvcPluginsTable> {
    private MySpeedSearchBar(MvcPluginsTable pluginsTable) {
      super(pluginsTable, null);
    }

    @Contract("_ -> new")
    static @NotNull MySpeedSearchBar installOn(MvcPluginsTable pluginsTable) {
      MySpeedSearchBar searchBar = new MySpeedSearchBar(pluginsTable);
      searchBar.setupListeners();
      return searchBar;
    }

    @Override
    public int getSelectedIndex() {
      return myComponent.getSelectedRow();
    }

    @Override
    protected int getElementCount() {
      return myComponent.getModel().getAvailablePlugins().size();
    }

    @Override
    protected Object getElementAt(int viewIndex) {
      return myComponent.getModel().getAvailablePlugins().get(getComponent().convertRowIndexToModel(viewIndex));
    }

    @Override
    public String getElementText(Object element) {
      return ((MvcPluginDescriptor)element).getName();
    }

    @Override
    public void selectElement(Object element, String selectedText) {
      for (int i = 0; i < myComponent.getRowCount(); i++) {
        if (myComponent.getPluginAt(i).getName().equals(((MvcPluginDescriptor)element).getName())) {
          myComponent.setRowSelectionInterval(i, i);
          TableUtil.scrollSelectionToVisible(myComponent);
          break;
        }
      }
    }
  }
}
