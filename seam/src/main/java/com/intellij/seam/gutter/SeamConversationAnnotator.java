package com.intellij.seam.gutter;

import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.context.SeamJamBegin;
import com.intellij.seam.model.jam.context.SeamJamEnd;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SeamConversationAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder) {
    if (psiElement instanceof PsiIdentifier) {
      final PsiElement parent = psiElement.getParent();
      if (parent instanceof PsiClass) {
        final PsiClass aClass = (PsiClass)parent;
        SeamJamComponent seamJamComponent = SeamCommonUtils.getSeamJamComponent(aClass);
        if (seamJamComponent != null)  {
          final List<PsiMethod> begins = ContainerUtil.map2List(seamJamComponent.getBegins(), seamJamBegin -> seamJamBegin.getPsiElement()) ;

          final List<PsiMethod> ends = ContainerUtil.map2List(seamJamComponent.getEnds(), end -> end.getPsiElement()) ;

          for (SeamJamBegin begin : seamJamComponent.getBegins()) {
            NavigationGutterIconBuilder.create(SeamIcons.ToEndConversation).
            setTargets(ends).
            setCellRenderer(MyPsiElementListCellRenderer::new).
            setPopupTitle(SeamBundle.message("seam.begin.conversation.to.end.title")).
            setTooltipText(SeamBundle.message("seam.begin.conversation.to.end.tooltip.text")).
              createGutterIcon(holder, begin.getIdentifyingAnnotation());
          }

          for (SeamJamEnd end : seamJamComponent.getEnds()) {
            NavigationGutterIconBuilder.create(SeamIcons.ToBeginConversation).
            setTargets(begins).
            setCellRenderer(MyPsiElementListCellRenderer::new).
            setPopupTitle(SeamBundle.message("seam.end.conversation.to.begin.title")).
            setTooltipText(SeamBundle.message("seam.end.conversation.to.begin.tooltip.text")).
              createGutterIcon(holder, end.getIdentifyingAnnotation());
          }
        }
      }
    }
  }


  private static class MyPsiElementListCellRenderer extends PsiElementListCellRenderer<PsiMethod> {
    @Override
    public String getElementText(final PsiMethod element) {
      return element.getName();
    }

    @Override
    protected String getContainerText(final PsiMethod element, final String name) {
      return null;
    }
  }
}
