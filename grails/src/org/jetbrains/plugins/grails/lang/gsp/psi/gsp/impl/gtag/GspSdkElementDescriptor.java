// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GspSdkElementDescriptor extends GspElementDescriptorBase {
  GspSdkElementDescriptor(GspNamespaceDescriptor nsDescriptor, @NotNull PsiElement place, String localName) {
    super(nsDescriptor, place, localName);
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
    Pair<Map<String,XmlAttributeDescriptor>,Set<String>> pair = GspTagLibUtil.getAttributesDescriptorsFromJavadocs(myPlace);

    Map<String, XmlAttributeDescriptor> javadocDescriptors = pair.first;

    List<XmlAttributeDescriptor> res = new ArrayList<>(javadocDescriptors.values());

    XmlAttributeDescriptor[] descriptors = GspTagDescriptorService.getInstance(getDeclaration().getProject()).getAttributesDescriptors(getName());
    for (XmlAttributeDescriptor descriptor : descriptors) {
      if (!javadocDescriptors.containsKey(descriptor.getName())) {
        res.add(descriptor);
      }
    }

    return res.toArray(XmlAttributeDescriptor.EMPTY);
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    XmlAttributeDescriptor descriptor = GspTagLibUtil.getAttributesDescriptorsFromJavadocs(myPlace).first.get(attributeName);
    if (descriptor != null) return descriptor;

    descriptor = GspTagDescriptorService.getInstance(getDeclaration().getProject()).getAttributesDescriptor(getName(), attributeName);
    if (descriptor != null) return descriptor;

    return new AnyXmlAttributeDescriptor(attributeName);
  }

}
