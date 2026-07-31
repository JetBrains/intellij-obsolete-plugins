package com.intellij.gwt.maven;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.ModifiableModelCommitter;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapterLegacyImpl;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.idea.maven.project.SupportedRequestType;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intellij.openapi.util.io.FileUtil.isAbsolute;
import static com.intellij.openapi.util.text.StringUtil.getPackageName;
import static com.intellij.openapi.util.text.StringUtil.getShortName;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.util.text.StringUtil.split;
import static com.intellij.openapi.util.text.StringUtil.splitByLines;
import static org.jetbrains.jps.gwt.index.GwtModuleXmlConstants.GWT_XML_SUFFIX;

public final class GwtLtgtModuleImporter extends MavenImporter {

  private static final Logger LOG = Logger.getInstance(GwtLtgtModuleImporter.class);

  private static final @NonNls String GWT_LIB = "gwt-lib";
  private static final @NonNls String GWT_APP = "gwt-app";
  private static final @NonNls List<String> SUPPORTED_PACKAGINGS = Arrays.asList(GWT_LIB, GWT_APP);

  private static final @NonNls String DEFAULT_MODULE_TEMPLATE = "src/main/module.gwt.xml";
  private static final @NonNls String MAIN_MODULE_META_INF_PATH = "META-INF/gwt/mainModule";

  private static final @NonNls String CONFIG_SKIP_MODULE = "skipModule";
  private static final @NonNls String CONFIG_MODULE_TEMPLATE = "moduleTemplate";
  private static final @NonNls String CONFIG_MODULE_NAME = "moduleName";
  private static final @NonNls String CONFIG_MODULE_SHORT_NAME = "moduleShortName";
  private static final @NonNls String CONFIG_GENERATE_INHERITS_FROM_DEPENDENCIES = "generateInheritsFromDependencies";
  private static final @NonNls String CONFIG_DEPENDENCY_ARTIFACTS = "dependencyArtifacts";

  public GwtLtgtModuleImporter() {
    super("net.ltgt.gwt.maven", "gwt-maven-plugin");
  }

  @Override
  public boolean isApplicable(MavenProject mavenProject) {
    MavenPlugin plugin = mavenProject.findPlugin(myPluginGroupID, myPluginArtifactID);
    return plugin != null && plugin.isExtensions() && SUPPORTED_PACKAGINGS.contains(mavenProject.getPackaging());
  }

  @Override
  public void getSupportedDependencyTypes(Collection<? super String> result, SupportedRequestType type) {
    result.addAll(SUPPORTED_PACKAGINGS);
  }

  @Override
  public void getSupportedPackagings(Collection<? super String> result) {
    result.addAll(SUPPORTED_PACKAGINGS);
  }

  @Override
  public void process(@NotNull IdeModifiableModelsProvider modifiableModelsProvider,
                      @NotNull Module module,
                      @NotNull MavenRootModelAdapter rootModel,
                      @NotNull MavenProjectsTree projectsTree,
                      @NotNull MavenProject mavenProject,
                      @NotNull MavenProjectChanges changes,
                      @NotNull Map<MavenProject, String> mavenProjectToModuleName,
                      @NotNull List<MavenProjectsProcessorTask> postTasks) {
    VirtualFile moduleTemplateFile = getModuleTemplate(mavenProject);
    if (moduleTemplateFile != null) {

      Project project = module.getProject();
      DumbService.getInstance(project).smartInvokeLater(() -> {
        PsiFile moduleTemplatePsiFile = PsiManager.getInstance(project).findFile(moduleTemplateFile);
        if (moduleTemplatePsiFile instanceof XmlFile) {
          MavenRootModelAdapter rootModelAdapter = new MavenRootModelAdapter(
            new MavenRootModelAdapterLegacyImpl(mavenProject, module, new IdeModifiableModelsProviderImpl(module.getProject())));
          generateModuleFile0(module, project, rootModelAdapter, mavenProject, (XmlFile)moduleTemplatePsiFile);
          WriteCommandAction.runWriteCommandAction(project, () -> {
            ModifiableModelCommitter.multiCommit(new ModifiableRootModel[]{rootModelAdapter.getRootModel()},
                                                 ModuleManager.getInstance(project).getModifiableModel());
          });
        }
      });
    }
  }

  public boolean isModuleTemplate(@NotNull MavenProject mavenProject, @NotNull VirtualFile virtualFile) {
    return virtualFile.equals(getModuleTemplate(mavenProject));
  }

  private @Nullable VirtualFile getModuleTemplate(@NotNull MavenProject mavenProject) {
    String moduleTemplatePath = findConfigValue(mavenProject, CONFIG_MODULE_TEMPLATE);
    if (isEmpty(moduleTemplatePath)) {
      moduleTemplatePath = DEFAULT_MODULE_TEMPLATE;
    }
    if (isAbsolute(moduleTemplatePath)) {
      return VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(moduleTemplatePath));
    }
    else {
      return mavenProject.getDirectoryFile().findFileByRelativePath(moduleTemplatePath);
    }
  }

  private void generateModuleFile0(@NotNull Module module, @NotNull Project project, @NotNull MavenRootModelAdapter rootModel,
                                   @NotNull MavenProject mavenProject, @NotNull XmlFile moduleTemplateXmlFile) {
    if (skipModule(mavenProject)) return;

    String moduleName = getModuleName(mavenProject);
    if (isEmpty(moduleName)) return;

    String moduleDirRelativePath = getPackageName(moduleName).replace('.', '/');

    List<String> mavenProjectSources = mavenProject.getSources();
    if (mavenProjectSources.isEmpty()) return;

    @NonNls String outputDirPath = mavenProject.getGeneratedSourcesDirectory(false) + "/gwt-maven-plugin/";

    VirtualFile moduleDir;
    try {
      moduleDir = VfsUtil.createDirectories(outputDirPath + moduleDirRelativePath); // just in case
    }
    catch (IOException e) {
      LOG.error("Failed to create directory for imported GWT module file", e);
      return;
    }

    PsiDirectory modulePsiDir = PsiManager.getInstance(project).findDirectory(moduleDir);
    if (modulePsiDir == null) return;

    WriteCommandAction.runWriteCommandAction(project, () -> {
      rootModel.addGeneratedJavaSourceFolder(outputDirPath, JavaSourceRootType.SOURCE);

      String moduleFileName = getShortName(moduleName) + GWT_XML_SUFFIX;

      PsiFile existingModulePsiFile = modulePsiDir.findFile(moduleFileName);
      if (existingModulePsiFile != null) {
        if (!(existingModulePsiFile instanceof XmlFile moduleXmlFile)) {
          existingModulePsiFile.delete();
          existingModulePsiFile = null;
        }
        else {
          XmlTag rootTag = moduleXmlFile.getRootTag();
          if (isNullOrEmpty(moduleTemplateXmlFile.getRootTag())) {
            if (rootTag != null) {
              rootTag.delete();
            }
          }
          else {
            PsiElement rootTagCopy = moduleTemplateXmlFile.getRootTag().copy();
            if (isNullOrEmpty(rootTag)) {
              moduleXmlFile.add(rootTagCopy);
            }
            else {
              rootTag.replace(rootTagCopy);
            }
          }
        }
      }

      XmlFile moduleXmlFile = (XmlFile)(existingModulePsiFile != null
          ? existingModulePsiFile
          : modulePsiDir.copyFileFrom(moduleFileName, moduleTemplateXmlFile));

      GwtModule gwtModule = GwtModulesManager.getInstance(project).getGwtModuleByXmlFile(moduleXmlFile);
      if (gwtModule == null) {
        XmlTag rootTag = moduleXmlFile.getRootTag();
        if (isNullOrEmpty(rootTag)) {
          XmlDocument document = moduleXmlFile.getDocument();
          if (document != null) {
            document.add(XmlElementFactory.getInstance(project).createTagFromText("<module/>"));
          }
          else {
            LOG.error("Failed to get document from XML file " + moduleXmlFile);
            return;
          }
        }
        else {
          rootTag.setName("module");
        }
        gwtModule = GwtModulesManager.getInstance(project).getGwtModuleByXmlFile(moduleXmlFile);
        LOG.assertTrue(gwtModule != null);
      }

      String moduleShortName = getModuleShortName(mavenProject);
      if (isNotEmpty(moduleShortName)) {
        gwtModule.getRenameTo().setStringValue(moduleShortName);
      }

      generateInheritsFromDependencies(module, project, mavenProject, gwtModule);

      if (gwtModule.getInheritses().isEmpty()) {
        gwtModule.addInherits().getName().setStringValue("com.google.gwt.core.Core");
      }

      if (gwtModule.getSources().isEmpty() && gwtModule.getSuperSources().isEmpty()) {
        gwtModule.addSource().getPath().setStringValue("client");
        gwtModule.addSource().getPath().setStringValue("shared");
        gwtModule.addSuperSource().getPath().setStringValue("super");
      }
    });
  }

  private static boolean isNullOrEmpty(XmlTag rootTag) {
    return rootTag == null || rootTag.getTextLength() == 0;
  }

  private void generateInheritsFromDependencies(@NotNull Module module, @NotNull Project project,
                                                @NotNull MavenProject mavenProject, @NotNull GwtModule gwtModule) {
    if (generateInheritsFromDependencies(mavenProject)) {
      Set<MavenArtifact> dependencyArtifacts = new HashSet<>();

      Element dependencyArtifactsElement = getConfig(mavenProject, CONFIG_DEPENDENCY_ARTIFACTS);
      if (dependencyArtifactsElement != null) {
        // not yet supported in plugin
      }

      for (MavenArtifactNode artifactNode : mavenProject.getDependencyTree()) {
        dependencyArtifacts.add(artifactNode.getArtifact());
      }
      Set<String> libraries = new HashSet<>();
      MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
      for (MavenArtifact artifact : dependencyArtifacts) {
        String scope = artifact.getScope();
        if (isEmpty(scope) || "compile".equals(scope) || "runtime".equals(scope)) {
          MavenProject dependency = mavenProjectsManager.findProject(artifact);
          if (dependency != null && isApplicable(dependency)) {
            String otherModuleName = getModuleName(dependency);
            if (isNotEmpty(otherModuleName)) {
              gwtModule.addInherits().getName().setStringValue(otherModuleName);
            }
          }
        }
        libraries.add(artifact.getLibraryName());
      }

      OrderEnumerator.orderEntries(module).compileOnly().runtimeOnly().forEachLibrary(library -> {
        if (libraries.contains(library.getName())) {
          for (VirtualFile jarRootDir : library.getFiles(OrderRootType.CLASSES)) {
            VirtualFile mainModuleMetaInfFile = jarRootDir.findFileByRelativePath(MAIN_MODULE_META_INF_PATH);
            if (mainModuleMetaInfFile != null) {
              String mainModuleName = readMainModuleName(mainModuleMetaInfFile);
              if (isNotEmpty(mainModuleName)) {
                gwtModule.addInherits().getName().setStringValue(mainModuleName);
              }
            }
          }
        }
        return true;
      });
    }
  }


  private boolean skipModule(MavenProject mavenProject) {
    return "true".equals(findConfigValue(mavenProject, CONFIG_SKIP_MODULE));
  }

  private String getModuleName(MavenProject mavenProject) {
    return findConfigValue(mavenProject, CONFIG_MODULE_NAME);
  }

  private String getModuleShortName(MavenProject mavenProject) {
    return findConfigValue(mavenProject, CONFIG_MODULE_SHORT_NAME);
  }

  private boolean generateInheritsFromDependencies(MavenProject mavenProject) {
    String generateInheritsStr = findConfigValue(mavenProject, CONFIG_GENERATE_INHERITS_FROM_DEPENDENCIES); // default true
    return generateInheritsStr == null || "true".equals(generateInheritsStr);
  }


  private static String readMainModuleName(VirtualFile mainModuleMetaInfFile) {
    try (Reader reader = new InputStreamReader(mainModuleMetaInfFile.getInputStream(), StandardCharsets.UTF_8)) {
      for (String line : splitByLines(StreamUtil.readText(reader))) {
        String mainModuleName = split(line, "#").get(0).trim(); // remove comments
        if (isNotEmpty(mainModuleName)) return mainModuleName; // ignore remaining lines
      }
    }
    catch (IOException e) {
      LOG.error("Error while reading library jar file", e);
    }
    return null;
  }
}
