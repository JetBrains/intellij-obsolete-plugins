// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import com.intellij.util.PathsList;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.ThreadingAssertions;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsFacetProvider;
import org.jetbrains.plugins.grails.util.GrailsNameUtils;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureUtil;
import org.jetbrains.plugins.groovy.mvc.MvcPathMacros;
import org.jetbrains.plugins.groovy.mvc.MvcProjectStructure;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GrailsFramework {

  private static final Logger LOG = Logger.getInstance(GrailsFramework.class);

  public static final Key<Boolean> UPDATE_IN_PROGRESS = Key.create("grails.updateInProgress");
  public static final MultiMap<JpsModuleSourceRootType<?>, String> GRAILS_SOURCE_FOLDERS =
    new MultiMap<>();

  static {
    GRAILS_SOURCE_FOLDERS.put(JavaSourceRootType.SOURCE, Arrays.asList("src/java", "src/gwt", "src/scala", "grails-app/utils",
                                                                       "src/groovy", "grails-app/jobs", "grails-app/i18n",
                                                                       "grails-app/realms", "grails-app/controllers", "grails-app/domain",
                                                                       "grails-app/services", "grails-app/taglib"));

    GRAILS_SOURCE_FOLDERS.put(JavaSourceRootType.TEST_SOURCE, Arrays.asList("test/unit", "test/integration", "test/functional"));

    GRAILS_SOURCE_FOLDERS.put(JavaResourceRootType.RESOURCE, Collections.singleton("grails-app/resources"));
  }

  private static final String PLUGINS_MODULE_SUFFIX = "-grailsPlugins";
  private static final String GLOBAL_PLUGINS_MODULE_NAME = "GrailsGlobalPlugins";

  private static final GrailsFramework INSTANCE = new GrailsFramework();

  public static @NotNull GrailsFramework getInstance() {
    return INSTANCE;
  }

  public boolean hasSupport(@NotNull Module module) {
    return !isAuxModule(module) && GrailsApplicationManager.findApplication(module) != null;
  }

  public @NotNull String getApplicationDirectoryName() {
    return GrailsUtils.GRAILS_APP_DIRECTORY;
  }

  public void syncSdkAndLibrariesInPluginsModule(@NotNull Module module) {
    final Module pluginsModule = findCommonPluginsModule(module);
    if (pluginsModule != null) {
      MvcModuleStructureUtil.syncAuxModuleSdk(module, pluginsModule, this);
    }
    for (Module auxModule : GrailsModuleStructureUtil.getAllCustomPluginModules(module)) {
      MvcModuleStructureUtil.syncAuxModuleSdk(module, auxModule, this);
    }
  }

  public void upgradeFramework(@NotNull Module module) {
    if (findAppRoot(module) != null && !GrailsModuleStructureUtil.isIdeaGeneratedCustomPluginModule(module)) {
      GrailsModuleStructureUtil.upgradeGrails(module, false);
    }
  }

  public @Nullable File getCommonPluginsDir(@NotNull Module module) {
    return toAbsoluteFile(module, GrailsSettingsService.getProjectPluginsDir(module));
  }

  public static void forceSynchronizationSetting(final @NotNull Module module) {
    new GrailsSettingSynchronizer(module) {
      @Override
      protected void onDone(boolean isSettingChanged, boolean isExtractingSettingsRan) {
        super.onDone(isSettingChanged, isExtractingSettingsRan);
        getInstance().updateProjectStructure(module);
      }
    }.syncGrailsSettings(true);
  }

  private void syncModules(final Project project) {
    ArrayList<Module> grailsModules = new ArrayList<>();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (!hasSupport(module)) continue;

      grailsModules.add(module);

      boolean f = new GrailsSettingSynchronizer(module) {
        @Override
        protected void onDone(boolean isSettingChanged, boolean isExtractingSettingsRan) {
          super.onDone(isSettingChanged, isExtractingSettingsRan);

          boolean stillGrailsModule = getInstance().hasSupport(myModule);
          assert isExtractingSettingsRan || stillGrailsModule;

          if (isSettingChanged && stillGrailsModule) {
            if (MvcModuleStructureUtil.isEnabledStructureUpdate()) {
              WriteAction.run(() -> {
                VirtualFile root = findAppRoot(myModule);
                assert root != null;
                project.putUserData(UPDATE_IN_PROGRESS, true);
                MvcModuleStructureUtil.updateModuleStructure(myModule, createProjectStructure(myModule, false), root);
                project.putUserData(UPDATE_IN_PROGRESS, null);
              });
            }
          }

          if (isExtractingSettingsRan) {
            syncModules(project);
          }
        }
      }.syncGrailsSettings(false);

      if (!f) return;
    }

    if (MvcModuleStructureUtil.isEnabledStructureUpdate()) {
      project.putUserData(UPDATE_IN_PROGRESS, true);
      configureAuxModules(grailsModules);
      project.putUserData(UPDATE_IN_PROGRESS, null);
    }
  }

  private void configureAuxModules(final ArrayList<Module> grailsModules) {
    if (grailsModules.isEmpty()) return;
    final Project project = grailsModules.get(0).getProject();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

    WriteAction.run(() -> {
      // Update in-place plugin modules.
      MultiMap<Module, Module> backInplacePluginDependencies = new MultiMap<>();
      MultiMap<Module, Module> inplacePluginDependencies = new MultiMap<>();

      for (Module module : grailsModules.toArray(Module.EMPTY_ARRAY)) {
        final Map<String, VirtualFile> locations = getCustomPluginLocations(module, true);

        for (Map.Entry<String, VirtualFile> entry : locations.entrySet()) {
          VirtualFile pluginRoot = entry.getValue();

          Module pluginModule = fileIndex.getModuleForFile(pluginRoot);

          if (pluginModule != null && Comparing.equal(fileIndex.getContentRootForFile(pluginRoot), pluginRoot)) {
            if (pluginModule == module || isAuxModule(pluginModule)) continue;
          }
          else {
            String pluginModuleName =
              GrailsModuleStructureUtil.generateInplacePluginModuleName(GrailsNameUtils.toPluginName(entry.getKey()));
            pluginModule = MvcModuleStructureUtil.createAuxiliaryModule(module, pluginModuleName, this);
            MvcModuleStructureUtil.updateModuleStructure(pluginModule,
                                                         createProjectStructure(pluginModule, false),
                                                         pluginRoot);
            grailsModules.add(pluginModule);
          }

          inplacePluginDependencies.putValue(module, pluginModule);
          backInplacePluginDependencies.putValue(pluginModule, module);
        }
      }

      List<Module> correctCommonPluginModules = new ArrayList<>();

      for (Module module : grailsModules) {
        assert !isAuxModule(module);
        assert !module.isDisposed();

        // Update common plugin modules.
        if (backInplacePluginDependencies.get(module).isEmpty()) {
          Module commonPluginModule = MvcModuleStructureUtil.updateAuxiliaryPluginsModuleRoots(module, this);
          if (commonPluginModule != null) {
            Set<Module> dependenciesTransitive = GrailsModuleStructureUtil.getAllCustomPluginModules(module);

            for (Module m : grailsModules) {
              if (m == module) continue;

              if (dependenciesTransitive.contains(m)) {
                MvcModuleStructureUtil.ensureDependency(m, commonPluginModule, false);
              }
              else {
                MvcModuleStructureUtil.removeDependency(m, commonPluginModule);
              }
            }

            correctCommonPluginModules.add(commonPluginModule);

            // remove from grails module source folders belongs to common plugin module if any
            final VirtualFile[] pluginSourcesRoots = ModuleRootManager.getInstance(commonPluginModule).getSourceRoots();
            ModuleRootModificationUtil.updateModel(module, modifiableModel -> {
              final List<Consumer<ContentEntry>> actions = new ArrayList<>();
              final List<VirtualFile> sourceRoots = new SmartList<>(modifiableModel.getSourceRoots());

              for (VirtualFile pluginSourceRoot : pluginSourcesRoots) {
                MvcModuleStructureUtil.removeSrcFolderFromRoots(pluginSourceRoot, actions, sourceRoots);
              }

              for (ContentEntry contentEntry : modifiableModel.getContentEntries()) {
                for (final Consumer<ContentEntry> action : actions) {
                  action.consume(contentEntry);
                }
              }
            });
          }
        }

        // Update dependencies between grails modules.
        for (Module m : grailsModules) {
          if (m == module) continue;

          if (inplacePluginDependencies.get(module).contains(m)) {
            MvcModuleStructureUtil.ensureDependency(module, m, true);
          }
          else {
            if (GrailsUtils.isGrailsPluginModule(m)) {
              MvcModuleStructureUtil.removeDependency(module, m);
            }
          }
        }
      }

      MvcModuleStructureUtil.updateGlobalPluginModule(project, getInstance());

      for (Module module : ModuleManager.getInstance(project).getModules()) {
        if (isCommonPluginsModule(module) && !correctCommonPluginModules.contains(module)) {
          MvcModuleStructureUtil.removeAuxiliaryModule(module);
        }
      }
    });
  }

  public void updateProjectStructure(final @NotNull Module someModule) {
    Project project = someModule.getProject();
    if (GrailsSettingSynchronizer.isUpdateSettingRunning(project)) return;

    if (MvcModuleStructureUtil.isEnabledStructureUpdate()) {
      Module[] modules = ModuleManager.getInstance(project).getModules();

      WriteAction.run(() -> {
        for (Module module : modules) {
          if (!hasSupport(module)) continue;

          final VirtualFile root = findAppRoot(module);
          if (root != null) {
            project.putUserData(UPDATE_IN_PROGRESS, true);
            MvcModuleStructureUtil.updateModuleStructure(module, createProjectStructure(module, false), root);
            project.putUserData(UPDATE_IN_PROGRESS, null);
          }
        }
      });
    }

    syncModules(project);
  }

  public VirtualFile getSdkRoot(@Nullable Module module) {
    return GrailsConfigUtils.getSDKInstallPath(module);
  }

  public String getUserLibraryName() {
    return GrailsUtils.GRAILS_USER_LIBRARY;
  }

  private List<File> getImplicitClasspathRootsInner(Module module) {
    final List<File> toExclude = new ArrayList<>();

    VirtualFile sdkRoot = getSdkRoot(module);
    if (sdkRoot != null) toExclude.add(VfsUtilCore.virtualToIoFile(sdkRoot));

    ContainerUtil.addIfNotNull(toExclude, getCommonPluginsDir(module));
    final VirtualFile appRoot = findAppRoot(module);
    if (appRoot != null) {
      VirtualFile pluginDir = appRoot.findChild(MvcModuleStructureUtil.PLUGINS_DIRECTORY);
      if (pluginDir != null) toExclude.add(VfsUtilCore.virtualToIoFile(pluginDir));


      VirtualFile libDir = appRoot.findChild("lib");
      if (libDir != null) toExclude.add(VfsUtilCore.virtualToIoFile(libDir));
    }

    final Library library = MvcModuleStructureUtil.findUserLibrary(module, getUserLibraryName());
    if (library != null) {
      for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
        toExclude.add(VfsUtilCore.virtualToIoFile(VfsUtil.getLocalFile(file)));
      }
    }
    return toExclude;
  }

  private List<File> getImplicitClasspathRoots(@NotNull Module module) {
    final List<File> toExclude = getImplicitClasspathRootsInner(module);

    ContainerUtil.addIfNotNull(toExclude, getInstance().getSdkWorkDir(module));

    for (Module customPluginModule : GrailsModuleStructureUtil.getAllCustomPluginModules(module)) {
      final CompilerModuleExtension extension =
        ModuleRootManager.getInstance(customPluginModule).getModuleExtension(CompilerModuleExtension.class);

      VirtualFile compilerOutputPath = extension.getCompilerOutputPath();
      if (compilerOutputPath != null) toExclude.add(VfsUtilCore.virtualToIoFile(compilerOutputPath));

      VirtualFile compilerOutputTestPath = extension.getCompilerOutputPath();
      if (compilerOutputTestPath != null) toExclude.add(VfsUtilCore.virtualToIoFile(compilerOutputTestPath));

      final VirtualFile appRoot = findAppRoot(customPluginModule);
      if (appRoot != null) {
        VirtualFile libDir = appRoot.findChild("lib");
        if (libDir != null) toExclude.add(VfsUtilCore.virtualToIoFile(libDir));
      }
    }
    return toExclude;
  }

  private static String getCommonPluginSuffix() {
    return PLUGINS_MODULE_SUFFIX;
  }

  public String getGlobalPluginsModuleName() {
    return GLOBAL_PLUGINS_MODULE_NAME;
  }

  public boolean isSDKLibrary(Library library) {
    return GrailsConfigUtils.getInstance().isSDKLibrary(library);
  }

  public MvcProjectStructure createProjectStructure(@NotNull Module module, boolean auxModule) {
    return new GrailsProjectStructure(module, auxModule);
  }

  static class GrailsProjectStructure extends MvcProjectStructure {

    GrailsProjectStructure(Module module, boolean auxModule) {
      super(module, auxModule, getUserHomeGrails(), getInstance().getSdkWorkDir(module));
    }

    @Override
    public @NotNull String getUserLibraryName() {
      return GrailsUtils.GRAILS_USER_LIBRARY;
    }

    @Override
    public MultiMap<JpsModuleSourceRootType<?>, String> getSourceFolders() {
      return GRAILS_SOURCE_FOLDERS;
    }

    @Override
    public String[] getInvalidSourceFolders() {
      return new String[]{"src", "."};
    }

    @Override
    public String[] getExcludedFolders() {
      return new String[]{"web-app/plugins", "target/classes", "target/test-classes"};
    }

    @Override
    public List<VirtualFile> getExcludedFolders(@NotNull VirtualFile root) {
      List<VirtualFile> res = super.getExcludedFolders(root);

      VirtualFile workDir = VfsUtil.findRelativeFile(root, "target", "work");
      if (workDir != null) {
        res = new ArrayList<>(res);

        for (VirtualFile dir : workDir.getChildren()) {
          if (dir.isDirectory() && !Comparing.equal("plugins", dir.getNameSequence())) {
            res.add(dir);
          }
        }
      }

      return res;
    }

    @Override
    public void setupFacets(Collection<Consumer<ModifiableFacetModel>> actions, Collection<VirtualFile> roots) {
      for (GrailsFacetProvider provider : GrailsFacetProvider.EP_NAME.getExtensions()) {
        provider.addFacets(actions, myModule, roots);
      }
    }
  }

  public static Map<String, VirtualFile> getCustomPluginLocations(@NotNull Module module, boolean refresh) {
    Map<String, String> customPluginLocations = GrailsSettingsService.getGrailsSettings(module).customPluginLocations;
    if (customPluginLocations.isEmpty()) {
      return Collections.emptyMap();
    }

    VirtualFile root = GrailsUtils.findGrailsAppRoot(module);
    assert root != null;

    final Map<String, VirtualFile> res = new HashMap<>();

    for (Map.Entry<String, String> entry : customPluginLocations.entrySet()) {
      VirtualFile pluginRoot;

      if (FileUtil.isAbsolute(entry.getValue())) {
        if (refresh) {
          ThreadingAssertions.assertEventDispatchThread();
          pluginRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(entry.getValue());
        }
        else {
          pluginRoot = LocalFileSystem.getInstance().findFileByPath(entry.getValue());
        }
      }
      else {
        pluginRoot = VfsUtilCore.findRelativeFile(entry.getValue(), root);
      }

      if (pluginRoot != null) {
        res.put(GrailsNameUtils.toPluginName(entry.getKey()), pluginRoot);
      }
    }

    return res;
  }

  private static boolean equalsIgnoreFirstCase(@NotNull String s1, @Nullable String s2) {
    if (s2 == null) return false;
    int length = s1.length();
    if (length != s2.length()) return false;

    if (length <= 1) return s1.equalsIgnoreCase(s2);

    return Character.toUpperCase(s1.charAt(0)) == Character.toUpperCase(s2.charAt(0)) && s1.substring(1).equals(s2.substring(1));
  }

  public @Nullable VirtualFile findPluginRoot(@NotNull Module module, String pluginName, boolean canHasVersion) {

    String withoutVersion = GrailsNameUtils.toPluginName(pluginName);
    String withVersion = null;
    if (canHasVersion) {
      int idx = pluginName.lastIndexOf('-');

      if (idx != -1) {
        withVersion = GrailsNameUtils.toPluginName(pluginName.substring(0, idx));
      }
    }

    for (VirtualFile root : getAllPluginRoots(module, false)) {
      String name = GrailsUtils.extractGrailsPluginName(root);
      if (name != null && (equalsIgnoreFirstCase(name, withoutVersion) || equalsIgnoreFirstCase(name, withVersion))) {
        return root;
      }
    }

    return null;
  }

  public Collection<VirtualFile> getAllPluginRoots(@NotNull Module module, boolean refresh) {
    Map<String, VirtualFile> map = new HashMap<>();
    collectCommonPluginRoots(map, module, refresh);

    ArrayList<VirtualFile> res = new ArrayList<>();
    res.addAll(map.values());
    res.addAll(getCustomPluginLocations(module, refresh).values());
    return res;
  }

  public File getGlobalPluginsDir(@NotNull Module module) {
    return toAbsoluteFile(module, GrailsSettingsService.getGlobalPluginsDir(module));
  }

  public @Nullable File getSdkWorkDir(@NotNull Module module) {
    return toAbsoluteFile(module, GrailsSettingsService.getGrailsWorkDir(module));
  }

  private @Nullable File toAbsoluteFile(@NotNull Module module, @Nullable String path) {
    if (path == null) return null;
    File res = new File(path);
    if (res.isAbsolute()) return res;

    VirtualFile root = findAppRoot(module);
    if (root == null) return null;

    return new File(VfsUtilCore.virtualToIoFile(root), path);
  }

  public static String getUserHomeGrails() {
    return MvcPathMacros.getSdkWorkDirParent("grails");
  }

  public @Nullable VirtualFile findAppDirectory(@Nullable Module module) {
    if (module == null || isCommonPluginsModule(module)) return null;

    OldGrailsApplication oldApplication = GrailsApplicationManager.findApplication(module);
    if (oldApplication != null) return oldApplication.getAppRoot();

    if (ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) {
      VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
      VirtualFile[] ancestors = VfsUtil.getCommonAncestors(contentRoots);
      if (ancestors.length == 1) {
        return ancestors[0].findChild(getApplicationDirectoryName());
      }
    }

    String appDirName = getApplicationDirectoryName();

    for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
      VirtualFile res = root.findChild(appDirName);
      if (res != null) return res;
    }

    return null;
  }

  //////////////////////////
  // MvcFramework inlined //
  //////////////////////////
  public static final @NonNls String GROOVY_STARTER_CONF = "/conf/groovy-starter.conf";

  public boolean isAuxModule(@NotNull Module module) {
    return isCommonPluginsModule(module) || isGlobalPluginModule(module);
  }

  public static boolean isCommonPluginsModule(@NotNull Module module) {
    return module.getName().endsWith(getCommonPluginSuffix());
  }

  public @Nullable VirtualFile findAppRoot(@Nullable Module module) {
    if (module == null || module.isDisposed()) return null;

    String appDirName = getApplicationDirectoryName();

    for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
      if (root.isInLocalFileSystem() && root.findChild(appDirName) != null) return root;
    }

    return null;
  }

  public @Nullable VirtualFile findAppRoot(@Nullable PsiElement element) {
    VirtualFile appDirectory = findAppDirectory(element);
    return appDirectory == null ? null : appDirectory.getParent();
  }

  public @Nullable VirtualFile findAppDirectory(@Nullable PsiElement element) {
    if (element == null) return null;

    PsiFile containingFile = element.getContainingFile().getOriginalFile();
    VirtualFile file = containingFile.getVirtualFile();
    if (file == null) return null;

    ProjectFileIndex index = ProjectRootManager.getInstance(containingFile.getProject()).getFileIndex();

    VirtualFile root = index.getContentRootForFile(file);
    if (root == null) return null;

    return root.findChild(getApplicationDirectoryName());
  }

  private PathsList removeFrameworkStuff(Module module, List<VirtualFile> rootFiles) {
    final List<File> toExclude = getImplicitClasspathRoots(module);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Before removing framework stuff: " + rootFiles);
      LOG.debug("Implicit roots:" + toExclude);
    }

    PathsList scriptClassPath = new PathsList();
    eachRoot:
    for (VirtualFile file : rootFiles) {
      for (final File excluded : toExclude) {
        if (VfsUtilCore.isAncestor(excluded, VfsUtilCore.virtualToIoFile(file), false)) {
          continue eachRoot;
        }
      }
      scriptClassPath.add(file);
    }
    return scriptClassPath;
  }

  public PathsList getApplicationClassPath(Module module) {
    final List<VirtualFile> classPath = new ArrayList<>(
      OrderEnumerator.orderEntries(module).recursively().withoutSdk().getPathsList().getVirtualFiles()
    );

    retainOnlyJarsAndDirectories(classPath);

    removeModuleOutput(module, classPath);

    final Module pluginsModule = findCommonPluginsModule(module);
    if (pluginsModule != null) {
      removeModuleOutput(pluginsModule, classPath);
    }

    return removeFrameworkStuff(module, classPath);
  }

  private static void retainOnlyJarsAndDirectories(List<VirtualFile> woSdk) {
    for (Iterator<VirtualFile> iterator = woSdk.iterator(); iterator.hasNext();) {
      VirtualFile file = iterator.next();
      final VirtualFile local = JarFileSystem.getInstance().getVirtualFileForJar(file);
      final boolean dir = file.isDirectory();
      final String name = file.getName();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Considering: " + file.getPath() + "; local=" + local + "; dir=" + dir + "; name=" + name);
      }
      if (dir || local != null) {
        continue;
      }
      if (name.endsWith(".jar")) {
        continue;
      }
      LOG.debug("Removing");
      iterator.remove();
    }
  }

  private static void removeModuleOutput(Module module, List<VirtualFile> from) {
    final CompilerModuleExtension extension = ModuleRootManager.getInstance(module).getModuleExtension(CompilerModuleExtension.class);
    from.remove(extension.getCompilerOutputPath());
    from.remove(extension.getCompilerOutputPathForTests());
  }

  public static void addJavaHome(Sdk sdk, @NotNull JavaParameters params) {
    String homePath = sdk.getHomePath();
    if (homePath != null) {
      String path = StringUtil.trimEnd(homePath, File.separator);
      if (StringUtil.isNotEmpty(path)) {
        params.addEnv("JAVA_HOME", FileUtil.toSystemDependentName(path));
      }
    }
  }

  private void extractPlugins(Project project, @Nullable VirtualFile pluginRoot, boolean refreshPluginRoot, Map<String, VirtualFile> res) {
    if (pluginRoot != null) {
      if (refreshPluginRoot) {
        pluginRoot.refresh(false, false);
      }

      VirtualFile[] children = pluginRoot.getChildren();
      if (children != null) {
        for (VirtualFile child : children) {
          String pluginName = getInstalledPluginNameByPath(project, child);
          if (pluginName != null) {
            res.put(pluginName, child);
          }
        }
      }
    }
  }

  public void collectCommonPluginRoots(Map<String, VirtualFile> result, @NotNull Module module, boolean refresh) {
    if (isCommonPluginsModule(module)) {
      for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
        String pluginName = getInstalledPluginNameByPath(module.getProject(), root);
        if (pluginName != null) {
          result.put(pluginName, root);
        }
      }
    }
    else {
      VirtualFile root = findAppRoot(module);
      if (root == null) return;

      extractPlugins(module.getProject(), root.findChild(MvcModuleStructureUtil.PLUGINS_DIRECTORY), refresh, result);
      extractPlugins(module.getProject(), MvcModuleStructureUtil.findFile(getCommonPluginsDir(module), refresh), refresh, result);
      extractPlugins(module.getProject(), MvcModuleStructureUtil.findFile(getGlobalPluginsDir(module), refresh), refresh, result);
    }
  }

  public Collection<VirtualFile> getCommonPluginRoots(@NotNull Module module, boolean refresh) {
    Map<String, VirtualFile> result = new HashMap<>();
    collectCommonPluginRoots(result, module, refresh);
    return result.values();
  }

  public static @Nullable Module findCommonPluginsModule(@NotNull Module module) {
    return ModuleManager.getInstance(module.getProject()).findModuleByName(getCommonPluginsModuleName(module));
  }

  public boolean isGlobalPluginModule(@NotNull Module module) {
    return module.getName().startsWith(getGlobalPluginsModuleName());
  }

  public static String getCommonPluginsModuleName(Module module) {
    return module.getName() + getCommonPluginSuffix();
  }

  public static void addAvailableSystemScripts(final Collection<? super String> result, @NotNull Module module) {
    VirtualFile scriptRoot = null;

    GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);

    for (PsiClass aClass : JavaPsiFacade.getInstance(module.getProject()).findClasses("CreateApp_", searchScope)) {
      PsiClass superClass = aClass.getSuperClass();
      if (superClass != null && GroovyCommonClassNames.GROOVY_LANG_SCRIPT.equals(superClass.getQualifiedName())) {
        PsiFile psiFile = aClass.getContainingFile();
        if (psiFile != null) {
          VirtualFile file = psiFile.getVirtualFile();
          if (file != null && file.getFileSystem() instanceof JarFileSystem) {
            VirtualFile parent = file.getParent();
            if (parent != null && parent.findChild("Console.class") != null) {
              scriptRoot = parent;
              break;
            }
          }
        }
      }
    }

    if (scriptRoot == null) return;

    Pattern scriptPattern = Pattern.compile("([A-Za-z0-9]+)_?\\.class");

    for (VirtualFile file : scriptRoot.getChildren()) {
      Matcher matcher = scriptPattern.matcher(file.getName());
      if (matcher.matches()) {
        result.add(GroovyNamesUtil.camelToSnake(matcher.group(1)));
      }
    }

  }

  public static void addAvailableScripts(final Collection<? super String> result, final @Nullable VirtualFile root) {
    if (root == null || !root.isDirectory()) {
      return;
    }

    final VirtualFile scripts = root.findChild("scripts");

    if (scripts == null || !scripts.isDirectory()) {
      return;
    }

    for (VirtualFile child : scripts.getChildren()) {
      if (isScriptFile(child)) {
        result.add(GroovyNamesUtil.camelToSnake(child.getNameWithoutExtension()));
      }
    }
  }

  public static boolean isScriptFileName(String fileName) {
    return fileName.endsWith(GroovyFileType.DEFAULT_EXTENSION) && fileName.charAt(0) != '_';
  }

  private static boolean isScriptFile(VirtualFile virtualFile) {
    return !virtualFile.isDirectory() && isScriptFileName(virtualFile.getName());
  }

  public @Nullable String getInstalledPluginNameByPath(Project project, @NotNull VirtualFile pluginPath) {
    VirtualFile pluginXml = pluginPath.findChild("plugin.xml");
    if (pluginXml == null) return null;

    PsiFile pluginXmlPsi = PsiManager.getInstance(project).findFile(pluginXml);
    if (!(pluginXmlPsi instanceof XmlFile)) return null;

    XmlTag rootTag = ((XmlFile)pluginXmlPsi).getRootTag();
    if (rootTag == null || !"plugin".equals(rootTag.getName())) return null;

    XmlAttribute attrName = rootTag.getAttribute("name");
    if (attrName == null) return null;

    String res = attrName.getValue();
    if (res == null) return null;

    res = res.trim();
    if (res.isEmpty()) return null;

    return res;
  }

  @Contract("null -> null")
  public static @Nullable GrailsFramework getInstance(final @Nullable Module module) {
    if (module == null) {
      return null;
    }

    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, () -> {
      final ModificationTracker tracker = MvcModuleStructureSynchronizer.getInstance(project).getFileAndRootsModificationTracker();
      GrailsFramework framework = getInstance();
      if (framework.hasSupport(module)) {
        return CachedValueProvider.Result.create(framework, tracker);
      }
      return CachedValueProvider.Result.create(null, tracker);
    });
  }
}
