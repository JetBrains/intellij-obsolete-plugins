// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.groovy;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspGroovyElementTypes;

public class GspGroovyParser implements PsiParser, GspGroovyElementTypes {

  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    Marker fileMarker = builder.mark();
    Marker classMarker = builder.mark();
    Marker methodMarker = builder.mark();

    new GspAwareGroovyParser().parseLight(GSP_RUN_BLOCK, builder);

    methodMarker.done(GSP_RUN_METHOD);
    classMarker.done(GSP_CLASS);
    fileMarker.done(root);

    return builder.getTreeBuilt();
  }
}
