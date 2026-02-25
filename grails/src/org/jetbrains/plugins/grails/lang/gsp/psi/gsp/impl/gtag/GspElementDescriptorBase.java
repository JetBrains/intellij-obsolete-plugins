// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.CommonProcessors;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

import java.util.Collection;

public abstract class GspElementDescriptorBase implements XmlElementDescriptor {
  protected final GspNamespaceDescriptor myNsDescriptor;
  protected final PsiElement myPlace;
  protected final String myLocalName;

  protected GspElementDescriptorBase(GspNamespaceDescriptor nsDescriptor, @NotNull PsiElement place, @NotNull String localName) {
    myNsDescriptor = nsDescriptor;
    myPlace = place;
    myLocalName = localName;
  }

  @Override
  public String getQualifiedName() {
    return myNsDescriptor.getPrefix() + ":" + myLocalName;
  }

  @Override
  public String getDefaultName() {
    return getQualifiedName();
  }

  @Override
  public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
    final XmlNSDescriptor descriptor = childTag.getNSDescriptor(childTag.getNamespace(), true);
    return descriptor == null ? null : descriptor.getElementDescriptor(childTag);
  }

  @Override
  public XmlNSDescriptor getNSDescriptor() {
    return myNsDescriptor;
  }

  /**
   * @return minimal occurrence constraint value (e.g. 0 or 1), on null if not applied
   */
  @Override
  public XmlElementsGroup getTopGroup() {
    return null;
  }

  @Override
  public int getContentType() {
    PsiElement element = myPlace.getNavigationElement().getNavigationElement();

    if (element instanceof GrField) {
      GrExpression initializerGroovy = ((GrField)element).getInitializerGroovy();
      if (initializerGroovy instanceof GrClosableBlock) {
        if (((GrClosableBlock)initializerGroovy).getAllParameters().length < 2) {
          return CONTENT_TYPE_EMPTY;
        }
      }
    }

    if (element instanceof PsiDocCommentOwner) {
      PsiDocComment docComment = ((PsiDocCommentOwner)element).getDocComment();
      if (docComment != null) {
        if (docComment.findTagByName("emptyTag") != null) {
          return CONTENT_TYPE_EMPTY;
        }
      }
    }

    return CONTENT_TYPE_UNKNOWN;
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public String getName(PsiElement context) {
    return getDefaultName();
  }

  @Override
  public String getName() {
    return myLocalName;
  }

  @Override
  public void init(PsiElement element) {
    throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
  }

  @Override
  public Object @NotNull [] getDependencies() {
    throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
  }

  @Override
  public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
    final CommonProcessors.CollectProcessor<XmlElementDescriptor> processor = new CommonProcessors.CollectProcessor<>();
    myNsDescriptor.processElementDescriptors(null, context == null ? myPlace : context, processor);
    final Collection<XmlElementDescriptor> results = processor.getResults();
    return results.toArray(XmlElementDescriptor.EMPTY_ARRAY);
  }

  @Override
  public PsiElement getDeclaration() {
    return myPlace;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
    return getAttributeDescriptor(attribute.getName(), null);
  }
}
