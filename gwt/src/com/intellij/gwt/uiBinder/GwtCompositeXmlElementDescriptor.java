package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementDescriptorAwareAboutChildren;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.impl.schema.ComplexTypeDescriptor;
import com.intellij.xml.impl.schema.TypeDescriptor;
import com.intellij.xml.impl.schema.XmlElementDescriptorImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GwtCompositeXmlElementDescriptor implements XmlElementDescriptor, XmlElementDescriptorAwareAboutChildren {
  private final GwtUiComponentDescriptor myMainDescriptor;
  private final XmlElementDescriptor myAdditionalDescriptor;

  public GwtCompositeXmlElementDescriptor(@NotNull GwtUiComponentDescriptor mainDescriptor, @NotNull XmlElementDescriptor additionalDescriptor) {
    myMainDescriptor = mainDescriptor;
    myAdditionalDescriptor = additionalDescriptor;
  }

  @Override
  public PsiElement getDeclaration() {
    return myMainDescriptor.getDeclaration();
  }

  @Override
  public String getName() {
    return myMainDescriptor.getName();
  }

  @Override
  public String getQualifiedName() {
    return myMainDescriptor.getQualifiedName();
  }

  @Override
  public String getDefaultName() {
    return myMainDescriptor.getDefaultName();
  }

  @Override
  public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
    boolean addAllComponents = false;
    if (myAdditionalDescriptor instanceof XmlElementDescriptorImpl) {
      final TypeDescriptor type = ((XmlElementDescriptorImpl)myAdditionalDescriptor).getType(context);
      addAllComponents = type instanceof ComplexTypeDescriptor && ((ComplexTypeDescriptor)type).hasAnyInContentModel();
    }

    final XmlElementDescriptor[] main = addAllComponents ? myMainDescriptor.getComponentDescriptors(context) : EMPTY_ARRAY;
    final XmlElementDescriptor[] additional = myAdditionalDescriptor.getElementsDescriptors(context);
    if (main.length == 0) {
      return additional;
    }
    if (additional.length == 0) {
      return main;
    }

    List<XmlElementDescriptor> descriptors = new ArrayList<>();
    final Set<String> names = new HashSet<>();
    for (XmlElementDescriptor descriptor : main) {
      descriptors.add(descriptor);
      names.add(descriptor.getName());
    }
    for (XmlElementDescriptor descriptor : additional) {
      if (!names.contains(descriptor.getName())) {
        descriptors.add(descriptor);
      }
    }
    return descriptors.toArray(XmlElementDescriptor.EMPTY_ARRAY);
  }

  @Override
  public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
    final XmlElementDescriptor descriptor = myMainDescriptor.getElementDescriptor(childTag, contextTag);
    return descriptor != null && !(descriptor instanceof AnyXmlElementDescriptor) ? descriptor : myAdditionalDescriptor.getElementDescriptor(childTag, contextTag);
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
    return mergeDescriptors(myMainDescriptor.getAttributesDescriptors(context), myAdditionalDescriptor.getAttributesDescriptors(context));
  }

  private static XmlAttributeDescriptor[] mergeDescriptors(XmlAttributeDescriptor[] main, XmlAttributeDescriptor[] additional) {
    if (main.length == 0) {
      return additional;
    }
    if (additional.length == 0) {
      return main;
    }

    List<XmlAttributeDescriptor> descriptors = new ArrayList<>();
    Set<String> names = new HashSet<>();
    for (XmlAttributeDescriptor descriptor : main) {
      descriptors.add(descriptor);
      names.add(descriptor.getName());
    }
    for (XmlAttributeDescriptor descriptor : additional) {
      if (!names.contains(descriptor.getName())) {
        descriptors.add(descriptor);
      }
    }
    return descriptors.toArray(XmlAttributeDescriptor.EMPTY);
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    final XmlAttributeDescriptor descriptor = myMainDescriptor.getAttributeDescriptor(attributeName, context);
    return descriptor != null ? descriptor : myAdditionalDescriptor.getAttributeDescriptor(attributeName, context);
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
    final XmlAttributeDescriptor descriptor = myMainDescriptor.getAttributeDescriptor(attribute);
    return descriptor != null ? descriptor : myAdditionalDescriptor.getAttributeDescriptor(attribute);
  }

  @Override
  public boolean allowElementsFromNamespace(String namespace, XmlTag context) {
    return myMainDescriptor.allowElementsFromNamespace(namespace, context)
           || myAdditionalDescriptor instanceof XmlElementDescriptorAwareAboutChildren
              && ((XmlElementDescriptorAwareAboutChildren)myAdditionalDescriptor).allowElementsFromNamespace(namespace, context);
  }

  @Override
  public XmlNSDescriptor getNSDescriptor() {
    return myMainDescriptor.getNSDescriptor();
  }

  @Override
  public XmlElementsGroup getTopGroup() {
    return null;
  }

  @Override
  public int getContentType() {
    return myMainDescriptor.getContentType();
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public String getName(PsiElement context) {
    return myMainDescriptor.getName(context);
  }

  @Override
  public void init(PsiElement element) {
    myAdditionalDescriptor.init(element);
  }

  @Override
  public Object @NotNull [] getDependencies() {
    return myAdditionalDescriptor.getDependencies();
  }
}
