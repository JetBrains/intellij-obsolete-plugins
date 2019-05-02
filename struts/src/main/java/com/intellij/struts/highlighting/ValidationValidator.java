package com.intellij.struts.highlighting;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.project.Project;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.struts.facet.StrutsFacet;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Provides {@link ValidatorInspection} on make.
 *
 * @author Dmitry Avdeev
 */
public class ValidationValidator extends StrutsValidatorBase {

  public ValidationValidator() {
    super("Struts Validation Model validator", "Validating Struts Validator model...");
  }

  @Override
  protected DomModelFactory getFactory(Project project) {
    return StrutsProjectComponent.getInstance(project).getValidatorFactory();
  }

  @Override
  protected boolean isAvailableOnFacet(final StrutsFacet facet) {
    return facet.getConfiguration().getValidationConfiguration().myValidatorValidationEnabled;
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public Class<? extends LocalInspectionTool>[] getInspectionToolClasses(CompileContext context) {
    return new Class[]{ValidatorInspection.class};
  }
}
