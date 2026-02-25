// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.CommonProcessors;
import com.intellij.util.PairProcessor;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;

import java.util.Collection;

public class GspNamespaceDescriptor implements XmlNSDescriptor, DumbAware {
  private final TagLibNamespaceDescriptor myDescriptor;

  public GspNamespaceDescriptor(TagLibNamespaceDescriptor descriptor) {
    myDescriptor = descriptor;
  }

  @Override
  public @Nullable XmlElementDescriptor getElementDescriptor(@NotNull XmlTag tag) {
    CommonProcessors.FindFirstProcessor<XmlElementDescriptor> descriptorProcessor = new CommonProcessors.FindFirstProcessor<>();
    processElementDescriptors(tag.getLocalName(), tag, descriptorProcessor);
    final XmlElementDescriptor found = descriptorProcessor.getFoundValue();
    if (found != null) {
      return found;
    }
    final XmlTag parentTag = tag.getParentTag();
    if (parentTag != null) {
      return new AnyXmlElementDescriptor(parentTag.getDescriptor(), this);
    }
    return null;
  }

  public void processElementDescriptors(final @Nullable String tagName, final @NotNull PsiElement place, final Processor<? super XmlElementDescriptor> processor) {
    Collection<PsiMethod> fields;
    if (tagName == null) {
      fields = myDescriptor.getAllTags();
    }
    else {
      PsiMethod variable = myDescriptor.getTag(tagName);
      fields = ContainerUtil.createMaybeSingletonList(variable);
    }

    for (PsiMethod method : fields) {
      PsiMember navigationElement = (PsiMember)method.getNavigationElement();
      String name = method.getName();

      PsiClass aClass = navigationElement.getContainingClass();
      assert aClass != null;

      XmlElementDescriptor descriptor = GspTagLibUtil.isSdkTagLib(aClass)
                                        ? new GspSdkElementDescriptor(this, method, name)
                                        : new GspPropertyElementDescriptor(this, method, name);

      if (!processor.process(descriptor)) return;
    }

    if (GspTagLibUtil.DEFAULT_TAGLIB_PREFIX.equals(myDescriptor.getNamespacePrefix())) {
      GspTagLibUtil.processBuiltInTagClasses(tagName, place, builtInTagProcessor(processor));
    }
  }

  private @NotNull PairProcessor<String, PsiClass> builtInTagProcessor(@NotNull Processor<? super XmlElementDescriptor> processor) {
    return (tagName, psiClass) -> processor.process(new GspSdkElementDescriptor(this, psiClass, tagName));
  }

  @Override
  public XmlElementDescriptor @NotNull [] getRootElementsDescriptors(@Nullable XmlDocument document) {
    if (document == null) {
      return XmlElementDescriptor.EMPTY_ARRAY;
    }

    final CommonProcessors.CollectProcessor<XmlElementDescriptor> processor = new CommonProcessors.CollectProcessor<>();
    processElementDescriptors(null, document, processor);
    final Collection<XmlElementDescriptor> results = processor.getResults();
    return results.toArray(XmlElementDescriptor.EMPTY_ARRAY);
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
    return myDescriptor.getNamespacePrefix();
  }

  @Override
  public String getName() {
    return myDescriptor.getNamespacePrefix();
  }

  @Override
  public void init(PsiElement element) {
    throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
  }

  @Override
  public Object @NotNull [] getDependencies() {
    throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
  }

  public String getPrefix() {
    return myDescriptor.getNamespacePrefix();
  }

  public static GspNamespaceDescriptor getDefaultNsDescriptor(final XmlTag tag) {
    return new GspNamespaceDescriptor(GspTagLibUtil.getTagLibClasses(tag, GspTagLibUtil.DEFAULT_TAGLIB_PREFIX));
  }
}
