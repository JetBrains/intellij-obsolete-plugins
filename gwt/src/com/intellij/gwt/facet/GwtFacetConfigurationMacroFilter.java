package com.intellij.gwt.facet;

import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.util.xmlb.Constants;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

final class GwtFacetConfigurationMacroFilter extends PathMacroFilter {
  @Override
  public boolean recursePathMacros(@NotNull Attribute attribute) {
    if (!Constants.VALUE.equals(attribute.getName())) {
      return false;
    }

    Element setting = attribute.getParent();
    if (setting != null && "setting".equals(setting.getName())) {
      Element configuration = setting.getParentElement();
      if (configuration != null && "configuration".equals(configuration.getName())) {
        Element facet = configuration.getParentElement();
        if (facet != null && "facet".equals(facet.getName())) {
          Attribute type = facet.getAttribute("type");
          if (type != null && GwtFacetType.ID.toString().equals(type.getValue())) {

            Attribute settingName = setting.getAttribute(Constants.NAME);
            if (settingName != null) {
              return Arrays.asList("compilerParameters", "additionalCompilerParameters").contains(settingName.getValue());
            }
          }
        }
      }
    }
    return false;
  }
}
