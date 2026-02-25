// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.ArrayList;
import java.util.List;

public class GspTmplNamespaceDescriptor implements XmlNSDescriptor, DumbAware {

  public static final String NAMESPACE_TMPL = "tmpl";

  private final PsiDirectory myDirectory;

  public GspTmplNamespaceDescriptor(@NotNull GspXmlRootTag tag) {
    PsiFile containingFile = tag.getContainingFile();
    if (containingFile != null) {
      myDirectory = containingFile.getOriginalFile().getContainingDirectory();
    }
    else {
      myDirectory = null;
    }
  }

  @Override
  public @Nullable XmlElementDescriptor getElementDescriptor(@NotNull XmlTag tag) {
    String tagName = tag.getName();
    if (tagName.equals("tmpl:") || tagName.equals("tmpl:/")) return null;
    return new AnyXmlElementDescriptor(null, this);
  }

  @Override
  public XmlElementDescriptor @NotNull [] getRootElementsDescriptors(@Nullable XmlDocument document) {
    if (myDirectory == null) return XmlElementDescriptor.EMPTY_ARRAY;

    List<XmlElementDescriptor> res = new ArrayList<>();

    for (PsiFile psiFile : myDirectory.getFiles()) {
      if (psiFile instanceof GspFile) {
        String templateName = GrailsUtils.getTemplateName(psiFile.getName());
        if (templateName != null) {
          res.add(new TmplElementDescriptor(templateName));
        }
      }
    }

    return res.toArray(XmlElementDescriptor.EMPTY_ARRAY);
  }

  @Override
  public XmlFile getDescriptorFile() {
    return null;
  }

  @Override
  public @Nullable PsiElement getDeclaration() {
    return null;
  }

  @Override
  public String getName(PsiElement context) {
    return NAMESPACE_TMPL;
  }

  @Override
  public String getName() {
    return NAMESPACE_TMPL;
  }

  @Override
  public void init(PsiElement element) {
    throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
  }

  @Override
  public Object @NotNull [] getDependencies() {
    throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
  }

  private final class TmplElementDescriptor implements XmlElementDescriptor {

    private final String name;
    private final String localName;

    private TmplElementDescriptor(String localName) {
      name = NAMESPACE_TMPL + ':' + localName;
      this.localName = localName;
    }

    @Override
    public String getQualifiedName() {
      return name;
    }

    @Override
    public String getDefaultName() {
      return name;
    }

    @Override
    public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
      return EMPTY_ARRAY;
    }

    @Override
    public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
      return null;
    }

    @Override
    public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
      return XmlAttributeDescriptor.EMPTY;
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
      return new AnyXmlAttributeDescriptor(attributeName);
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
      return getAttributeDescriptor(attribute.getName(), null);
    }

    @Override
    public XmlNSDescriptor getNSDescriptor() {
      return GspTmplNamespaceDescriptor.this;
    }

    @Override
    public XmlElementsGroup getTopGroup() {
      return null;
    }

    @Override
    public int getContentType() {
      return CONTENT_TYPE_UNKNOWN;
    }

    @Override
    public String getDefaultValue() {
      return null;
    }

    @Override
    public PsiElement getDeclaration() {
      return null;
    }

    @Override
    public String getName(PsiElement context) {
      return name;
    }

    @Override
    public String getName() {
      return localName;
    }

    @Override
    public void init(PsiElement element) {
      throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
    }

    @Override
    public Object @NotNull [] getDependencies() {
      throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
    }
  }

}
