package com.intellij.seam.pages;

import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.annotations.NonNls;

public abstract class BasicPagesDomFileDescription<T> extends DomFileDescription<T> {
  private static final String[] PAGES_NAMESPACES =
    {SeamNamespaceConstants.PAGES_NAMESPACE, SeamNamespaceConstants.PAGES_DTD_1_1, SeamNamespaceConstants.PAGES_DTD_1_2,SeamNamespaceConstants.PAGES_DTD_2_0};

  public BasicPagesDomFileDescription(final Class<T> rootElementClass, @NonNls final String rootTagName, @NonNls final String... allPossibleRootTagNamespaces) {
    super(rootElementClass, rootTagName, allPossibleRootTagNamespaces);
  }

  @Override
  protected void initializeFileDescription() {
    registerNamespacePolicy(SeamNamespaceConstants.PAGES_NAMESPACE_KEY, PAGES_NAMESPACES);
  }
}

