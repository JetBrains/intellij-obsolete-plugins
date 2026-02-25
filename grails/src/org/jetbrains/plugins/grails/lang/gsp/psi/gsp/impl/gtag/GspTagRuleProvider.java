// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.util.TagSetRuleProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;

public final class GspTagRuleProvider extends TagSetRuleProvider {

  @Override
  protected String getNamespace(@NotNull XmlTag tag) {
    return tag instanceof GspGrailsTag && tag.getName().startsWith("g:") ? "g" : null;
  }

  @Override
  protected void initMap(TagsRuleMap map, @NotNull String version) {
    map.add("if", requireAttr("test", "env"));
    map.add("elseif", requireAttr("test", "env"));
    map.add("include", shouldHaveParams());
    map.add("applyLayout", shouldHaveParams(), unusedIfPresent("url", "view", "template"));
    map.add("render", shouldHaveParams(), unusedIfPresent("plugin", "contextPath"));

    map.add("resource", shouldHaveParams(), unusedIfPresent("plugin", "contextPath"), unusedIfPresent("base", "absolute"));
    map.put("createLinkTo", map.get("resource"));

    map.add("link", unusedIfPresent("base", "absolute"));
    map.add("createLink", unusedAllIfPresent("uri", "base", "absolute"), unusedIfPresent("base", "absolute"));

    map.add("sortableColumn", requireAttr("title", "titleKey"));
  }

}

