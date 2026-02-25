// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.configSlurper.ConfigSlurperSupport;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.util.List;

public abstract class AbstractConfigSupport extends ConfigSlurperSupport implements ConfigSlurperSupport.PropertiesProvider {

  private String[] myFinalProperties;
  private String[] myPrefixes;

  protected abstract String @NotNull [] getFinalProperties();

  protected String @NotNull [] getPrefixes() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Override
  public abstract PropertiesProvider getProvider(@NotNull GroovyFile file);

  @Override
  public void collectVariants(@NotNull List<String> prefix, @NotNull PairConsumer<String, Boolean> consumer) {
    String sPrefix = StringUtil.join(prefix, ".");

    int prefixLength = sPrefix.length();
    if (prefixLength > 0) {
      prefixLength++;
    }

    if (myFinalProperties == null) myFinalProperties = getFinalProperties();

    for (String property : myFinalProperties) {
      if (property.startsWith(sPrefix) && prefixLength < property.length()) {
        String suffix = property.substring(prefixLength);
        consumer.consume(suffix, true);
      }
    }

    if (myPrefixes == null) myPrefixes = getPrefixes();

    for (String property : myPrefixes) {
      if (property.startsWith(sPrefix) && prefixLength < property.length()) {
        String suffix = property.substring(prefixLength);
        consumer.consume(suffix, false);
      }
    }
  }
}
