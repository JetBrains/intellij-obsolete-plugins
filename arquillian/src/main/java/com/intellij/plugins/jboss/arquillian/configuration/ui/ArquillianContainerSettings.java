package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.google.common.primitives.Ints;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainer;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainerModel;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianExistLibraryModel;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianLibraryModel;
import com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.ui.RemoteDebuggerPanel;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.containers.JBIterable;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.action.OpenBrowserAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

public class ArquillianContainerSettings {
  private final @NotNull Project project;
  private final @NotNull ArquillianContainer container;
  private final @NotNull ArquillianContainerModel model;

  private JPanel mainPanel;
  private JPanel librariesPanel;
  private JPanel containerNamePanel;
  private JPanel bottomPanel;
  private JPanel remoteDebugPanel;
  private JBTextField runContainerQualifier;
  private JBTextField debugContainerQualifier;

  private JBList repositoryLibraries;

  private LabeledComponent<RawCommandLineEditor> myVMParametersComponent;
  private EnvironmentVariablesComponent myEnvVariablesComponent;

  public ArquillianContainerSettings(@NotNull Project project,
                                     @NotNull ArquillianContainer container,
                                     @NotNull final ArquillianContainerModel model) {
    this.project = project;
    this.container = container;
    this.model = model;

    ListModel listModel = model.createListModel();
    //noinspection unchecked
    repositoryLibraries.setModel(listModel);

    String descriptionUri = container.getDescriptionUri();
    if (descriptionUri == null) {
      containerNamePanel.add(new JLabel(container.getName()));
    }
    else {
      OpenBrowserAction linkAction = new OpenBrowserAction(descriptionUri);
      JXHyperlink hyperlink = new JXHyperlink(linkAction);
      hyperlink.setText(container.getName());
      containerNamePanel.add(hyperlink);
    }
    librariesPanel.setBorder(IdeBorderFactory.createTitledBorder(ArquillianBundle.message("arquillian.libraries.title")));

    initComponents();

    bottomPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 0, 5, true, false));
    bottomPanel.add(myVMParametersComponent);
    bottomPanel.add(myEnvVariablesComponent);

    debugContainerQualifier.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        model.getRemoteDebuggerModel().setDebugContainerQualifier(debugContainerQualifier.getText());
      }
    });
    debugContainerQualifier.setText(model.getRemoteDebuggerModel().getDebugContainerQualifier());

    runContainerQualifier.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        model.getRemoteDebuggerModel().setRunContainerQualifier(runContainerQualifier.getText());
      }
    });
    runContainerQualifier.setText(model.getRemoteDebuggerModel().getRunContainerQualifier());
  }

  private void initComponents() {
    myVMParametersComponent = LabeledComponent.create(new RawCommandLineEditor(),
                                                      ExecutionBundle.message("run.configuration.java.vm.parameters.label"),
                                                      BorderLayout.WEST);
    myVMParametersComponent.getComponent().setText(model.getJvmParameters());
    myVMParametersComponent.getComponent().getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        model.setJvmParameters(myVMParametersComponent.getComponent().getText());
      }
    });

    copyDialogCaption(myVMParametersComponent);
    myEnvVariablesComponent = new EnvironmentVariablesComponent();
    myEnvVariablesComponent.setLabelLocation(BorderLayout.WEST);
    myEnvVariablesComponent.setEnvs(model.getEnvVariables());
    myEnvVariablesComponent.getComponent().getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        model.setEnvVariables(myEnvVariablesComponent.getEnvs());
      }
    });
    myEnvVariablesComponent.addChangeListener(e -> model.setEnvVariables(myEnvVariablesComponent.getEnvs()));

    myVMParametersComponent.setAnchor(myEnvVariablesComponent.getLabel());

    RemoteDebuggerPanel remoteDebuggerPanel = new RemoteDebuggerPanel(model.getRemoteDebuggerModel());
    remoteDebugPanel.add(remoteDebuggerPanel.getMainPanel());
  }

  protected void copyDialogCaption(LabeledComponent<RawCommandLineEditor> component) {
    RawCommandLineEditor rawCommandLineEditor = component.getComponent();
    rawCommandLineEditor.setDialogCaption(component.getRawText());
    component.getLabel().setLabelFor(rawCommandLineEditor.getTextField());
  }

  private void createUIComponents() {
    repositoryLibraries = new JBList();
    repositoryLibraries.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        ArquillianLibraryModel model = (ArquillianLibraryModel)value;
        setText(model.getDescription());
        Icon icon = model.getIcon();
        setForeground(icon == null ? JBColor.RED : JBColor.BLACK);
        setIcon(icon);
        return this;
      }
    });
    repositoryLibraries.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent evt) {
        if (evt.getClickCount() < 2) {
          return;
        }
        int index = repositoryLibraries.locationToIndex(evt.getPoint());
        model.getChildren().get(index).editProperties(project);
      }
    });

    ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(repositoryLibraries)
      .disableUpDownActions()
      .setToolbarPosition(ActionToolbarPosition.RIGHT)
      .setAddActionName(ArquillianBundle.message("arquillian.container.add.library"))
      .setAddActionUpdater(e -> container.canChangeDependencyList())
      .setAddAction(button -> {
        //noinspection ConstantConditions
        JBPopupFactory.getInstance().createListPopup(
          new BaseListPopupStep<>(
            ArquillianBundle.message("arquillian.libraries.type"),
            new AddExistLibraryAction(project),
            new AddMavenDependencyAction(project, button.getContextComponent())) {
            @Override
            public Icon getIconFor(AddLibraryAction action) {
              return action.getIcon();
            }

            @Override
            public boolean hasSubstep(com.intellij.plugins.jboss.arquillian.configuration.ui.AddLibraryAction selectedValue) {
              return super.hasSubstep(selectedValue);
            }

            @Override
            public boolean isMnemonicsNavigationEnabled() {
              return true;
            }

            @Override
            public PopupStep onChosen(final AddLibraryAction selectedValue, final boolean finalChoice) {
              return doFinalStep(() -> addLibraries(selectedValue.execute()));
            }

            @Override
            @NotNull
            public String getTextFor(AddLibraryAction action) {
              return action.getText();
            }
          }).show(button.getPreferredPopupPoint());
      })
      .setRemoveActionName(ArquillianBundle.message("arquillian.container.remove.library"))
      .setRemoveActionUpdater(e -> container.canChangeDependencyList() && repositoryLibraries.getSelectedIndex() != -1)
      .setRemoveAction(button -> {
        List<ArquillianLibraryModel> items = JBIterable.from(Ints.asList(repositoryLibraries.getSelectedIndices()))
          .transform(i -> model.getChildren().get(i)).toList();
        for (ArquillianLibraryModel item : items) {
          model.removeItem(item);
        }
      })
      .setEditActionName(ArquillianBundle.message("arquillian.container.edit.library"))
      .setEditActionUpdater(e -> repositoryLibraries.getSelectedIndices().length == 1
                             && !(repositoryLibraries.getSelectedValue() instanceof ArquillianExistLibraryModel))
      .setEditAction(button -> ((ArquillianLibraryModel)repositoryLibraries.getSelectedValue()).editProperties(project));

    librariesPanel = toolbarDecorator.createPanel();
  }

  private void addLibraries(Collection<ArquillianLibraryModel> libraries) {
    if (libraries == null) {
      return;
    }
    for (ArquillianLibraryModel library : libraries) {
      model.addItem(library);
    }
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
