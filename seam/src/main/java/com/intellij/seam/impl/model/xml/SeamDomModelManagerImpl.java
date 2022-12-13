package com.intellij.seam.impl.model.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.utils.SeamConfigFileUtils;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.converters.values.GenericDomValueConvertersRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SeamDomModelManagerImpl extends SeamDomModelManager {
  private final GenericDomValueConvertersRegistry myValueProvidersRegistry;
  private final DomManager myDomManager;

  public SeamDomModelManagerImpl(Project project) {
    myDomManager = DomManager.getDomManager(project);

    myValueProvidersRegistry = new GenericDomValueConvertersRegistry();

    myValueProvidersRegistry.registerDefaultConverters();
  }

  @Override
  public boolean isSeamComponents(@NotNull final XmlFile file) {
    return myDomManager.getFileElement(file, SeamComponents.class) != null;
  }

  @Override
  public @Nullable SeamComponents getSeamModel(@NotNull final XmlFile file) {
    final DomFileElement<SeamComponents> element = myDomManager.getFileElement(file, SeamComponents.class);
    return element == null ? null : element.getRootElement();
  }

  @Override
  public @NotNull List<SeamComponents> getAllModels(@NotNull final Module module) {
    return SeamConfigFileUtils.getConfigurationFiles(module).stream()
      .map(file -> getSeamModel(file))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    //final List<DomFileElement<SeamComponents>> elements =
    //  DomService.getInstance().getFileElements(SeamComponents.class, module.getProject(), module.getModuleWithDependenciesScope());
    //return ContainerUtil.map(elements, element -> element.getRootElement());
  }

  @Override
  public GenericDomValueConvertersRegistry getValueConvertersRegistry() {
    return myValueProvidersRegistry;
  }
}
