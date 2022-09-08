package com.intellij.seam.model.xml.theme;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/theme:theme-selectorElemType interface.
 */
@Namespace(SeamNamespaceConstants.THEME_NAMESPACE_KEY)
public interface ThemeSelector extends BasicSeamComponent {
  /**
   * Returns the value of the theme child.
   *
   * @return the value of the theme child.
   */
  @NotNull
  GenericAttributeValue<String> getTheme();

  /**
   * Returns the value of the available-themes child.
   *
   * @return the value of the available-themes child.
   */
  @NotNull
  @Attribute("available-themes")
  GenericAttributeValue<String> getAvailableThemesAttr();

  /**
   * Returns the value of the cookie-max-age child.
   *
   * @return the value of the cookie-max-age child.
   */
  @NotNull
  GenericAttributeValue<String> getCookieMaxAge();

  /**
   * Returns the value of the cookie-enabled child.
   *
   * @return the value of the cookie-enabled child.
   */
  @NotNull
  GenericAttributeValue<CookieEnabled> getCookieEnabled();

  /**
   * Returns the value of the available-themes child.
   *
   * @return the value of the available-themes child.
   */
  @NotNull
  @Required
  MultiValuedProperty getAvailableThemes();
}
