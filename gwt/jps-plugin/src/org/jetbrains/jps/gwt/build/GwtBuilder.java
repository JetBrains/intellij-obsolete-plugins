package org.jetbrains.jps.gwt.build;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Processor;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.BidirectionalMultiMap;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FileCollectionFactory;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager;
import org.jetbrains.jps.builders.BuildOutputConsumer;
import org.jetbrains.jps.builders.BuildRootDescriptor;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaBuilderUtil;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.gwt.GwtJpsBundle;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.gwt.index.JpsGwtModuleIndex;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.gwt.model.JpsGwtCompilerProjectExtension;
import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.gwt.model.MavenCoordinates;
import org.jetbrains.jps.gwt.model.impl.JpsGwtCompilerProjectExtensionImpl;
import org.jetbrains.jps.gwt.model.impl.sdk.JpsGwtDependenciesStorage;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ExternalProcessUtil;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.incremental.TargetBuilder;
import org.jetbrains.jps.incremental.dependencies.DependencyResolvingBuilder;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

public final class GwtBuilder extends TargetBuilder<BuildRootDescriptor, GwtBuildTarget> {
  private static final Logger LOG = Logger.getInstance(GwtBuilder.class);
  private static final boolean SKIP_COMPILATION_FOR_COMPILE_FILE = SystemProperties.getBooleanProperty("idea.gwt.skip.compilation.for.compile.file.action", true);
  public static final String ID = "GWT Compiler";

  public GwtBuilder() {
    super(List.of(GwtBuildTargetType.INSTANCE));
  }

  @Override
  public void build(@NotNull GwtBuildTarget target,
                    @NotNull DirtyFilesHolder<BuildRootDescriptor, GwtBuildTarget> holder,
                    @NotNull BuildOutputConsumer outputConsumer,
                    @NotNull CompileContext context) throws ProjectBuildException, IOException {
    BuildDataPaths dataPaths = context.getProjectDescriptor().dataManager.getDataPaths();
    JpsGwtModuleIndex index = JpsGwtExtensionService.getInstance().getGwtModuleIndex(context.getProjectDescriptor().getModel(), dataPaths);
    JpsProject project = context.getProjectDescriptor().getProject();
    JpsJavaCompilerConfiguration compilerConfiguration = JpsJavaExtensionService.getInstance().getCompilerConfiguration(project);
    BidirectionalMultiMap<File, JpsGwtModule> rootsToModules = new BidirectionalMultiMap<>(FileCollectionFactory.createCanonicalFileMap(),
                                                                                           CollectionFactory.createSmallMemoryFootprintMap());

    JpsGwtModuleExtension extension = target.getExtension();
    final String compileReportModuleName = extension.getModule().getName().equals(GwtBuilderParameters.getCompileReportModuleName(context))
                                                 ? GwtBuilderParameters.getCompileReportGwtModuleQualifiedName(context) : null;
    final Set<JpsGwtModule> modulesToRecompile = new LinkedHashSet<>();

    for (JpsGwtModule gwtModule : index.getModules(extension, true, false)) {
      if (!compilerConfiguration.getCompilerExcludes().isExcluded(gwtModule.getModuleFile().toFile())) {
        if (gwtModule.getQualifiedName().equals(compileReportModuleName) && index.isCompilable(gwtModule, extension)) {
          modulesToRecompile.add(gwtModule);
        }

        collectModuleRoots(gwtModule, rootsToModules, gwtModule);
        for (JpsGwtModule module : index.getInheritedModules(gwtModule)) {
          collectModuleRoots(module, rootsToModules, gwtModule);
        }
      }
    }

    holder.processDirtyFiles((dirtyTarget, file, root) -> {
      collectModulesToRecompile(file, root, rootsToModules, modulesToRecompile);
      return true;
    });
    Collection<Path> removedFiles = holder.getRemoved(target);
    for (Path file : removedFiles) {
      collectModulesToRecompile(file.toFile(), null, rootsToModules, modulesToRecompile);
    }

    if (SKIP_COMPILATION_FOR_COMPILE_FILE && !context.getScope().isWholeTargetAffected(target)) {
      if (!modulesToRecompile.isEmpty()) {
        context.processMessage(new CompilerMessage(getCompilerName(), BuildMessage.Kind.INFO, GwtJpsBundle
          .message("compiler.message.gwt.compiler.for.0.module.was.not.invoked", extension.getModule().getName())));
      }
      return;
    }

    for (JpsGwtModule module : modulesToRecompile) {
      if (index.isCompilable(module, extension)) {
        compile(target, module, rootsToModules.getKeys(module), module.getQualifiedName().equals(compileReportModuleName), outputConsumer,
                context);
      }
    }
  }

  private static void collectModulesToRecompile(File file, @Nullable BuildRootDescriptor root,
                                                BidirectionalMultiMap<File, JpsGwtModule> modulesByRoots,
                                                Set<JpsGwtModule> modulesToRecompile) {
    File current = file;
    do {
      modulesToRecompile.addAll(modulesByRoots.getValues(current));
      current = FileUtil.getParentFile(current);
    }
    while (current != null && (root == null || !FileUtil.filesEqual(current, root.getRootFile())));
  }

  private static void collectModuleRoots(JpsGwtModule moduleToGetRoots,
                                         BidirectionalMultiMap<File, JpsGwtModule> modulesByRoots,
                                         JpsGwtModule targetModule) {
    modulesByRoots.put(moduleToGetRoots.getModuleFile().toFile(), targetModule);
    for (File root : moduleToGetRoots.getSourceRoots(false)) {
      modulesByRoots.put(root, targetModule);
    }
    for (File root : moduleToGetRoots.getPublicRoots(false)) {
      modulesByRoots.put(root, targetModule);
    }
  }

  private static void compile(GwtBuildTarget target,
                              final JpsGwtModule gwtModule,
                              Set<File> roots,
                              boolean generateReport, final BuildOutputConsumer outputConsumer,
                              final CompileContext context) throws IOException, ProjectBuildException {
    context.processMessage(new ProgressMessage(GwtJpsBundle.message("compiler.progress.compiling.gwt.module.0",
                                                                    gwtModule.getQualifiedName())));
    JpsGwtModuleExtension extension = target.getExtension();
    JpsModule module = extension.getModule();
    JpsSdk<JpsDummyElement> sdk = JavaBuilderUtil.ensureModuleHasJdk(module, context, getCompilerName());

    BuildDataPaths dataPaths = context.getProjectDescriptor().dataManager.getDataPaths();
    final File outputDir = JpsGwtCompilerPaths.getCompilerOutputRoot(target, dataPaths);
    if (!outputDir.mkdirs() && !outputDir.isDirectory()) {
      LOG.warn("Failed to create GWT compiler output directory: " + outputDir);
    }

    if (context.getLoggingManager().getProjectBuilderLogger().isEnabled()) {
      context.getLoggingManager().getProjectBuilderLogger().logCompiled(List.of(gwtModule.getModuleFile()), ID, "Compiling GWT modules:");
    }
    Collection<File> sourceRoots = JpsGwtClasspathUtil.getSourceRootsOfGwtModules(extension.getModule(), true);
    List<String> classpath = createClasspath(extension, sourceRoots, context);
    List<String> vmOptions = new ArrayList<>(ParametersListUtil.parse(extension.getAdditionalCompilerVMParameters()));
    vmOptions.add("-Xmx" + extension.getCompilerMaximumHeapSize() + "m");

    List<String> parameters = new ArrayList<>(ParametersListUtil.parse(extension.getCompilerParameters()));
    File extraOutputRoot = JpsGwtCompilerPaths.getExtraOutputRoot(target, dataPaths);
    if (generateReport) {
      parameters.add("-compileReport");
      parameters.add("-extra");
      parameters.add(extraOutputRoot.getAbsolutePath());
    }

    parameters.addAll(Arrays.asList("-logLevel", "TRACE"));
    parameters.addAll(Arrays.asList("-war", outputDir.getAbsolutePath()));
    parameters.addAll(Arrays.asList("-style", extension.getOutputStyle().getId()));
    parameters.add(gwtModule.getQualifiedName());

    int localWorkersIndex = 1 + parameters.indexOf("-localWorkers");
    if (localWorkersIndex != 0 && localWorkersIndex < parameters.size()) {
      String localWorkers = parameters.get(localWorkersIndex);
      if (localWorkers.endsWith("C")) {
        parameters.set(localWorkersIndex, Integer.toString((int)(Float.parseFloat(StringUtil.trimEnd(localWorkers, 'C'))
                                                                 * Runtime.getRuntime().availableProcessors())));
      }
    }

    boolean shortenCp = SystemProperties.getBooleanProperty("idea.gwt.wrap.compiler.command.line", true);
    List<String> commandLine = ExternalProcessUtil.buildJavaCommandLine(
      JpsJavaSdkType.getJavaExecutable(sdk), "com.google.gwt.dev.Compiler", emptyList(), classpath, vmOptions, parameters,
      shortenCp, false);  // classpath .jar doesn't work for GWT compiler because it manually searches for resources in URLs returned by `URLClassLoader#getURLs`
    Process process = new ProcessBuilder().command(commandLine).directory(outputDir).start();
    String moduleFileUrl = JpsPathUtil.pathToUrl(FileUtil.toSystemIndependentName(gwtModule.getModuleFile().toString()));
    JpsGwtCompilerProjectExtension compilerProjectExtension = extension.getModule().getProject().getContainer().getChild(JpsGwtCompilerProjectExtensionImpl.ROLE);
    boolean showCompilerOutput = compilerProjectExtension != null && compilerProjectExtension.isShowCompilerOutput(extension);
    GwtExternalCompilerProcessHandler handler = new GwtExternalCompilerProcessHandler(process, StringUtil.join(commandLine, " "), moduleFileUrl, sourceRoots, context);
    if (showCompilerOutput) {
      GwtBuilderMessages.sendCompilationStartedMessage(context, gwtModule);
      handler.addProcessListener(new ProcessListener() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
          GwtBuilderMessages.sendLogMessage(context, gwtModule, event.getText());
        }
      });
    }

    try {
      handler.startNotify();
      handler.waitForTerminationOrCancellation();
      context.checkCanceled();
      if (generateReport) {
        GwtBuilderMessages.sendCompileReportGenerated(context, gwtModule, new File(extraOutputRoot, gwtModule.getOutputName()).getAbsolutePath());
      }
    }
    finally {
      if (showCompilerOutput) {
        GwtBuilderMessages.sendCompilationFinishedMessage(context, gwtModule);
      }
    }

    final List<String> sourceFiles = new ArrayList<>();
    CollectPathsProcessor processor = new CollectPathsProcessor(sourceFiles);
    for (File root : roots) {
      FileUtil.processFilesRecursively(root, processor);
    }

    outputConsumer.registerOutputDirectory(outputDir, sourceFiles);
  }

  private static List<String> createClasspath(JpsGwtModuleExtension extension, Collection<File> sourceRoots, CompileContext context) {
    List<File> roots = new ArrayList<>();
    roots.addAll(sourceRoots);
    roots.addAll(JpsJavaExtensionService.dependencies(extension.getModule()).productionOnly().recursively().classes().getRoots());

    List<String> classpath = new ArrayList<>();
    GwtSdkPaths sdkPaths = extension.getSdkPaths();
    classpath.add(sdkPaths.getDevJarPath(true));
    for (File root : roots) {
      classpath.add(root.getAbsolutePath());
    }
    JpsGwtDependenciesResolver dependenciesResolver = new JpsGwtDependenciesResolver(context);
    classpath.addAll(sdkPaths.getGwtUserDependencies(dependenciesResolver));
    classpath.addAll(sdkPaths.getGwtDevDependencies(dependenciesResolver));
    return classpath;
  }

  @Override
  public @NotNull String getPresentableName() {
    return getCompilerName();
  }

  @Override
  public long getExpectedBuildTime() {
    return 500;
  }

  public static @Nls String getCompilerName() {
    return GwtJpsBundle.message("gwt.compiler.name");
  }

  private static class CollectPathsProcessor implements Processor<File> {
    private final List<String> myResult;

    CollectPathsProcessor(List<String> result) {
      myResult = result;
    }

    @Override
    public boolean process(File file) {
      if (file.isFile()) {
        myResult.add(file.getAbsolutePath());
      }
      return true;
    }
  }

  private static class JpsGwtDependenciesResolver implements GwtDependenciesResolver {
    private final CompileContext myCompileContext;
    private final JpsGwtDependenciesStorage myDependenciesStorage;

    JpsGwtDependenciesResolver(CompileContext context) {
      myCompileContext = context;
      myDependenciesStorage = new JpsGwtDependenciesStorage(myCompileContext.getProjectDescriptor().dataManager.getDataPaths());
    }

    @Override
    public @NotNull List<String> resolveDependencies(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
      ArtifactRepositoryManager repositoryManager = DependencyResolvingBuilder.getRepositoryManager(myCompileContext);
      try {
        return myDependenciesStorage.getDependenciesPaths(groupId, artifactId, version);
      }
      catch (JpsGwtDependenciesStorage.GwtDependenciesNotFoundException e) {
        myCompileContext.processMessage(new CompilerMessage(getCompilerName(), BuildMessage.Kind.WARNING, e.getMessage()));
      }

      try {
        Collection<File> files = repositoryManager.resolveDependency(groupId, artifactId, version, true, emptyList());
        List<String> resolvedPaths = ContainerUtil.map(files, File::getAbsolutePath);
        myDependenciesStorage.storeDependenciesPaths(groupId, artifactId, version, resolvedPaths);
        return resolvedPaths;
      }
      catch (Exception e) {
        LOG.info(e);
        String message = GwtJpsBundle.message("compiler.message.failed.to.resolve.0.dependencies.1", artifactId, e.getMessage());
        myCompileContext.processMessage(new CompilerMessage(getCompilerName(), BuildMessage.Kind.WARNING, message));
        return emptyList();
      }
    }

    @Override
    public @NotNull List<String> resolveAnyOf(@NotNull Collection<MavenCoordinates> alternatives) {
      ArtifactRepositoryManager repositoryManager = DependencyResolvingBuilder.getRepositoryManager(myCompileContext);

      List<CompilerMessage> compilerMessages = new ArrayList<>();

      for (MavenCoordinates alternative : alternatives) {
        try {
          return myDependenciesStorage.getDependenciesPaths(alternative.groupId, alternative.artifactId, alternative.version);
        }
        catch (JpsGwtDependenciesStorage.GwtDependenciesNotFoundException e) {
          compilerMessages.add(new CompilerMessage(getCompilerName(), BuildMessage.Kind.WARNING, e.getMessage()));
        }
      }

      for (CompilerMessage message : compilerMessages) {
        myCompileContext.processMessage(message);
      }
      compilerMessages.clear();

      for (MavenCoordinates alternative : alternatives) {
        try {
          Collection<File> files = repositoryManager.resolveDependency(alternative.groupId, alternative.artifactId, alternative.version,
                                                                       true, emptyList());
          List<String> resolvedPaths = ContainerUtil.map(files, File::getAbsolutePath);
          myDependenciesStorage.storeDependenciesPaths(alternative.groupId, alternative.artifactId, alternative.version, resolvedPaths);
          return resolvedPaths;
        }
        catch (Exception e) {
          String message = GwtJpsBundle.message("compiler.message.failed.to.resolve.0.dependencies.1", alternative.artifactId, e.getMessage());
          compilerMessages.add(new CompilerMessage(getCompilerName(), BuildMessage.Kind.WARNING, message));
        }
      }

      for (CompilerMessage message : compilerMessages) {
        myCompileContext.processMessage(message);
      }

      return emptyList();
    }
  }
}
