/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods for installing ReferenceProviders in XML context.
 *
 * @author Dmitry Avdeev
 */
public final class ReferenceProviderUtils {

  public final static ElementFilter TAG_CLASS_FILTER = XmlTagFilter.INSTANCE;

  /**
   * Register the given provider on the given XmlAttribute(s)/Namespace/XmlTag combination.
   *
   * @param psiReferenceRegistrar Registrar instance.
   * @param provider              Provider to install.
   * @param tagName               Tag name.
   * @param namespaceFilter       Namespace for tag.
   * @param attributeNames        Attribute name(s).
   */
  public static void registerAttributes(final PsiReferenceRegistrar psiReferenceRegistrar,
                                        final PsiReferenceProvider provider,
                                        final @NonNls String tagName,
                                        final NamespaceFilter namespaceFilter,
                                        final @NonNls String... attributeNames) {
    XmlUtil.registerXmlAttributeValueReferenceProvider(psiReferenceRegistrar, attributeNames,
      andTagNames(namespaceFilter, tagName), provider);
  }

  /**
   * Register the given provider on the given XmlAttribute/Namespace/XmlTag(s) combination.
   *
   * @param psiReferenceRegistrar Registrar instance.
   * @param provider              Provider to install.
   * @param attributeName         Attribute name.
   * @param namespaceFilter       Namespace for tag(s).
   * @param tagNames              Tag name(s).
   */
  public static void registerTags(final PsiReferenceRegistrar psiReferenceRegistrar,
                                  final PsiReferenceProvider provider,
                                  final @NonNls String attributeName,
                                  final NamespaceFilter namespaceFilter,
                                  final @NonNls String... tagNames) {
    XmlUtil.registerXmlAttributeValueReferenceProvider(psiReferenceRegistrar, new String[]{attributeName},
      andTagNames(namespaceFilter, tagNames), provider);
  }

  /**
   * Registers a class provider with no restrictions on the given XmlAttribute/Namespace/XmlTag combination.
   *
   * @param psiReferenceRegistrar Registrar instance.
   * @param namespaceFilter       Namespace for tag.
   * @param tagName               Tag name.
   * @param attributeName         Attribute name.
   */
  public static void registerSubclass(final PsiReferenceRegistrar psiReferenceRegistrar,
                                      final NamespaceFilter namespaceFilter,
                                      final @NonNls String tagName,
                                      final @NonNls String attributeName) {
    registerTags(psiReferenceRegistrar, new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement,
                                                   @NotNull final ProcessingContext processingContext) {
        final JavaClassReferenceProvider provider = new JavaClassReferenceProvider();
        return provider.getReferencesByElement(psiElement, processingContext);
      }
    }, attributeName, namespaceFilter, tagName);
  }

  /**
   * Registers a class provider allowing only the given subclass(es) on the given XmlAttribute/Namespace/XmlTag combination.
   *
   * @param psiReferenceRegistrar Registrar instance.
   * @param namespaceFilter       Namespace for tag.
   * @param tagName               Tag name.
   * @param attributeName         Attribute name.
   * @param classes               Sub-class(es) to allow.
   */
  public static void registerSubclass(final PsiReferenceRegistrar psiReferenceRegistrar,
                                      final NamespaceFilter namespaceFilter,
                                      final @NonNls String tagName,
                                      final @NonNls String attributeName,
                                      final @NonNls String... classes) {
    registerTags(psiReferenceRegistrar, new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement,
                                                   @NotNull final ProcessingContext processingContext) {
        final JavaClassReferenceProvider provider = new JavaClassReferenceProvider();
        provider.setOption(JavaClassReferenceProvider.EXTEND_CLASS_NAMES, classes);
        provider.setOption(JavaClassReferenceProvider.INSTANTIATABLE, Boolean.TRUE);
        return provider.getReferencesByElement(psiElement, processingContext);
      }
    }, attributeName, namespaceFilter, tagName);
  }

  private static ScopeFilter andTagNames(final ElementFilter namespace, final String... tagNames) {
    return new ScopeFilter(new ParentElementFilter(new AndFilter(namespace, TAG_CLASS_FILTER, new TextFilter(tagNames)), 2));
  }
  
}
