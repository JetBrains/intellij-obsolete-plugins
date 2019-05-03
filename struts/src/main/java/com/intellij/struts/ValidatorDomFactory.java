/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
