// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspDirectiveKind;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspScriptletTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;

public abstract class GspPsiElementFactory {

  public static GspPsiElementFactory getInstance(Project project) {
    return project.getService(GspPsiElementFactory.class);
  }

  public abstract GspDirective createDirectiveByKind(GspDirectiveKind kind);

  public abstract GspDirectiveAttribute createDirectiveAttribute(@NotNull String name, @NotNull String value);

  public abstract GspScriptletTag createScriptletTagFromText(String text);

  public abstract GspOuterHtmlElement createOuterHtmlElement(String text);

  public abstract <T> T createElementFromText(String text);

}
