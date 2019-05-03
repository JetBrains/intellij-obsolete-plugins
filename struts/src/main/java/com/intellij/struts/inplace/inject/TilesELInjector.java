/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.inject;

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.jsp.el.ELLanguage;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.dom.tiles.Add;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.dom.tiles.Put;
import com.intellij.struts.dom.tiles.TilesDefinitions;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yann C&eacute;bron
 */
public class TilesELInjector implements MultiHostInjector {

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    final PsiFile containingFile = context.getContainingFile();
    if (!JamCommonUtil.isPlainXmlFile(containingFile)) {
      return;
    }

    XmlFile xmlFile = (XmlFile)containingFile;
    if (DomManager.getDomManager(xmlFile.getProject()).getFileElement(xmlFile, TilesDefinitions.class) == null) {
      return;
    }

    final String value = ((XmlAttributeValue)context).getValue();
    if (value == null ||
        !StringUtil.startsWith(value, "${")) {
      return;
    }

    final PsiElement parent = context.getParent();
    if (!(parent instanceof XmlAttribute)) {
      return;
    }

    final XmlAttribute attribute = (XmlAttribute)parent;
    final String attributeName = attribute.getLocalName();
    if (!attributeName.equals("expression") &&
        !attributeName.equals("templateExpression")) {
      return;
    }

    DomElement domElement = DomManager.getDomManager(context.getProject()).getDomElement(attribute.getParent());
    if (domElement instanceof Definition ||
        domElement instanceof Put ||
        domElement instanceof Add) {
      registrar.startInjecting(ELLanguage.INSTANCE)
        .addPlace(null, null, (PsiLanguageInjectionHost)context, ElementManipulators.getValueTextRange(context))
        .doneInjecting();
    }
  }

  @NotNull
  @Override
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Arrays.asList(XmlAttributeValue.class);
  }
}
