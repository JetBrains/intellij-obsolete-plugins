/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
