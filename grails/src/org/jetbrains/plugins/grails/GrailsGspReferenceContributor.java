// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.AndFilter;
import com.intellij.psi.filters.ClassFilter;
import com.intellij.psi.filters.OrFilter;
import com.intellij.psi.filters.ScopeFilter;
import com.intellij.psi.filters.TextFilter;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttributeValue;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.reference.contributor.DefaultCodecDirectiveReferenceProvider;
import org.jetbrains.plugins.grails.references.providers.GspImportListReferenceProvider;
import org.jetbrains.plugins.grails.references.tagSupport.GspTagSupportGspReferenceProvider;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public final class GrailsGspReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar,
                                                           new String[]{"import"},
                                                           new ScopeFilter(
                                                             new ParentElementFilter(
                                                               new AndFilter(
                                                                 new OrFilter(
                                                                   new AndFilter(
                                                                     new ClassFilter(GspDirective.class),
                                                                     new TextFilter("page")
                                                                   )
                                                                 ),
                                                                 new NamespaceFilter(XmlUtil.JSP_URI)
                                                               ),
                                                               2
                                                             )
                                                           ),
                                                           new GspImportListReferenceProvider()
        );

    registrar.registerReferenceProvider(psiElement(GspDirectiveAttributeValue.class)
                                          .withParent(XmlPatterns.xmlAttribute("defaultCodec")),
                                        new DefaultCodecDirectiveReferenceProvider());

    GspTagSupportGspReferenceProvider.register(registrar);
  }
}
