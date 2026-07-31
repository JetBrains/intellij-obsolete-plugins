package com.intellij.gwt.facet;

import com.intellij.CommonBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.compound.CompoundRunConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.packaging.GwtCompileOutputRelativePathSuggester;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.gwt.rpc.GwtServletUtil;
import com.intellij.gwt.run.GwtRunConfiguration;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.gwt.web.GwtWebUtil;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.javaee.JavaeeUtil;
import com.intellij.javaee.appServers.deployment.DeploymentSettings;
import com.intellij.javaee.appServers.run.configuration.CommonStrategy;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.facet.JavaeeFacetType;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.deployment.DeploymentUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.gwt.facet.GwtFacetFrameworkSupportProvider.addServletJarToArtifact;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;
import static com.intellij.openapi.util.text.StringUtil.decapitalize;
import static com.intellij.openapi.util.text.StringUtil.getPackageName;
import static com.intellij.openapi.util.text.StringUtil.getQualifiedName;
import static com.intellij.openapi.util.text.StringUtil.getShortName;
import static com.intellij.openapi.util.text.StringUtil.replace;
import static com.intellij.openapi.util.text.StringUtil.split;
import static com.intellij.openapi.util.text.StringUtil.trimEnd;
import static com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun;

public class GwtSampleApplicationCreator {
  private static final Logger LOG = Logger.getInstance(GwtSampleApplicationCreator.class);
  private static final @NonNls String WAR_STRING = "/war/";
  private static final @NonNls String JAVA_STRING = "/java/";
  private final GwtFacet myFacet;
  private final ModuleRootModel myRootModel;
  private final @NotNull String myAppName;
  private final FrameworkSupportModel myFrameworkSupportModel;
  private final Module myModule;
  private final String myAppPackageName;

  public GwtSampleApplicationCreator(GwtFacet facet, String qualifiedAppName, ModuleRootModel rootModel, FrameworkSupportModel model) {
    myFacet = facet;
    myRootModel = rootModel;
    myAppName = getShortName(qualifiedAppName);
    myFrameworkSupportModel = model;
    myModule = myFacet.getModule();
    myAppPackageName = getQualifiedName(getPackageName(qualifiedAppName), decapitalize(myAppName));
  }

  public @NotNull String getAppName() {
    return myAppName;
  }

  public void create() {
    try {
      WriteAction.run(this::doCreate);
    } catch (IOException exc) {
      LOG.info(exc);
      Messages.showErrorDialog(GwtBundle.message("dialog.message.cannot.create.sample.gwt.application.0", exc.getMessage()), CommonBundle.getErrorTitle());
    }
  }

  private void doCreate() throws IOException {
    VirtualFile contentRoot = null;
    VirtualFile sourceRoot = null;
    final ContentEntry[] contentEntries = myRootModel.getContentEntries();
    for (ContentEntry contentEntry : contentEntries) {
      SourceFolder folder = ContainerUtil.getFirstItem(contentEntry.getSourceFolders(JavaSourceRootType.SOURCE));
      if (folder != null) {
        contentRoot = getOrCreateContentRoot(contentEntry);
        sourceRoot = folder.getFile();
        if (sourceRoot == null) {
          final String path = VfsUtilCore.urlToPath(folder.getUrl());
          sourceRoot = VfsUtil.createDirectoryIfMissing(path);
          if (sourceRoot == null) {
            throw new IOException("Cannot create source root " + path);
          }
        }
      }
    }
    if (sourceRoot == null) {
      if (contentEntries.length == 0) {
        throw new IOException("Module does not have content roots");
      }
      final ContentEntry entry = contentEntries[0];
      contentRoot = getOrCreateContentRoot(entry);
      sourceRoot = findOrCreateChildDirectory(contentRoot, "src");
      entry.addSourceFolder(sourceRoot, false);
    }

    VirtualFile javaDirectory = sourceRoot;
    for (String dirName : split(myAppPackageName, ".")) {
      javaDirectory = findOrCreateChildDirectory(javaDirectory, dirName);
    }

    final VirtualFile finalContentRoot = contentRoot;
    final VirtualFile finalJavaDirectory = javaDirectory;

    Project project = myModule.getProject();
    StartupManager.getInstance(project).runWhenProjectIsInitialized(() ->
                                           runWriteCommandAction(project, GwtBundle.message("command.create.sample.gwt.project.files"), null, () -> {
      final GwtVersion gwtVersion = myFacet.getSdkVersion();
      WebFacet webFacet = myFacet.getWebFacet();
      if (gwtVersion.isHostedModeRequiresWebXml() && webFacet == null) {
        webFacet = GwtWebUtil.createWebFacet(myFacet, finalContentRoot);
      }

      CommonStrategy javaEeConfiguration = null;
      for (RunConfiguration configuration : RunManager.getInstance(project).getAllConfigurationsList()) {
        if (configuration instanceof CommonStrategy) {
          javaEeConfiguration = (CommonStrategy)configuration;
        }
      }

      RunManager runManager = RunManager.getInstance(project);
      GwtRunConfiguration gwtRunConfiguration = ContainerUtil.findInstance(runManager.getAllConfigurationsList(), GwtRunConfiguration.class);
      if (javaEeConfiguration != null) {
        ArtifactManager artifactManager = ArtifactManager.getInstance(project);
        String webFacetName = webFacet == null ? null : webFacet.getName();
        String artifactName = JavaeeUtil.suggestArtifactName(myModule.getName(), webFacetName, WebFacet.ID);

        DeploymentSettings javaEeDeploymentSettings = javaEeConfiguration.getDeploymentSettings();
        if (javaEeDeploymentSettings != null) {
          Artifact warExplodedArtifact = createNewArtifact(project, webFacet, artifactManager, artifactName, true);
          javaEeDeploymentSettings.getOrCreateModel(warExplodedArtifact);
          setBuildArtifactBeforeRun(project, javaEeConfiguration, warExplodedArtifact);
        }
        //we need to call javaEeConfiguration.getUrlToOpenInBrowser() after deployment settings are updated to ensure that context root is added to the URL
        javaEeConfiguration.setUrlToOpenInBrowser(DeploymentUtil.appendToPath(javaEeConfiguration.getUrlToOpenInBrowser(), gwtRunConfiguration.getPage()));

        CommonStrategy javaEeDevMode = javaEeConfiguration.clone();
        javaEeDevMode.getSettingsBean().OPEN_IN_BROWSER = false;
        javaEeDevMode.setName(javaEeConfiguration.getName() + " DevMode");

        runManager.addConfiguration(runManager.createConfiguration(javaEeDevMode, javaEeConfiguration.getFactory()));

        DeploymentSettings javaEeDevModeDeploymentSettings = javaEeDevMode.getDeploymentSettings();
        Artifact warExplodedDevModeArtifact = createNewArtifact(project, webFacet, artifactManager,
                                                                artifactName + " DevMode", false);
        if (javaEeDevModeDeploymentSettings != null) {
          javaEeDevModeDeploymentSettings.getOrCreateModel(warExplodedDevModeArtifact);

          GwtRunConfiguration noServerConfiguration = (GwtRunConfiguration)gwtRunConfiguration.clone();

          noServerConfiguration.setName(gwtRunConfiguration.getName() + " NoServer");
          @NonNls String programParameters = gwtRunConfiguration.getProgramParameters()
                                             + " -noserver"
                                             + " -port " + javaEeDevMode.getPort()
                                             + " -war " + warExplodedDevModeArtifact.getOutputPath();
          noServerConfiguration.setProgramParameters(programParameters);

          runManager.addConfiguration(runManager.createConfiguration(noServerConfiguration, gwtRunConfiguration.getFactory()));
          setBuildArtifactBeforeRun(project, noServerConfiguration, warExplodedDevModeArtifact);
          setBuildArtifactBeforeRun(project, javaEeDevMode, warExplodedDevModeArtifact);

          CompoundRunConfiguration compoundRunConfiguration = new CompoundRunConfiguration(
            noServerConfiguration.getName() + " with " + javaEeDevMode.getName(), project);
          compoundRunConfiguration.setConfigurationsWithoutTargets(Arrays.asList(noServerConfiguration, javaEeDevMode));
          runManager.addConfiguration(runManager.createConfiguration(compoundRunConfiguration, compoundRunConfiguration.getFactory()));
        }
      }
      generateFiles(finalContentRoot, finalJavaDirectory, gwtVersion);
    }));
  }

  private @NotNull Artifact createNewArtifact(Project project, WebFacet webFacet, ArtifactManager artifactManager,
                                              @NlsSafe String artifactName, boolean includeGwtOutput) {
    ArtifactType explodedArtifactType = ((JavaeeFacetType<?, ?>)webFacet.getType()).getExplodedArtifactType();
    Artifact artifact = artifactManager.addArtifact(artifactName, explodedArtifactType, null);

    artifactManager.addElementsToDirectory(artifact, "WEB-INF/classes",
                                           PackagingElementFactory.getInstance().createModuleOutput(myModule));

    String relativePath = GwtCompileOutputRelativePathSuggester.suggestRelativeOutputPath(myFrameworkSupportModel);
    if (includeGwtOutput) {
      artifactManager.addElementsToDirectory(artifact, relativePath, new GwtCompilerOutputElement(project, myFacet));
    }

    addServletJarToArtifact(artifactManager, artifact, myFacet.getConfiguration().getSdk());

    JavaeeArtifactUtil javaeeArtifactUtil = JavaeeArtifactUtil.getInstance();
    artifactManager.addElementsToDirectory(artifact, relativePath, javaeeArtifactUtil.createFacetResourcesElement(webFacet));

    return artifact;
  }

  private static @NotNull VirtualFile getOrCreateContentRoot(ContentEntry contentEntry) throws IOException {
    VirtualFile contentRoot = contentEntry.getFile();
    if (contentRoot == null) {
      final String path = VfsUtilCore.urlToPath(contentEntry.getUrl());
      contentRoot = VfsUtil.createDirectoryIfMissing(path);
      if (contentRoot == null) {
        throw new IOException("Cannot create content root " + path);
      }
    }
    return contentRoot;
  }

  private void generateFiles(@NotNull VirtualFile contentRoot, @NotNull VirtualFile javaDirectory, @NotNull GwtVersion gwtVersion) {
    @NonNls Map<String, String> properties = new HashMap<>();
    properties.put("NAME", myAppName);
    properties.put("PACKAGE_NAME", myAppPackageName);
    properties.put("RELATIVE_SERVLET_PATH", myAppPackageName + "." + myAppName + "/" + myAppName + "Service");
    final String serviceName = myAppName + "Service";
    properties.put("SHORT_SERVLET_PATH", serviceName);
    properties.put(GwtTemplates.GWT_MODULE_DOCTYPE_VAR, gwtVersion.getGwtModuleDocTypeString());

    Project project = myModule.getProject();
    GwtModule gwtModule = null;
    PsiClass servletImpl = null;

    if (!javaDirectory.isValid()) {
      try {
        VirtualFile validJavaDirectory = VfsUtil.createDirectoryIfMissing(javaDirectory.getPath());
        if (validJavaDirectory != null) {
          javaDirectory = validJavaDirectory;
        }
        else {
          LOG.error("Cannot create directory " + javaDirectory.getPath());
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    for (String templatePath : myFacet.getSdkVersion().getGwtSampleAppTemplates()) {
      InputStream stream = getClass().getResourceAsStream(templatePath);
      LOG.assertTrue(stream != null, templatePath + " template not found");

      try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        String text = StreamUtil.readText(reader);
        int i = templatePath.indexOf(JAVA_STRING);
        VirtualFile parent;
        String relativePath;
        if (i != -1) {
          relativePath = templatePath.substring(i + JAVA_STRING.length());
          parent = javaDirectory;
        }
        else {
          i = templatePath.indexOf(WAR_STRING);
          relativePath = templatePath.substring(i + WAR_STRING.length());
          LOG.assertTrue(i != -1, "incorrect template path: " + templatePath);
          final WebFacet webFacet = myFacet.getWebFacet();
          LOG.assertTrue(webFacet != null);
          final List<WebRoot> webRoots = webFacet.getWebRoots();
          if (webRoots.isEmpty()) {
            parent = findOrCreateChildDirectory(contentRoot, "web");
            webFacet.addWebRoot(parent, "/");
          }
          else {
            parent = webRoots.get(0).getFile();
          }
        }

        String[] names = relativePath.split("/");
        for (int j = 0; j < names.length - 1; j++) {
          parent = findOrCreateChildDirectory(parent, names[j]);
        }
        @NonNls String fileName = trimEnd(replace(names[names.length - 1], "App", myAppName), ".ft");

        final VirtualFile file = parent.createChildData(this, fileName);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          text = replace(text, "${" + entry.getKey() + "}", entry.getValue());
        }
        VfsUtil.saveText(file, text);

        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        CodeStyleManager.getInstance(project).reformat(psiFile);

        if (fileName.endsWith(".gwt.xml")) {
          gwtModule = GwtModulesManager.getInstance(project).getGwtModuleByXmlFile(psiFile);
        }
        else if (fileName.endsWith("ServiceImpl.java")) {
          servletImpl = ((PsiJavaFile)psiFile).getClasses()[0];
        }
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    if (myFacet.getSdkVersion().isHostedModeRequiresWebXml()) {
      final String urlPattern = "/" + myAppName + "/" + serviceName;
      final WebApp root = myFacet.getWebFacet().getRoot();
      if (root != null) {
        GwtModule finalGwtModule = gwtModule;
        PsiClass finalServletImpl = servletImpl;
        DumbService.getInstance(project).runWithAlternativeResolveEnabled(() ->
          GwtServletUtil.registerServletForService(finalGwtModule, root, finalServletImpl, serviceName, urlPattern)
        );
      }
    }
  }

  private @NotNull VirtualFile findOrCreateChildDirectory(VirtualFile parent, final String name) throws IOException {
    VirtualFile child = parent.findChild(name);
    if (child != null) {
      return child;
    }
    return parent.createChildDirectory(this, name);
  }
}
