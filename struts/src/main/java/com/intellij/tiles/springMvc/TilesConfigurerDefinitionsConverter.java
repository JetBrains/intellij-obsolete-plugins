/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.tiles.springMvc;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlFile;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.values.converters.resources.ResourceValueConverter;
import com.intellij.spring.model.values.converters.resources.SpringResourceTypeProvider;
import com.intellij.spring.model.xml.mvc.TilesConfigurer;
import com.intellij.spring.web.SpringWebConstants;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.values.GenericDomValueConvertersRegistry;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public class TilesConfigurerDefinitionsConverter extends ResourceValueConverter
  implements GenericDomValueConvertersRegistry.Provider,
    Condition<Pair<PsiType, GenericDomValue>> {

  @NonNls
  static final String DEFINITIONS_ATTRIBUTE = "definitions";

  @Override
  public Condition<Pair<PsiType, GenericDomValue>> getCondition() {
    return this;
  }

  @Override
  public Converter getConverter() {
    return this;
  }

  @Override
  public boolean value(Pair<PsiType, GenericDomValue> pair) {
    final GenericDomValue genericDomValue = pair.getSecond();
    return isRelevantProperty(genericDomValue);
  }

  private static boolean isRelevantProperty(GenericDomValue genericDomValue) {
    // <mvc:tiles-configurer>
    if ("location".equals(genericDomValue.getXmlElementName()) &&
        genericDomValue.getParentOfType(TilesConfigurer.class, true) != null) {
      return true;
    }

    // <bean>
    return SpringPropertyUtils.isSpecificProperty(genericDomValue,
                                                  DEFINITIONS_ATTRIBUTE,
                                                  SpringWebConstants.TILES_CONFIGURER_CLASSES);
  }


  static final Condition<PsiFileSystemItem> TILES_XML_CONDITION = item -> {
    if (item.isDirectory()) return true;

    final VirtualFile file = item.getVirtualFile();
    if (file == null) {
      return false;
    }

    final PsiFile psiFile = item.getManager().findFile(file);
    if (!(psiFile instanceof XmlFile)) {
      return false;
    }

    XmlFile xmlFile = (XmlFile)psiFile;
    return StrutsProjectComponent.getInstance(item.getProject()).getTilesFactory().getDomRoot(xmlFile) != null;
  };

  @Override
  protected boolean isEndingSlashNotAllowed() {
    return true;
  }

  public static class ResourceTypeProvider implements SpringResourceTypeProvider {

    @Nullable
    @Override
    public Condition<PsiFileSystemItem> getResourceFilter(@NotNull GenericDomValue genericDomValue) {
      return isRelevantProperty(genericDomValue) ? TILES_XML_CONDITION : null;
    }
  }
}