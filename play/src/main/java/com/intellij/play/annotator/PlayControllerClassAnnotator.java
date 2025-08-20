package com.intellij.play.annotator;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.play.PlayIcons;
import com.intellij.play.utils.PlayBundle;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PlayControllerClassAnnotator extends RelatedItemLineMarkerProvider {
  @Override
  protected void collectNavigationMarkers(@NotNull PsiElement psiElement, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    if (psiElement instanceof PsiIdentifier) {
      final PsiElement parent = psiElement.getParent();
      if (parent instanceof PsiMethod psiMethod) {
        if (PlayUtils.isController(psiMethod.getContainingClass())) {
            if (psiMethod.hasModifierProperty(PsiModifier.STATIC) && psiMethod.hasModifierProperty(PsiModifier.PUBLIC)) {
              PsiFile viewFile = PlayPathUtils.getCorrespondingView(psiMethod);
              if (viewFile != null) {
                final NavigationGutterIconBuilder<PsiElement> builder =
                  NavigationGutterIconBuilder.
                    create(PlayIcons.Play).
                    setTargets(viewFile).
                    setTooltipText(
                      PlayBundle.message("goto.custom.view"));

                result.add(builder.createLineMarkerInfo(psiMethod.getNameIdentifier()));
              }
          }
        }
      }
    }
  }
}
