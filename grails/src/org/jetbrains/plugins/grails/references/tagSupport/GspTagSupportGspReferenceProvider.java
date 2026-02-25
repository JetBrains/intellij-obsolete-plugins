// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.jetbrains.plugins.grails.references.common.XmlGspTagWrapper;
import org.jetbrains.plugins.grails.util.GrailsPatterns;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.patterns.XmlPatterns.xmlAttribute;

public class GspTagSupportGspReferenceProvider extends PsiReferenceProvider {

  private static final Map<String, TagAttributeReferenceProvider[]> PROVIDERS_MAP = new HashMap<>();

  public static final TagAttributeReferenceProvider[] PROVIDERS;

  static {
    PROVIDERS = new TagAttributeReferenceProvider[]{
      new GspFieldValueTagSupport(),
      new GspControllerAttributeSupport(),
      new GspActionAttributeSupport(),
      new GspTemplateAttributeSupport(),
      new GspContextPathAttributeSupport(),
      new GspMappingAttributeSupport(),
      new CodecAttributeSupport("encodeAs", "message"),
      new CodecAttributeSupport("codec", "encodeAs"),
      new GspPluginAttributeSupport("g"),
      new GspPluginAttributeSupport("r"), // 'resources' plugin namespace.
      new GspSelectTagSupport("optionKey"),
      new GspSelectTagSupport("optionValue"),
      new GspMetaTagSupport(),
      new GspResourceDirAttributeSupport(),
      new GspResourceContextPathAttributeSupport(),
      new GspResourceFileAttributeSupport(),
      new GspIncludeViewAttributeSupport(),
      new GspSrcJsAttributeSupport(),
      new GspRResourceModuleAttrSupport("module", "require", "use"),
      new GspRResourceModuleAttrSupport("name", "renderModule"),
      new GspRResourceRequireTagModulesAttrSupport(),
      new GspRResourceUriAttributeSupport(),
      new GspUpdateAttributeSupport(),
      new GspApplyLayoutNameAttributeSupport()
    };

    MultiMap<String, TagAttributeReferenceProvider> res = new MultiMap<>();

    for (TagAttributeReferenceProvider provider : PROVIDERS) {
      if (provider.getTagNames() != null) {
        res.putValue(provider.getAttributeName(), provider);
      }
    }

    for (TagAttributeReferenceProvider provider : PROVIDERS) {
      if (provider.getTagNames() == null) {
        res.putValue(provider.getAttributeName(), provider);
      }
    }

    for (Map.Entry<String, Collection<TagAttributeReferenceProvider>> entry : res.entrySet()) {
      Collection<TagAttributeReferenceProvider> providers = entry.getValue();
      PROVIDERS_MAP.put(entry.getKey(), providers.toArray(new TagAttributeReferenceProvider[0]));
    }
  }

  public static void register(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(GrailsPatterns.gspAttributeValue(xmlAttribute().withName(
          StandardPatterns.string().oneOf(PROVIDERS_MAP.keySet()))), new GspTagSupportGspReferenceProvider());
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    XmlAttributeValue fieldAttr = (XmlAttributeValue)element;
    if (!GrailsPsiUtil.isSimpleAttribute(fieldAttr)) return PsiReference.EMPTY_ARRAY;

    GspAttribute attribute = (GspAttribute)element.getParent();

    XmlTag tag = attribute.getParent();
    if (tag == null) return PsiReference.EMPTY_ARRAY;

    String prefix = tag.getNamespacePrefix();

    TagAttributeReferenceProvider[] providers = PROVIDERS_MAP.get(attribute.getName());
    if (providers != null) {
      for (TagAttributeReferenceProvider provider : providers) {
        String providerPrefix = provider.getPrefix();
        if (providerPrefix != null && !prefix.equals(providerPrefix)) continue;

        String[] names = provider.getTagNames();

        if (names == null || ArrayUtil.contains(tag.getLocalName(), names)) {
          return provider.getReferencesByElement(element, fieldAttr.getValue(), 1, new XmlGspTagWrapper(tag));
        }
      }
    }

    return PsiReference.EMPTY_ARRAY;
  }
}
