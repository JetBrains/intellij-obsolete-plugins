package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.DMFacetBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.elements.PackagingElementOutputKind;
import com.intellij.util.concurrency.annotations.RequiresWriteLock;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class DMArtifactTypeBase<E extends WithModulePackagingElement, T extends WithModulePackagingElementType<E>, F extends DMFacetBase<?>>
  extends ArtifactType {

  private static final Logger LOG = Logger.getInstance(DMArtifactTypeBase.class);

  public DMArtifactTypeBase(@NonNls String id, Supplier<@Nls(capitalization = Nls.Capitalization.Sentence) String> title) {
    super(id, title);
  }

  @Override
  public String getDefaultPathFor(@NotNull PackagingElementOutputKind kind) {
    return "/";
  }

  protected E addOrFindModuleReference(CompositePackagingElement<?> destinationElement, Module module) {
    E moduleRef = getModulePackagingElementType().createFor(module);
    moduleRef = destinationElement.addOrFindChild(moduleRef);
    return moduleRef;
  }

  protected static void addOrFindModuleOutputReference(CompositePackagingElement<?> destinationElement, Module module) {
    PackagingElement<?> moduleOutput = PackagingElementFactory.getInstance().createModuleOutput(module);
    destinationElement.addOrFindChild(moduleOutput);
  }

  public final boolean isCompatibleArtifact(@NotNull Artifact artifact) {
    return artifact.getArtifactType() == this;
  }

  public final VirtualFile findMainFileToDeploy(@NotNull Artifact artifact) {
    return findFileByFilter(artifact, getMainFileToDeployFilter(artifact));
  }

  @NotNull
  protected abstract VirtualFileFilter getMainFileToDeployFilter(@NotNull Artifact artifact);

  private static VirtualFile findFileByFilter(@NotNull Artifact artifact, @NotNull VirtualFileFilter filter) {
    VirtualFile output = LocalFileSystem.getInstance().refreshAndFindFileByPath(artifact.getOutputPath());
    if (output == null) {
      LOG.warn("Artifact output doesn't exist, is the artifact built?");
      return null;
    }
    for (VirtualFile nextToDeploy : output.getChildren()) {
      if (filter.accept(nextToDeploy)) {
        return nextToDeploy;
      }
    }
    return null;
  }

  public static VirtualFile findFileByExtension(@NotNull Artifact artifact, @NonNls String @NotNull ... extensions) {
    return findFileByFilter(artifact, new ByExtensionFilter(extensions));
  }

  @RequiresWriteLock
  public abstract Artifact createArtifactFor(@NotNull Module module, @NotNull F facet);

  public abstract void synchronizeArtifact(@NotNull ModifiableArtifact modifiableArtifact, @NotNull Module module, @NotNull F facet);

  public abstract T getModulePackagingElementType();

  protected static class ByExtensionFilter implements VirtualFileFilter {
    private final List<String> myExtensions;

    public ByExtensionFilter(String... extensions) {
      myExtensions = new ArrayList<>(extensions.length);
      for (String next : extensions) {
        myExtensions.add(StringUtil.toLowerCase(next));
      }
    }

    @Override
    public boolean accept(@NotNull VirtualFile file) {
      if (file.isDirectory()) {
        return false;
      }
      String extension = file.getExtension();
      return myExtensions.contains(StringUtil.toLowerCase(extension));
    }
  }
}
