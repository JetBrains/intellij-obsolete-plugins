// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

public class GroovyGspAttributeWrapper implements GspAttributeWrapper {

  private final GrNamedArgument myNamedArgument;
  private final GroovyGspTagWrapper tagWrapper;

  public GroovyGspAttributeWrapper(GrNamedArgument namedArgument, GroovyGspTagWrapper tagWrapper) {
    myNamedArgument = namedArgument;
    this.tagWrapper = tagWrapper;
  }

  public GroovyGspAttributeWrapper(GrNamedArgument namedArgument, TagLibNamespaceDescriptor.GspTagMethod gspTagLibVariable) {
    this(namedArgument, new GroovyGspTagWrapper((GrNamedArgumentsOwner)namedArgument.getParent(), gspTagLibVariable));
  }

  @Override
  public @NotNull GspTagWrapper getTag() {
    return tagWrapper;
  }

  @Override
  public String getName() {
    return myNamedArgument.getLabelName();
  }

  @Override
  public PsiElement getValue() {
    return myNamedArgument.getExpression();
  }
}
