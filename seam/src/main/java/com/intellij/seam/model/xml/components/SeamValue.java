package com.intellij.seam.model.xml.components;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Namespace;
import com.intellij.seam.constants.SeamNamespaceConstants;

@Namespace(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY)

public interface SeamValue extends GenericDomValue<Object>  {
}
