// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails;

import com.intellij.lang.cacheBuilder.SimpleWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import org.jetbrains.plugins.groovy.findUsages.GroovyFindUsagesProvider;

public final class GspFindUsagesProvider extends GroovyFindUsagesProvider {
  @Override
  public WordsScanner getWordsScanner() {
    return new SimpleWordsScanner();
  }
}
