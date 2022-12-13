package com.intellij.seam.model.xml.core;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.seam.model.xml.components.SeamValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Namespace(SeamNamespaceConstants.CORE_NAMESPACE_KEY)
public interface BasicBundleNamesHolder extends BasicSeamComponent, CommonModelElement {

  /**
   * Returns the value of the simple content.
   *
   * @return the value of the simple content.
   */
  @NotNull
  @Required
  String getValue();

  /**
   * Sets the value of the simple content.
   *
   * @param value the new value to set
   */
  void setValue(@NotNull String value);

  /**
   * Returns the value of the bundle-names child.
   *
   * @return the value of the bundle-names child.
   */
  @NotNull
  SeamValue getBundleNames();

  /**
   * Returns the list of bundle-names children.
   *
   * @return the list of bundle-names children.
   */
  @NotNull
  @SubTagList("bundle-names") List<MultiValuedProperty> getBundleNameses();
}

