package com.intellij.seam.model.xml.components;

import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.model.converters.SeamPropertyNameConverter;
import com.intellij.seam.model.converters.SeamPropertyValueConverter;
import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

@Namespace(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY)
@Convert(value = SeamPropertyValueConverter.class, soft = false)
public interface SeamProperty extends BasicProperty, SeamDomElement, SeamValue {

  @NotNull
  @Required
  @NameValue(unique = true)
  @Convert(value = SeamPropertyNameConverter.class)  
  GenericAttributeValue<BeanProperty> getName();
}
