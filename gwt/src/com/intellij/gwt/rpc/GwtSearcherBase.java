package com.intellij.gwt.rpc;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiFile;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import org.jetbrains.annotations.NotNull;

public abstract class GwtSearcherBase<Result, Param> implements QueryExecutor<Result, Param> {
  @Override
  public boolean execute(final @NotNull Param queryParameters, final @NotNull Processor<? super Result> consumer) {
    return ReadAction.computeBlocking(() -> {
      PsiFile file = getContainingFile(queryParameters);
      if (file != null) {
        GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(file.getProject(), file.getVirtualFile());
        if (gwtFacet != null) {
          return doExecute(queryParameters, consumer);
        }
      }
      return true;
    });
  }

  protected abstract PsiFile getContainingFile(Param parameters);

  protected abstract boolean doExecute(final Param queryParameters, final Processor<? super Result> consumer);
}
