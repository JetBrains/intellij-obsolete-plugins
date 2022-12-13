package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.openapi.module.Module;
import com.intellij.packaging.elements.CompositePackagingElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import java.util.function.Supplier;

public abstract class DMContainerArtifactTypeBase
  extends DMArtifactTypeBase<DMContainerPackagingElement, DMContainerPackagingElementType, DMBundleFacet> {

  public DMContainerArtifactTypeBase(@NonNls String id, Supplier<@Nls String> title) {
    super(id, title);
  }

  @Override
  public DMContainerPackagingElementType getModulePackagingElementType() {
    return DMContainerPackagingElementType.getInstance();
  }

  @Override
  protected DMContainerPackagingElement addOrFindModuleReference(CompositePackagingElement<?> destinationElement, Module module) {
    DMContainerPackagingElement result = super.addOrFindModuleReference(destinationElement, module);

    //we need this to workaround the "missing osmorc jar" problem:
    //if the artifact is not associated with module output, the related ArtifactScope won't contain any modules,
    //and the BundleCompiler will delete the created bundle jars as "outdated"
    addOrFindModuleOutputReference(result, module);

    return result;
  }
}
