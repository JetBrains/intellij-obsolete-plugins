// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.xml.XmlChildRole;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.GspDirectiveKind;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspElementDescriptorBase;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspNamespaceDescriptor;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;

public class GspDirectiveImpl extends XmlTagImpl implements GspDirective {

  private static final XmlAttributeDescriptor[] TAGLIB_ATTRIBUTE_DESCRIPTORS_DESCRIPTORS =
    new XmlAttributeDescriptor[]{new AnyXmlAttributeDescriptor("prefix"), new AnyXmlAttributeDescriptor("uri")};

  private static final XmlAttributeDescriptor[] NOT_TAGLIB_ATTRIBUTE_DESCRIPTORS_DESCRIPTORS =
    new XmlAttributeDescriptor[]{new AnyXmlAttributeDescriptor("import"), new AnyXmlAttributeDescriptor("contentType"),
      new AnyXmlAttributeDescriptor("defaultCodec")};

  public GspDirectiveImpl() {
    super(GspTokenTypes.GSP_DIRECTIVE);
  }

  @Override
  public String toString() {
    return "GSP directive";
  }

  @Override
  public String[] knownNamespaces() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Override
  public @NotNull String getNamespace() {
    return GspTagLibUtil.DEFAULT_TAGLIB_PREFIX;
  }

  @Override
  public XmlElementDescriptor getDescriptor() {
    return new GspElementDescriptorBase(GspNamespaceDescriptor.getDefaultNsDescriptor(this), this, getLocalName()) {

      @Override
      public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
        return null;
      }

      @Override
      public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
        return EMPTY_ARRAY;
      }

      @Override
      public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
        if (GspDirectiveKind.TAGLIB.isInstance(GspDirectiveImpl.this)) {
          return TAGLIB_ATTRIBUTE_DESCRIPTORS_DESCRIPTORS;
        }

        return NOT_TAGLIB_ATTRIBUTE_DESCRIPTORS_DESCRIPTORS;
      }

      @Override
      public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
        return new AnyXmlAttributeDescriptor(attributeName);
      }
    };
  }

  @Override
  public boolean addOrReplaceAttribute(@NotNull GspDirectiveAttribute attribute) {
    // only if not exists
    GspDirectiveAttribute oldAttribute = (GspDirectiveAttribute) getAttribute(attribute.getName());
    if (oldAttribute != null) {
      ASTNode oldChild = oldAttribute.getNode();
      ASTNode newChild = attribute.getNode();
      assert oldChild != null && newChild != null;
      replaceChild(oldChild, newChild);
      return true;
    }
    ASTNode startTagName = XmlChildRole.START_TAG_NAME_FINDER.findChild(this);
    if (startTagName == null) return false;
    PsiElement element = startTagName.getPsi();
    assert element != null;
    try {
      addAfter(attribute, element);
    } catch (IncorrectOperationException e) {
      return false;
    }
    return true;
  }
}
