/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.gutter;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.ElementPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * Provides "Go To Declaration" action in gutter mark.
 *
 * @author davdeev
 * @author Yann C&eacute;bron
 */
public abstract class GotoDeclGutter extends GutterIconRendererBase implements DumbAware {

  private final PsiElement myElement;
  private final String myTooltip;
  private final AnAction myClickAction;

  private static final DomElementListCellRenderer DOM_ELEMENT_LIST_CELL_RENDERER = new DomElementListCellRenderer();

  protected GotoDeclGutter(@NotNull final PsiElement element, @NotNull final Icon icon, final String tooltip) {
    super(icon);
    myElement = element;
    myTooltip = tooltip;
    myClickAction = new AnAction() {

      @Override
      public void actionPerformed(@NotNull final AnActionEvent e) {
        final DomElement[] elements = getDestinations(myElement);
        if (elements == null || elements.length == 0) {

        } else if (elements.length == 1) {
          // only one navigation target
          final PsiElement element = elements[0].getXmlTag();
          if (element instanceof Navigatable && ((Navigatable) element).canNavigateToSource()) {
            ((Navigatable) element).navigate(true);
          }
        } else {
          // show popup for selecting navigation target from list
          final JBPopup gotoDeclarationPopup = NavigationUtil.getPsiElementPopup(
            DomUtil.getElementTags(elements),
            DOM_ELEMENT_LIST_CELL_RENDERER,
            "Goto " + elements[0].getPresentation().getTypeName() + " Declaration");
          gotoDeclarationPopup.show(new RelativePoint((MouseEvent) e.getInputEvent()));
        }
      }
    };
  }

  @Nullable
  protected abstract DomElement[] getDestinations(@NotNull PsiElement element);

  @Override
  @Nullable
  public String getTooltipText() {
    return myTooltip;
  }

  @Override
  @Nullable
  public AnAction getClickAction() {
    return myClickAction;
  }

  @Override
  public boolean isNavigateAction() {
    return true;
  }

  /**
   * Renderer for DOM elements in multiple targets popup.
   */
  private static class DomElementListCellRenderer extends PsiElementListCellRenderer {

    /**
     * Gets the presentation text for the given element.
     *
     * @param psiElement Element from list.
     *
     * @return DomElement's presentation name.
     */
    @Override
    public String getElementText(final PsiElement psiElement) {
      return getDomElementPresentation(psiElement).getElementName();
    }

    /**
     * Show corresponding Struts Module prefix or name of containing file.
     *
     * @param psiElement Element from list.
     * @param s          Present container text.
     *
     * @return Container text.
     */
    @Override
    protected String getContainerText(final PsiElement psiElement,
                                      final String s) {
      final StrutsModel model = StrutsManager.getInstance().getStrutsModel(psiElement);
      if (model != null) {
        return '[' + model.getModulePrefix() + ']';
      } else {
        return '(' + psiElement.getContainingFile().getName() + ')';
      }
    }

    /**
     * Show corresponding icon for element.
     *
     * @param psiElement Element from list.
     *
     * @return DomElement's presentation icon.
     */
    @Override
    protected Icon getIcon(final PsiElement psiElement) {
      return getDomElementPresentation(psiElement).getIcon();
    }

    @Override
    protected int getIconFlags() {
      return 0;
    }

    /**
     * Gets the DOM presentation for the given PsiElement.
     *
     * @param psiElement PsiElement to get presentation for.
     *
     * @return ElementPresentation.
     */
    @NotNull
    private static ElementPresentation getDomElementPresentation(final PsiElement psiElement) {
      final DomElement domElement = DomManager.getDomManager(psiElement.getProject())
        .getDomElement((XmlTag) psiElement);
      assert domElement != null; // we only pass XmlTag from DOM-mapped config files
      return domElement.getPresentation();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GotoDeclGutter that = (GotoDeclGutter)o;

    if (myElement != null ? !myElement.equals(that.myElement) : that.myElement != null) return false;
    if (myTooltip != null ? !myTooltip.equals(that.myTooltip) : that.myTooltip != null) return false;
    if (!getIcon().equals(that.getIcon())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myElement != null ? myElement.hashCode() : 0;
    result = 31 * result + (myTooltip != null ? myTooltip.hashCode() : 0);
    return result;
  }
}
