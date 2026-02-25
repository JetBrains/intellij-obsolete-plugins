// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspGroovyElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.impl.GspImportUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.GrTopStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyFileBaseImpl;
import org.jetbrains.plugins.groovy.lang.resolve.imports.GroovyFileImports;

public class GspGroovyFileImpl extends GroovyFileBaseImpl implements GspGroovyFile {

  private final CachedValue<GspPageSkeleton> myPageSkeletonCachedValue;

  @Override
  public @NotNull GrStatement addStatementBefore(@NotNull GrStatement statement, @Nullable GrStatement anchor) throws IncorrectOperationException {
    return super.addStatementBefore(statement, anchor);
  }

  public GspGroovyFileImpl(final FileViewProvider viewProvider) {
    super(GspGroovyElementTypes.GSP_GROOVY_DECLARATIONS_ROOT, GspGroovyElementTypes.GSP_GROOVY_DECLARATIONS_ROOT, viewProvider);

    CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(getProject());

    myPageSkeletonCachedValue = cachedValuesManager.createCachedValue(
      () -> new CachedValueProvider.Result<>(new GspPageSkeleton(this),
                                             PsiModificationTracker.MODIFICATION_COUNT), false);
  }

  @Override
  public GspPageSkeleton getSkeleton() {
    return myPageSkeletonCachedValue.getValue();
  }

  @Override
  public String toString() {
    return "GspGroovyDummyHolder";
  }

  @Override
  public @NotNull GspFile getGspLanguageRoot() {
    GspFile res = (GspFile)getViewProvider().getPsi(GspLanguage.INSTANCE);
    assert res != null;
    return res;
  }

  @Override
  public GrImportStatement addImportForClass(@NotNull PsiClass aClass) throws IncorrectOperationException {
    getGspLanguageRoot().addImportForClass(aClass);
    return null;
  }

  @Override
  public @NotNull GrImportStatement addImport(@NotNull GrImportStatement statement) throws IncorrectOperationException {
    //todo implement me!
    return null;
  }

  // GSP page is ALWAYS Groovy script
  @Override
  public boolean isScript() {
    return true;
  }

  @Override
  public @Nullable PsiClass getScriptClass() {
    return (PsiClass)getFirstChild();
  }

  @Override
  public PsiClass @NotNull [] getClasses() {
    return new PsiClass[]{getScriptClass()};
  }

  @Override
  public GrTypeDefinition @NotNull [] getTypeDefinitions() {
    return GrTypeDefinition.EMPTY_ARRAY;
  }

  @Override
  public GrTopStatement @NotNull [] getTopStatements() {
    //todo implement me!
    return GrTopStatement.EMPTY_ARRAY;

  }

  @Override
  public void removeImport(@NotNull GrImportStatement importStatement) throws IncorrectOperationException {
    //todo implement me!
  }

  @Override
  public @NotNull String getPackageName() {
    return "";
  }

  @Override
  public void setPackageName(@NotNull String s) throws IncorrectOperationException {
    throw new IncorrectOperationException("Cannot set package name for gsp files");
  }

  @Override
  public @NotNull GroovyFileImports getImports() {
    return GspImportUtil.getFileImports(this);
  }
}
