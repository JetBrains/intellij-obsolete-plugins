// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.util.CharTable;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;

public final class GspOuterHtmlElementManipulator extends AbstractElementManipulator<GspOuterHtmlElementImpl> {
  @Override
  public GspOuterHtmlElementImpl handleContentChange(@NotNull GspOuterHtmlElementImpl element, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
    String newText = range.replace(element.getText(), newContent);

    CharTable charTable = SharedImplUtil.findCharTableByTree(element.getNode());

    LeafElement e = Factory.createSingleLeafElement(GspTokenTypesEx.GSP_TEMPLATE_DATA, newText, charTable, element.getManager());

    return (GspOuterHtmlElementImpl)element.replace(e.getPsi());
  }
}
