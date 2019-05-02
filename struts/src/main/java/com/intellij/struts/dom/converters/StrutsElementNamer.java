package com.intellij.struts.dom.converters;

import com.intellij.ide.presentation.PresentationProvider;
import com.intellij.struts.dom.SetProperty;
import com.intellij.struts.dom.validator.Arg;
import com.intellij.struts.dom.validator.Formset;

/**
 * @author Dmitry Avdeev
 */
@SuppressWarnings({"UnusedDeclaration"})
public class StrutsElementNamer extends PresentationProvider {

  @Override
  public String getName(Object element) {
    if (element instanceof SetProperty) {
      final SetProperty setProperty = ((SetProperty)element);
      final String property = setProperty.getProperty().getStringValue();
      if (property != null) {
        return property;
      }
      return setProperty.getKey().getStringValue();
    } else if (element instanceof Formset) {
      final Formset formset = ((Formset) element);
      String lang = formset.getLanguage().getStringValue();
      String country = formset.getCountry().getStringValue();
      String variant = formset.getVariant().getStringValue();
      String name = lang;
      if (country != null) {
        name = name == null ? country : name + "_" + country;
      }
      if (variant != null) {
        name = name == null ? variant : name + "_" + variant;
      }
      return name;
    } else if (element instanceof Arg) {
      final String name = ((Arg) element).getName().getStringValue();
      if (name == null) {
        return null;
      }
      final String position = ((Arg) element).getPosition().getStringValue();
      return position == null ? name : name + position;
    }
    return null;
  }
}
