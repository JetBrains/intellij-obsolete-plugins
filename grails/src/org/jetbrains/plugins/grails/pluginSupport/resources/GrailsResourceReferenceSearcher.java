// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.resources;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public final class GrailsResourceReferenceSearcher extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters> {

  public GrailsResourceReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull MethodReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiMethod elementToSearch = queryParameters.getMethod();
    if (!GrLightMethodBuilder.checkKind(elementToSearch, GrailsResourcesUtil.MODULE_METHOD_KIND)) return;
    
    GrMethodCall methodCall = (GrMethodCall)elementToSearch.getNavigationElement();
    GrReferenceExpression invokedExpression = (GrReferenceExpression)methodCall.getInvokedExpression();

    consumer.process(new ModuleDeclarationReference(invokedExpression, elementToSearch));
    
    SearchScope searchScope = queryParameters.getEffectiveSearchScope();

    if (searchScope instanceof GlobalSearchScope) {
      searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(((GlobalSearchScope)searchScope), GspFileType.GSP_FILE_TYPE,
                                                                    GroovyFileType.GROOVY_FILE_TYPE);
    }
    
    String text = elementToSearch.getName();
    
    queryParameters.getOptimizer().searchWord(text,
                                              searchScope,
                                              (short)(UsageSearchContext.IN_FOREIGN_LANGUAGES | UsageSearchContext.IN_STRINGS),
                                              true,
                                              elementToSearch);
  }

  private static class ModuleDeclarationReference extends PsiReferenceBase<PsiElement> {
    private final PsiMethod myModuleDeclaration;

    ModuleDeclarationReference(GrReferenceExpression invokedExpression, PsiMethod moduleDeclaration) {
      super(invokedExpression, new TextRange(0, invokedExpression.getTextLength()), false);
      myModuleDeclaration = moduleDeclaration;
    }

    @Override
    public PsiElement resolve() {
      return myModuleDeclaration;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
      return ((GrReferenceExpression)getElement()).handleElementRename(newElementName);
    }
  }
}
