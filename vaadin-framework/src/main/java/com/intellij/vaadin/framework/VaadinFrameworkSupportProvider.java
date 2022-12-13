package com.intellij.vaadin.framework;

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.library.DownloadableLibraryService;
import com.intellij.framework.library.FrameworkLibraryVersion;
import com.intellij.framework.library.FrameworkLibraryVersionFilter;
import com.intellij.framework.library.LibraryBasedFrameworkSupportProvider;
import com.intellij.gwt.facet.GwtFacetFrameworkSupportProvider;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.BrowseFilesListener;
import com.intellij.ide.util.PackageUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportModelBase;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.web.CommonParamValue;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.Servlet;
import com.intellij.javaee.web.model.xml.ServletMapping;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.descriptors.ConfigFile;
import com.intellij.vaadin.VaadinBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VaadinFrameworkSupportProvider extends LibraryBasedFrameworkSupportProvider {
  public VaadinFrameworkSupportProvider(VaadinFrameworkType type) {
    super(type, VaadinLibraryType.class);
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
    return new VaadinFrameworkSupportConfigurable(model);
  }

  @Override
  public boolean isEnabledForModuleBuilder(@NotNull ModuleBuilder builder) {
    return "JavaEELegacy".equals(builder.getBuilderId());
  }

  private static class VaadinFrameworkSupportConfigurable extends FrameworkSupportInModuleConfigurable {
    private static final String PATH_TO_INSTALLATION_KEY = "PATH_TO_VAADIN_INSTALLATION";
    private final FrameworkSupportModel myModel;
    private JPanel myMainPanel;
    private JCheckBox myCreateSampleAppCheckBox;
    private JTextField myAppNameField;
    private ComboBox<VaadinVersionImpl> myVersionComboBox;
    private JPanel myVaadinSdkPanel;
    private TextFieldWithBrowseButton mySdkPathField;
    private HyperlinkLabel myErrorLabel;

    VaadinFrameworkSupportConfigurable(FrameworkSupportModel model) {
      myModel = model;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
      mySdkPathField.addBrowseFolderListener(VaadinBundle.message("dialog.title.path.to.vaadin.installation.directory"), null, null, BrowseFilesListener.SINGLE_DIRECTORY_DESCRIPTOR);
      mySdkPathField.setText(PropertiesComponent.getInstance().getValue(PATH_TO_INSTALLATION_KEY, ""));
      mySdkPathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          onSdkPathChanged();
        }
      });
      myErrorLabel.setHyperlinkTarget("https://vaadin.com/releases");
      myErrorLabel.setIcon(AllIcons.General.BalloonError);
      myVersionComboBox.setRenderer(SimpleListCellRenderer.create("", value -> value.getVersionName()));
      myVersionComboBox.setModel(new EnumComboBoxModel<>(VaadinVersionImpl.class));
      myVersionComboBox.setSelectedItem(VaadinVersionUtil.getDefaultVersion());
      myVersionComboBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          onVersionChanged();
        }
      });
      myCreateSampleAppCheckBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myAppNameField.setEnabled(myCreateSampleAppCheckBox.isSelected());
        }
      });
      onVersionChanged();
      return myMainPanel;
    }

    private void onVersionChanged() {
      myModel.updateFrameworkLibraryComponent(VaadinFrameworkType.ID);
      myVaadinSdkPanel.setVisible(getSelectedVersion().isFullDistributionRequired());
      onSdkPathChanged();
    }

    @Override
    public void onFrameworkSelectionChanged(boolean selected) {
      myAppNameField.setEnabled(selected && myCreateSampleAppCheckBox.isSelected());
    }

    private void onSdkPathChanged() {
      if (!getSelectedVersion().isFullDistributionRequired()) return;

      final String path = mySdkPathField.getText();
      if (StringUtil.isEmptyOrSpaces(path)) {
        myErrorLabel.setVisible(true);
        myErrorLabel.setHyperlinkText(VaadinBundle.message("link.label.path.to.vaadin.installation.not.specified"),
                                      VaadinBundle.message("link.label.download.vaadin"), "");
      }
      else {
        myErrorLabel.setVisible(false);

        FrameworkSupportConfigurable gwtConfigurable =
          myModel.findFrameworkConfigurable(FacetBasedFrameworkSupportProvider.getProviderId(GwtFacetType.ID));
        if (gwtConfigurable instanceof GwtFacetFrameworkSupportProvider.GwtFrameworkSupportConfigurable) {
          ((GwtFacetFrameworkSupportProvider.GwtFrameworkSupportConfigurable)gwtConfigurable).getSdkPathEditor().setPath(path);
        }
      }
      myMainPanel.repaint();
    }

    @Override
    public void addSupport(@NotNull final Module module,
                           @NotNull ModifiableRootModel rootModel,
                           @NotNull ModifiableModelsProvider modifiableModelsProvider) {
      final WebFacet webFacet = ContainerUtil.getFirstItem(WebFacet.getInstances(module));
      final Project project = module.getProject();
      final String applicationClassName;
      final VaadinVersion version = getSelectedVersion();
      Library serverLibrary = null;
      if (version.isFullDistributionRequired()) {
        String path = mySdkPathField.getText();
        if (!StringUtil.isEmpty(path)) {
          PropertiesComponent.getInstance().setValue(PATH_TO_INSTALLATION_KEY, path);
        }
        LibrariesContainer container = ((FrameworkSupportModelBase)myModel).getLibrariesContainer();
        if (!container.canCreateLibrary(LibrariesContainer.LibraryLevel.PROJECT)) {
          container = LibrariesContainerFactory.createContainer(project);
        }
        VirtualFile dir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(path));
        if (dir != null) {
          List<OrderRoot> clientRoots = new ArrayList<>();
          List<OrderRoot> serverRoots = new ArrayList<>();
          for (VirtualFile file : dir.getChildren()) {
            VirtualFile jar = JarFileSystem.getInstance().getJarRootForLocalFile(file);
            if (jar != null) {
              String fileName = file.getName();
              if (fileName.startsWith("vaadin-client") && !fileName.startsWith("vaadin-client-compiled")) {
                clientRoots.add(new OrderRoot(jar, OrderRootType.CLASSES));
                clientRoots.add(new OrderRoot(jar, OrderRootType.SOURCES));
              }
              else {
                serverRoots.add(new OrderRoot(jar, OrderRootType.CLASSES));
              }
            }
          }
          Library clientLibrary = findOrCreateLibrary("Vaadin Client", container, clientRoots);
          rootModel.addLibraryEntry(clientLibrary).setScope(DependencyScope.PROVIDED);
          VirtualFile libDir = dir.findChild("lib");
          if (libDir != null) {
            for (VirtualFile file : libDir.getChildren()) {
              VirtualFile jar = JarFileSystem.getInstance().getJarRootForLocalFile(file);
              if (jar != null && !jar.getName().startsWith("guava")) {
                serverRoots.add(new OrderRoot(jar, OrderRootType.CLASSES));
              }
            }
          }
          serverLibrary = findOrCreateLibrary("Vaadin Server", container, serverRoots);
          rootModel.addLibraryEntry(serverLibrary);
        }
      }
      if (myCreateSampleAppCheckBox.isSelected()) {
        applicationClassName = myAppNameField.getText().trim();
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> WriteCommandAction.writeCommandAction(project).run(() -> {
          String packageName = StringUtil.getPackageName(applicationClassName);
          PsiDirectory directory = PackageUtil.findOrCreateDirectoryForPackage(module, packageName, null, false);
          if (directory != null) {
            JavaDirectoryService.getInstance().createClass(directory, StringUtil.getShortName(applicationClassName),
                                                           version.getTemplateNames().getApplication());
          }
        }));
      }
      else {
        applicationClassName = null;
      }

      if (webFacet != null) {
        if (serverLibrary != null) {
          ArtifactManager artifactManager = ArtifactManager.getInstance(project);
          final PackagingElementResolvingContext context = artifactManager.getResolvingContext();
          Collection<Artifact> artifacts = JavaeeArtifactUtil.getInstance().getArtifactsContainingFacet(webFacet, context, WebArtifactUtil.getInstance().getWebArtifactTypes(), false);
          for (Artifact artifact : artifacts) {
            artifactManager.addElementsToDirectory(artifact, "/WEB-INF/lib", PackagingElementFactory.getInstance().createLibraryElements(serverLibrary));
          }
        }
        StartupManager.getInstance(project).runWhenProjectIsInitialized(new SetupVaadinInWebXmlRunnable(webFacet, version, applicationClassName));
      }
    }

    private static Library findOrCreateLibrary(final String libraryName, LibrariesContainer container, List<OrderRoot> clientRoots) {
      for (Library library : container.getLibraries(LibrariesContainer.LibraryLevel.PROJECT)) {
        if (libraryName.equals(library.getName())) {
          return library;
        }
      }
      return container.createLibrary(libraryName, LibrariesContainer.LibraryLevel.PROJECT, clientRoots);
    }

    @NotNull
    @Override
    public FrameworkLibraryVersionFilter getLibraryVersionFilter() {
      return new FrameworkLibraryVersionFilter() {
        @Override
        public boolean isAccepted(@NotNull FrameworkLibraryVersion version) {
          return !getSelectedVersion().isFullDistributionRequired();
        }
      };
    }

    private VaadinVersion getSelectedVersion() {
      return ObjectUtils.notNull((VaadinVersion)myVersionComboBox.getSelectedItem(), VaadinVersionUtil.getDefaultVersion());
    }

    @Override
    public boolean isVisible() {
      return !getSelectedVersion().isFullDistributionRequired();
    }

    @Nullable
    @Override
    public CustomLibraryDescription createLibraryDescription() {
      return DownloadableLibraryService.getInstance().createDescriptionForType(VaadinLibraryType.class);
    }

    private static class SetupVaadinInWebXmlRunnable implements Runnable {
      private final WebFacet myWebFacet;
      private final VaadinVersion myVersion;
      @Nullable private final String myApplicationClassName;

      SetupVaadinInWebXmlRunnable(@NotNull WebFacet webFacet, @NotNull VaadinVersion version, @Nullable String applicationClassName) {
        myWebFacet = webFacet;
        myVersion = version;
        myApplicationClassName = applicationClassName;
      }

      @Override
      public void run() {
        final WebApp root = myWebFacet.getRoot();
        ConfigFile descriptor = myWebFacet.getWebXmlDescriptor();
        final PsiFile file = descriptor != null ? descriptor.getXmlFile() : null;
        if (root != null && file != null) {
          final Project project = myWebFacet.getModule().getProject();
          WriteCommandAction.writeCommandAction(project, file).run(() -> {
            Servlet servlet = root.addServlet();
            servlet.getServletName().setValue("VaadinApplicationServlet");
            servlet.getServletClass().setStringValue(myVersion.getServletClass());
            if (myApplicationClassName != null) {
              CommonParamValue param = servlet.addInitParam();
              param.getParamName().setValue(myVersion.getApplicationServletParameterName());
              param.getParamValue().setValue(myApplicationClassName);
            }
            ServletMapping mapping = root.addServletMapping();
            mapping.getServletName().setValue(servlet);
            mapping.addUrlPattern().setValue("/*");
            CodeStyleManager.getInstance(project).reformat(file);
          });
        }
      }
    }
  }
}
