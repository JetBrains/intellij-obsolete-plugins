package com.intellij.struts.highlighting.syntax;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.facet.StrutsFacet;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for DOM-based {@link com.intellij.lang.annotation.Annotator} components.
 *
 * @param <T> Root-DomElement.
 * @author Yann C&eacute;bron
 */
abstract class DomAnnotatorComponentBase<T extends DomElement> implements Annotator {

  private final Class<? extends T> domClass;

  protected DomAnnotatorComponentBase(final Class<? extends T> domClass) {
    this.domClass = domClass;
  }

  @Override
  public final void annotate(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder) {
    if (!(psiElement instanceof XmlTag)) {
      return;
    }

    if (!StrutsFacet.isPresentForContainingWebFacet(psiElement)) {
      return;
    }

    final DomManager domManager = DomManager.getDomManager(psiElement.getProject());

    if (domManager.getFileElement((XmlFile)psiElement.getContainingFile(), domClass) == null) {
      return;
    }

    final DomElement domElement = domManager.getDomElement(((XmlTag)psiElement));

    if (domElement == null) {
      return;
    }

    domElement.acceptChildren(buildVisitor(holder));
  }


  /**
   * Implementing classes must return an instance of {@link DomAnnotatorVisitor} which will be called from
   * {@link #annotate(com.intellij.psi.PsiElement, com.intellij.lang.annotation.AnnotationHolder)} if the current file and root element matches.
   *
   * @param holder For direct use in visitor.
   * @return Specialized visitor to perform the actual annotation.
   */
  protected abstract DomAnnotatorVisitor buildVisitor(final AnnotationHolder holder);
}
