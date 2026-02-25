// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiClassPattern;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttributeValue;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;

public final class GrailsPatterns {

  private GrailsPatterns() {

  }

  public static XmlAttributeValuePattern gspAttributeValue(ElementPattern<? extends XmlAttribute> attributePattern) {
    return gspAttributeValue().withParent(attributePattern);
  }

  public static PsiFilePattern.Capture<PsiFile> buildConfig() {
    return new PsiFilePattern.Capture<>(new InitialPatternCondition<>(PsiFile.class) {
      @Override
      public boolean accepts(@Nullable Object o, ProcessingContext context) {
        return o instanceof GroovyFileBase && GrailsUtils.isBuildConfigFile((PsiFile)o);
      }
    });
  }

  public static PsiClassPattern artifact(final @NotNull GrailsArtifact artifact) {
    return PsiJavaPatterns.psiClass().with(new PatternCondition<>("Grails artifact") {
      @Override
      public boolean accepts(@NotNull PsiClass aClass, ProcessingContext context) {
        return artifact.isInstance(aClass);
      }
    });
  }

  public static XmlAttributeValuePattern gspAttributeValue() {
    return new XmlAttributeValuePattern(CONDITION);
  }

  private static final InitialPatternCondition CONDITION = new InitialPatternCondition<>(GspAttributeValue.class) {
    @Override
    public boolean accepts(final @Nullable Object o, final ProcessingContext context) {
      return o instanceof GspAttributeValue;
    }
  };

}
