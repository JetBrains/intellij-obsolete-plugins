package com.intellij.seam.providers;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SeamImplicitUsageProvider implements ImplicitUsageProvider {
  private static final List<String> READ_SYMBOL_ANNOTATIONS =
      Arrays.asList(SeamAnnotationConstants.JSF_DATA_MODEL_ANNOTATION,
                    SeamAnnotationConstants.OUT_ANNOTATION,
                    SeamAnnotationConstants.FACTORY_ANNOTATION,
                    SeamAnnotationConstants.UNWRAP_ANNOTATION,
                    SeamAnnotationConstants.COMPONENT_ANNOTATION,
                    SeamAnnotationConstants.CREATE_ANNOTATION,
                    SeamAnnotationConstants.DESTROY_ANNOTATION);

  private static final List<String> WRITE_SYMBOL_ANNOTATIONS =
      Arrays.asList(SeamAnnotationConstants.JSF_DATA_MODEL_SELECTION_ANNOTATION,
                    SeamAnnotationConstants.JSF_DATA_MODEL_SELECTION_INDEX_ANNOTATION,
                    SeamAnnotationConstants.IN_ANNOTATION,
                    SeamAnnotationConstants.LOGGER_ANNOTATION,
                    SeamAnnotationConstants.REQUEST_PARAMETER_ANNOTATION_1_0,
                    SeamAnnotationConstants.REQUEST_PARAMETER_ANNOTATION_2_0);

  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    return isImplicitRead(element) || isImplicitWrite(element);
  }

  @Override
  public boolean isImplicitRead(@NotNull final PsiElement element) {
    return element instanceof PsiModifierListOwner && AnnotationUtil.isAnnotated((PsiModifierListOwner)element, READ_SYMBOL_ANNOTATIONS, 0);
  }

  @Override
  public boolean isImplicitWrite(@NotNull final PsiElement element) {
    return element instanceof PsiModifierListOwner && AnnotationUtil.isAnnotated((PsiModifierListOwner)element, WRITE_SYMBOL_ANNOTATIONS, 0);
  }
}
