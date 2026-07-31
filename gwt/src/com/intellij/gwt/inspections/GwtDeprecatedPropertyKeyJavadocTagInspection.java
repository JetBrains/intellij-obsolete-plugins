package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.rpc.GwtGenericsUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GwtDeprecatedPropertyKeyJavadocTagInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkClass(final @NotNull PsiClass aClass, final @NotNull InspectionManager manager,
                                        final boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null || !gwtFacet.getSdkVersion().isGenericsSupported()) {
      return null;
    }

    PropertiesFile[] files = GwtI18nManager.getInstance(manager.getProject()).getPropertiesFiles(aClass);
    if (files.length == 0) return null;

    return checkInterface(aClass, manager, isOnTheFly);
  }

  private static ProblemDescriptor[] checkInterface(final PsiClass anInterface, final InspectionManager manager, boolean isOnTheFly) {
    List<ProblemDescriptor> problems = new ArrayList<>();
    for (PsiMethod method : anInterface.getMethods()) {
      PsiDocComment comment = method.getDocComment();
      if (comment != null) {
        PsiDocTag tag = comment.findTagByName(GwtI18nUtil.GWT_KEY_TAG);
        if (tag != null) {
          ReplaceTagByAnnotationQuickFix fix = new ReplaceTagByAnnotationQuickFix(tag, method);
          String message = GwtBundle.message("problem.description.gwt.key.tag.is.deprecated.in.gwt.1.5");
          problems.add(manager.createProblemDescriptor(tag, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
        }
      }
    }
    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  private static class ReplaceTagByAnnotationQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    ReplaceTagByAnnotationQuickFix(final PsiDocTag tag, final PsiMethod method) {
      super(GwtBundle.message("quickfix.family.name.replace.gwt.key.tag.with.key.annotation"), GwtBundle.message("quickfix.name.replace.gwt.key.tag.with.key.annotation.in.method.0",
                                                PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME |
                                                                                                         PsiFormatUtilBase.SHOW_PARAMETERS,
                                                                           PsiFormatUtilBase.SHOW_TYPE)),
            tag, method);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiDocTag tag) || !(endElement instanceof PsiMethod method)) return;

      PsiDocTagValue tagValueElement = tag.getValueElement();
      if (tagValueElement == null) return;

      String propertyName = tagValueElement.getText();
      try {
        GwtI18nUtil.addKeyAnnotation(propertyName, method, JavaPsiFacade.getInstance(project).getElementFactory());
        GwtGenericsUtil.removeJavadocTags(method, GwtI18nUtil.GWT_KEY_TAG);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }
  }
}
