/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.libraries.FacetLibrariesValidator;
import com.intellij.facet.ui.libraries.LibraryInfo;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.struts.core.JDOMClassExternalizer;
import com.intellij.struts.facet.ui.StrutsFeaturesEditor;
import com.intellij.struts.facet.ui.StrutsLibrariesValidatorDescription;
import com.intellij.struts.facet.ui.StrutsValidationEditor;
import org.jdom.Element;

/**
 * @author nik
 */
public class StrutsFacetConfiguration implements FacetConfiguration {
  private final StrutsValidationConfiguration myValidationConfiguration = new StrutsValidationConfiguration();

  @Override
  public FacetEditorTab[] createEditorTabs(final FacetEditorContext editorContext, final FacetValidatorsManager validatorsManager) {
    FacetLibrariesValidator validator = FacetEditorsFactory.getInstance().createLibrariesValidator(LibraryInfo.EMPTY_ARRAY, new StrutsLibrariesValidatorDescription(), editorContext, validatorsManager);

    validatorsManager.registerValidator(validator);
    final StrutsValidationEditor validationEditor = new StrutsValidationEditor(myValidationConfiguration);
    StrutsFeaturesEditor featuresEditor = new StrutsFeaturesEditor(editorContext, validator);
    return new FacetEditorTab[]{
      featuresEditor,
      validationEditor
    };
  }

  @Override
  public void readExternal(final Element element) throws InvalidDataException {
    JDOMClassExternalizer.readExternal(myValidationConfiguration, element);
  }

  @Override
  public void writeExternal(final Element element) throws WriteExternalException {
    JDOMClassExternalizer.writeExternal(myValidationConfiguration, element);
  }

  public StrutsValidationConfiguration getValidationConfiguration() {
    return myValidationConfiguration;
  }
}
