package org.jetbrains.jps.gwt.model.impl;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleReference;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;
import org.jetbrains.jps.model.serialization.artifact.JpsPackagingElementSerializer;
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer;
import org.jetbrains.jps.model.serialization.facet.JpsFacetSerializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants.GWT_COMPILER_DEPLOY_OUTPUT_ELEMENT_ID;
import static org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants.GWT_COMPILER_OUTPUT_ELEMENT_ID;
import static org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants.GWT_FACET_ID;
import static org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants.GWT_FACET_NAME;
import static org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants.PACKAGING_FACET_ATTRIBUTE;

public class JpsGwtModelSerializerExtension extends JpsModelSerializerExtension {
  public static final @NonNls String GWT_COMPILER_CONFIGURATION_COMPONENT_NAME = "GwtCompilerConfiguration";

  @Override
  public @NotNull List<? extends JpsProjectExtensionSerializer> getProjectExtensionSerializers() {
    return Collections.singletonList(new JpsGwtWorkspaceConfigurationSerializer());
  }

  @Override
  public @NotNull List<? extends JpsFacetConfigurationSerializer<?>> getFacetConfigurationSerializers() {
    return Collections.singletonList(new JpsGwtFacetConfigurationSerializer());
  }

  @Override
  public @NotNull List<? extends JpsPackagingElementSerializer<?>> getPackagingElementSerializers() {
    return Arrays.asList(
      new JpsGwtCompilerOutputElementSerializer(GWT_COMPILER_OUTPUT_ELEMENT_ID, JpsGwtCompilerOutputPackagingElement.OutputKind.REGULAR),
      new JpsGwtCompilerOutputElementSerializer(GWT_COMPILER_DEPLOY_OUTPUT_ELEMENT_ID, JpsGwtCompilerOutputPackagingElement.OutputKind.DEPLOY)
    );
  }

  private static class JpsGwtFacetConfigurationSerializer extends JpsFacetConfigurationSerializer<JpsGwtModuleExtension> {
    JpsGwtFacetConfigurationSerializer() {
      super(JpsGwtModuleExtensionImpl.ROLE, GWT_FACET_ID, GWT_FACET_NAME);
    }

    @Override
    protected JpsGwtModuleExtension loadExtension(@NotNull Element facetConfigurationElement,
                                                  String name,
                                                  JpsElement parent,
                                                  JpsModule module) {
      GwtModuleExtensionProperties properties = XmlSerializer.deserialize(facetConfigurationElement, GwtModuleExtensionProperties.class);
      return new JpsGwtModuleExtensionImpl(properties);
    }
  }

  private static class JpsGwtCompilerOutputElementSerializer extends JpsPackagingElementSerializer<JpsGwtCompilerOutputPackagingElement> {
    private final JpsGwtCompilerOutputPackagingElement.OutputKind myOutputKind;

    JpsGwtCompilerOutputElementSerializer(final String typeId, final JpsGwtCompilerOutputPackagingElement.OutputKind outputKind) {
      super(typeId, JpsGwtCompilerOutputPackagingElement.class);
      myOutputKind = outputKind;
    }

    @Override
    public JpsGwtCompilerOutputPackagingElement load(Element element) {
      JpsModuleReference moduleReference = JpsFacetSerializer.createModuleReference(element.getAttributeValue(PACKAGING_FACET_ATTRIBUTE));
      return new JpsGwtCompilerOutputPackagingElement(moduleReference, myOutputKind);
    }
  }

  private static final class JpsGwtWorkspaceConfigurationSerializer extends JpsProjectExtensionSerializer {
    private JpsGwtWorkspaceConfigurationSerializer() {
      super(WORKSPACE_FILE, GWT_COMPILER_CONFIGURATION_COMPONENT_NAME);
    }

    @Override
    public void loadExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
      GwtCompilerWorkspaceState state = XmlSerializer.deserialize(componentTag, GwtCompilerWorkspaceState.class);
      JpsGwtCompilerProjectExtensionImpl extension = new JpsGwtCompilerProjectExtensionImpl(state.getModulesToShowCompilerOutput());
      project.getContainer().setChild(JpsGwtCompilerProjectExtensionImpl.ROLE, extension);
    }
  }
}
