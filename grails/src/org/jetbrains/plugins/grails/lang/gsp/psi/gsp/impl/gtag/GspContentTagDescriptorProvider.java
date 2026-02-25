// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.BasicXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.impl.GspHtmlFileImpl;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;

public final class GspContentTagDescriptorProvider implements XmlElementDescriptorProvider {

  private static final String CONTENT = "content";

  @Override
  public XmlElementDescriptor getDescriptor(final XmlTag tag) {
    if (!CONTENT.equals(tag.getName())) return null;

    PsiFile file = tag.getContainingFile();

    if (!(file instanceof GspHtmlFileImpl)) return null;

    final TagLibNamespaceDescriptor sitemeshContentTagDescriptor = GspTagLibUtil.getTagLibClasses(file, "sitemesh");
    if (sitemeshContentTagDescriptor == null) return null;

    final XmlDocument parentOfType = PsiTreeUtil.getParentOfType(tag, XmlDocument.class);
    if (parentOfType == null) return null;

    final XmlNSDescriptor nsDescriptor = parentOfType.getDefaultNSDescriptor(tag.getNamespace(), false);

    final PsiElement field = sitemeshContentTagDescriptor.getTag("captureContent");
    if (field == null) return null;

    return new XmlElementDescriptor() {

      @Override
      public String getQualifiedName() {
        return CONTENT;
      }

      @Override
      public String getDefaultName() {
        return CONTENT;
      }

      @Override
      public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
        XmlDocumentImpl xmlDocument = PsiTreeUtil.getParentOfType(context, XmlDocumentImpl.class);
        if (xmlDocument == null) return EMPTY_ARRAY;
        return xmlDocument.getRootTagNSDescriptor().getRootElementsDescriptors(xmlDocument);
      }

      @Override
      public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
        XmlTag parent = contextTag.getParentTag();
        if (parent == null) return null;
        final XmlNSDescriptor descriptor = parent.getNSDescriptor(childTag.getNamespace(), true);
        return descriptor == null ? null : descriptor.getElementDescriptor(childTag);
      }

      @Override
      public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
        return new XmlAttributeDescriptor[]{TagAttributeDescriptor.INSTANCE};
      }

      @Override
      public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
        if (!"tag".equals(attributeName)) return null;

        return TagAttributeDescriptor.INSTANCE;
      }

      @Override
      public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
        return getAttributeDescriptor(attribute.getName(), null);
      }

      @Override
      public XmlNSDescriptor getNSDescriptor() {
        return nsDescriptor;
      }

      @Override
      public XmlElementsGroup getTopGroup() {
        return null;
      }

      @Override
      public int getContentType() {
        return 0;
      }

      @Override
      public String getDefaultValue() {
        return null;
      }

      @Override
      public PsiElement getDeclaration() {
        return field;
      }

      @Override
      public String getName(PsiElement context) {
        return CONTENT;
      }

      @Override
      public String getName() {
        return CONTENT;
      }

      @Override
      public void init(PsiElement element) {
        throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
      }

      @Override
      public Object @NotNull [] getDependencies() {
        throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
      }
    };
  }

  private static class TagAttributeDescriptor extends BasicXmlAttributeDescriptor {

    private static final TagAttributeDescriptor INSTANCE = new TagAttributeDescriptor();

    @Override
    public boolean isRequired() {
      return true;
    }

    @Override
    public boolean isFixed() {
      return false;
    }

    @Override
    public boolean hasIdType() {
      return false;
    }

    @Override
    public boolean hasIdRefType() {
      return false;
    }

    @Override
    public String getDefaultValue() {
      return null;
    }

    @Override
    public boolean isEnumerated() {
      return false;
    }

    @Override
    public String[] getEnumeratedValues() {
      return null;
    }

    @Override
    public PsiElement getDeclaration() {
      return null;
    }

    @Override
    public String getName() {
      return "tag";
    }

    @Override
    public void init(PsiElement element) {

    }
  }
}
