package org.jetbrains.jps.dmserver.model.impl;

import org.jdom.Element;
import org.jetbrains.jps.dmserver.model.JpsDMContainerPackagingElement;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.module.JpsModuleReference;
import org.jetbrains.jps.model.serialization.artifact.JpsPackagingElementSerializer;

/**
 * @author michael.golubev
 */
public final class JpsDMContainerPackagingElementSerializer extends JpsPackagingElementSerializer<JpsDMContainerPackagingElement> {
  private static final String MODULE_ATTRIBUTE = "module";

  public JpsDMContainerPackagingElementSerializer() {
    super(JpsDMContainerPackagingElement.TYPE_ID, JpsDMContainerPackagingElement.class);
  }

  @Override
  public JpsDMContainerPackagingElement load(Element element) {
    JpsModuleReference reference = JpsElementFactory.getInstance().createModuleReference(element.getAttributeValue(MODULE_ATTRIBUTE));
    return new JpsDMContainerPackagingElementImpl(reference);
  }
}
