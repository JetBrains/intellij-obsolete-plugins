/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.struts;

import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.jsp.WebDirectoryUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.dom.validator.FormValidation;
import com.intellij.struts.inplace.reference.StrutsReferenceContributor;
import com.intellij.struts.psi.ValidationModelImpl;
import com.intellij.util.xml.DomFileElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
class ValidatorDomFactory extends StrutsPluginDomFactory<FormValidation, ValidationModel> {

  @NonNls
  private static final String VALIDATOR_PLUGIN_CLASS = "org.apache.struts.validator.ValidatorPlugIn";
  @NonNls
  private static final String PATHNAMES_PROPERTY = "pathnames";

  private final Project project;

  protected ValidatorDomFactory(@NotNull final StrutsDomFactory strutsFactory, final Project project) {
    super(FormValidation.class,
          VALIDATOR_PLUGIN_CLASS,
          PATHNAMES_PROPERTY,
          strutsFactory,
          project,
          "Validator");
    this.project = project;
  }

  @Override
  protected ValidationModel createModel(@NotNull final Set<XmlFile> configFiles,
                                        @NotNull final DomFileElement<FormValidation> mergedModel,
                                        final StrutsModel strutsModel) {
    return new ValidationModelImpl(configFiles, mergedModel, strutsModel);
  }

  @Override
  protected ValidationModel createCombinedModel(@NotNull final Set<XmlFile> configFiles,
                                                @NotNull final DomFileElement<FormValidation> mergedModel,
                                                final ValidationModel firstModel, final Module module) {
    return new ValidationModelImpl(configFiles, mergedModel, firstModel.getStrutsModel());
  }

  @Override
  protected PsiElement resolveFile(final String path,
                                   final WebDirectoryUtil webDirectoryUtil,
                                   final WebFacet webFacet) {
    if (path.equals(StrutsReferenceContributor.VALIDATOR_RULES_XML)) {
      return StrutsReferenceContributor.resolveValidatorConfigInJAR(project);
    }
    return super.resolveFile(path, webDirectoryUtil, webFacet);
  }
}
