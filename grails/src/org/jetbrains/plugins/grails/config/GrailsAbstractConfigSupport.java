// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.List;

public abstract class GrailsAbstractConfigSupport extends AbstractConfigSupport {

  @Override
  public void collectVariants(@NotNull List<String> prefix, @NotNull PairConsumer<String, Boolean> consumer) {
    if (prefix.isEmpty()) {
      consumer.consume(GrailsUtils.ENVIRONMENTS, false);
    }
    else if (prefix.size() == 1) {
      if (GrailsUtils.ENVIRONMENTS.equals(prefix.get(0))) {
        for (String s : GrailsUtils.ENVIRONMENT_LIST) {
          consumer.consume(s, false);
        }

        return;
      }
    }
    else {
      if (GrailsUtils.ENVIRONMENTS.equals(prefix.get(0)) && prefix.get(1) != null && GrailsUtils.ENVIRONMENT_LIST.contains(prefix.get(1))) {
        super.collectVariants(prefix.subList(2, prefix.size()), consumer);
        return;
      }
    }

    super.collectVariants(prefix, consumer);
  }
}
