package com.intellij.seam.highlighting.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.javaee.ejb.model.EnterpriseBean;
import com.intellij.javaee.ejb.model.EntityBean;
import com.intellij.javaee.ejb.model.SessionBean;
import com.intellij.javaee.ejb.role.EjbClassRole;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamRole;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModel;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.List;

public class SeamIllegalComponentScopeInspection extends SeamJamModelInspectionBase {

  @Override
  protected void checkSeamJamComponent(final SeamJamComponent jamComponent, final ProblemsHolder holder) {
    checkBeans(jamComponent, holder);
    checkDataModels(jamComponent, holder);
  }

  private static void checkDataModels(final SeamJamComponent jamComponent, final ProblemsHolder holder) {
    for (final SeamJamDataModel dataModel : jamComponent.getDataModels()) {
      checkComponentScope(holder, SeamScopeTypes.DATAMODEL, dataModel.getScope(), () -> dataModel.getIdentifyingAnnotation());
    }
  }

  private static void checkBeans(final SeamJamComponent seamJamComponent, final ProblemsHolder holder) {
    for (EjbClassRole ejbClassRole : SeamCommonUtils.getEjbRoles(seamJamComponent)) {
      EnterpriseBean bean = ejbClassRole.getEnterpriseBean();
      if (bean instanceof SessionBean) {
        if (SeamCommonUtils.isStateful((SessionBean)bean)) {
          checkComponentScopes(seamJamComponent, holder, SeamScopeTypes.STATEFUL);
        }
        else {
          checkComponentScopes(seamJamComponent, holder, SeamScopeTypes.STATELESS);
        }
      }
      else if (bean instanceof EntityBean) {
        checkComponentScopes(seamJamComponent, holder, SeamScopeTypes.ENTITY);
      }
    }
  }

  private static void checkComponentScopes(final SeamJamComponent seamComponent,
                                           final ProblemsHolder holder,
                                           final SeamScopeTypes scopeTypes) {

    final SeamComponentScope componentScope = seamComponent.getComponentScope();
    checkComponentScope(holder, scopeTypes, componentScope, () -> {
      PsiAnnotation scopeAnnotation =
        AnnotationUtil.findAnnotation(seamComponent.getPsiElement(), SeamAnnotationConstants.SCOPE_ANNOTATION);
      return scopeAnnotation == null ? seamComponent.getIdentifyingPsiElement() : scopeAnnotation;
    });

    for (final SeamJamRole seamJamRole : seamComponent.getRoles()) {
      checkComponentScope(holder, scopeTypes, seamJamRole.getScope(), () -> seamJamRole.getIdentifyingAnnotation());
    }
  }

  private static void checkComponentScope(final ProblemsHolder holder,
                                          final SeamScopeTypes scopeTypes,
                                          final SeamComponentScope scope,
                                          Factory<PsiElement> getPsiElement) {
    if (scope == null || scope.isEqual(SeamComponentScope.UNSPECIFIED)) return;

    if (!scopeTypes.isLegalScope(scope)) {
      holder.registerProblem(getPsiElement.create(), SeamInspectionBundle.message("illegal.component.scope", scopeTypes.getName(), scope,
                                                                                  scopeTypes.getAllowedScopes()));
    }
  }

  private enum SeamScopeTypes {
    @NonNls DATAMODEL("@DataModel", SeamComponentScope.UNSPECIFIED, SeamComponentScope.PAGE),
    @NonNls STATELESS("Stateless Session Bean", SeamComponentScope.STATELESS),
    @NonNls STATEFUL("Stateful Session Bean", SeamComponentScope.CONVERSATION, SeamComponentScope.BUSINESS_PROCESS,
                     SeamComponentScope.SESSION, SeamComponentScope.APPLICATION, SeamComponentScope.EVENT),
    @NonNls ENTITY("Entity Bean", SeamComponentScope.CONVERSATION, SeamComponentScope.EVENT, SeamComponentScope.PAGE,
                   SeamComponentScope.BUSINESS_PROCESS, SeamComponentScope.SESSION, SeamComponentScope.APPLICATION);

    private final String myName;
    private final List<SeamComponentScope> myAllowedScopes;

    SeamScopeTypes(String name, SeamComponentScope... allowedScopes) {
      myName = name;
      myAllowedScopes = Arrays.asList(allowedScopes);
    }

    public String getFullName() {
      return myName;
    }

    public boolean isLegalScope(SeamComponentScope scope) {
      for (SeamComponentScope allowedScope : myAllowedScopes) {
        if (allowedScope.isEqual(scope)) return true;
      }
      return false;
    }

    public String getName() {
      return myName;
    }

    public List<SeamComponentScope> getAllowedScopes() {
      return myAllowedScopes;
    }
  }
}
