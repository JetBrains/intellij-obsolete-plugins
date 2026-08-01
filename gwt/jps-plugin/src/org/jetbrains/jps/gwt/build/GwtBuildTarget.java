package org.jetbrains.jps.gwt.build;

import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildRootDescriptor;
import org.jetbrains.jps.builders.BuildRootIndex;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.BuildTargetRegistry;
import org.jetbrains.jps.builders.TargetOutputIndex;
import org.jetbrains.jps.builders.impl.BuildRootDescriptorImpl;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.indices.IgnoredFileIndex;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GwtBuildTarget extends BuildTarget<BuildRootDescriptor> {
  private final JpsGwtModuleExtension myExtension;

  public GwtBuildTarget(JpsGwtModuleExtension extension) {
    super(GwtBuildTargetType.INSTANCE);
    myExtension = extension;
  }

  @Override
  public @NotNull String getId() {
    return myExtension.getModule().getName();
  }

  public JpsGwtModuleExtension getExtension() {
    return myExtension;
  }

  @Override
  public @NotNull Collection<BuildTarget<?>> computeDependencies(@NotNull BuildTargetRegistry targetRegistry, @NotNull TargetOutputIndex outputIndex) {
    return Collections.unmodifiableCollection(targetRegistry.getModuleBasedTargets(myExtension.getModule(), BuildTargetRegistry.ModuleTargetSelector.PRODUCTION));
  }

  @Override
  public @NotNull List<BuildRootDescriptor> computeRootDescriptors(@NotNull JpsModel model, @NotNull ModuleExcludeIndex index, @NotNull IgnoredFileIndex ignoredFileIndex,
                                                                   @NotNull BuildDataPaths dataPaths) {
    final List<BuildRootDescriptor> roots = new ArrayList<>();
    processGwtExtensionFromDependencies(extension -> {
      for (JpsModuleSourceRoot sourceRoot : extension.getModule().getSourceRoots(JavaSourceRootType.SOURCE)) {
        final File root = JpsPathUtil.urlToFile(sourceRoot.getUrl());
        roots.add(new BuildRootDescriptorImpl(this, root, true));
      }
    });
    return roots;
  }

  public void processGwtExtensionFromDependencies(final Consumer<JpsGwtModuleExtension> processor) {
    JpsJavaExtensionService.dependencies(myExtension.getModule()).recursively().productionOnly().forEachModule(module -> {
      JpsGwtModuleExtension extension = JpsGwtExtensionService.getInstance().getExtension(module);
      if (extension != null) {
        processor.consume(extension);
      }
    });
  }

  @Override
  public @Nullable BuildRootDescriptor findRootDescriptor(@NotNull String rootId, @NotNull BuildRootIndex rootIndex) {
    for (BuildRootDescriptor descriptor : rootIndex.getTargetRoots(this, null)) {
      if (descriptor.getRootId().equals(rootId)) {
        return descriptor;
      }
    }
    return null;
  }

  @Override
  public @NotNull String getPresentableName() {
    return "GWT in module '" + myExtension.getModule().getName() + "'";
  }

  @Override
  public @NotNull Collection<File> getOutputRoots(@NotNull CompileContext context) {
    final File root = JpsGwtCompilerPaths.getCompilerOutputRoot(this, context.getProjectDescriptor().dataManager.getDataPaths());
    return Collections.singleton(root);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GwtBuildTarget target = (GwtBuildTarget)o;
    return myExtension.equals(target.myExtension);
  }

  @Override
  public int hashCode() {
    return myExtension.hashCode();
  }
}
