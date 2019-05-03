/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ElementPresentationManager;
import com.intellij.util.xml.highlighting.ResolvingElementQuickFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 */
public abstract class XmlValueReference implements PsiReference, LocalQuickFixProvider, EmptyResolveMessageProvider {

  protected final XmlAttributeValue myValue;
  private final BaseReferenceProvider myProvider;

  public void setSoft(boolean soft) {
    mySoft = soft;
  }

  protected boolean mySoft;

  public void setRange(final TextRange range) {
    myRange = range;
  }

  private TextRange myRange;

  /**
   * may affect getRangeInElement() and getUnresolvedMessage()
   */
  protected int errorType;

  private static final int ERROR_NO = 0;
  private static final int ERROR_EMPTY = 1;
  private static final int ERROR_DEFAULT = 2;

  public XmlValueReference(XmlAttributeValue attribute, BaseReferenceProvider provider) {
    this(attribute, provider, null);
  }


  public XmlValueReference(XmlAttributeValue attribute, BaseReferenceProvider provider, TextRange range) {
    myRange = range == null ? new TextRange(1, attribute.getValue().length() + 1) : range;
    myValue = attribute;
    myProvider = provider;
    mySoft = provider.isSoft();
  }

  @NotNull
  public Project getProject() {
    return myValue.getProject();
  }

  @Nullable
  public WebFacet getWebFacet() {
    return WebUtil.getWebFacet(myValue);
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    return ElementManipulators.getManipulator(myValue).handleContentChange(myValue, getRangeInElement(), newElementName);
  }

  @Nullable
  protected static Object[] getItems(Collection<? extends DomElement> elements) {
    if (elements == null) {
      return null;
    }
    return ElementPresentationManager.getInstance().createVariants(elements, Iconable.ICON_FLAG_VISIBILITY);
  }

  @NotNull
  public String getValue() {
    String s = myValue.getValue();
    if (myRange == null) {
      return s;
    }
    else {
      return s.substring(myRange.getStartOffset() - 1, myRange.getEndOffset() - 1);
    }
  }

  @Override
  @NotNull
  public String getCanonicalText() {
    return myProvider.getCanonicalName() + " " + getValue();
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement psiElement) throws IncorrectOperationException {
    return null;
  }

  @Override
  @NotNull
  public PsiElement getElement() {
    return myValue;
  }

  @Override
  @NotNull
  public Object[] getVariants() {
    Object[] objects = doGetVariants();
    return objects == null ? EMPTY_ARRAY : objects;
  }

  @Override
  @NotNull
  public TextRange getRangeInElement() {
    if (errorType == ERROR_EMPTY || myRange == null) {
      return new TextRange(1, getValue().length() + 1);
    }
    return myRange;
  }

  /**
   * Base resolver
   *
   * @return null if the attribute is empty
   */
  @Override
  public PsiElement resolve() {
    if (myValue.getValue().trim().isEmpty()) {
      errorType = ERROR_EMPTY;
      return null;
    }
    errorType = ERROR_NO;
    PsiElement result = doResolve();
    if (result == null && errorType == ERROR_NO) {
      errorType = ERROR_DEFAULT;
    }
    return result;
  }

  @Nullable
  protected abstract PsiElement doResolve();

  @Nullable
  protected abstract Object[] doGetVariants();

  @Override
  public boolean isReferenceTo(@NotNull PsiElement psielement) {
    return psielement.getManager().areElementsEquivalent(psielement, resolve());
  }

  @Override
  public boolean isSoft() {
    return mySoft;
  }

  @Override
  @NotNull
  public String getUnresolvedMessagePattern() {
    if (errorType == ERROR_EMPTY) {
      return "Wrong attribute value";
    }
    return "Cannot resolve " + getCanonicalText();
  }

  @Nullable
  protected ResolvingElementQuickFix createResolvingFix(final DomElement scope) {
    final Class<? extends DomElement> domClass = myProvider.getDomClass();
    if (domClass != null) {
      final String text = getValue().trim();
      if (!text.isEmpty() && scope != null) {
        return ResolvingElementQuickFix.createFix(text, domClass, scope);
      }
    }
    return null;
  }

  @Override
  public LocalQuickFix[] getQuickFixes() {
    final ResolvingElementQuickFix quickFix = createResolvingFix(getScope());
    return quickFix == null ? LocalQuickFix.EMPTY_ARRAY : new LocalQuickFix[] { quickFix };
  }

  @Nullable
  protected DomElement getScope() {
    return null;
  }
}
