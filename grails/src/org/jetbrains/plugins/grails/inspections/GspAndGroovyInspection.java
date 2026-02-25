// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;

public abstract class GspAndGroovyInspection extends LocalInspectionTool {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    GspElementVisitor visitor = createGspElementVisitor();
    assert visitor.getProblemHolder() == null; // Assert it's new visitor instance.

    visitor.setProblemHolder(holder);
    visitor.setOnTheFly(isOnTheFly);
    return visitor;
  }

  protected abstract GroovyRecursiveElementVisitor createGroovyFileVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly);

  protected abstract GspElementVisitor createGspElementVisitor();

  public class GspElementVisitor extends XmlElementVisitor {

    private ProblemsHolder myProblemHolder;
    private boolean myIsOnTheFly;

    public ProblemsHolder getProblemHolder() {
      return myProblemHolder;
    }

    public void setProblemHolder(ProblemsHolder holder) {
      myProblemHolder = holder;
    }

    public boolean isOnTheFly() {
      return myIsOnTheFly;
    }

    public void setOnTheFly(boolean onTheFly) {
      myIsOnTheFly = onTheFly;
    }

    @Override
    public void visitFile(@NotNull PsiFile psiFile) {
      if (psiFile instanceof GroovyFileBase) {
        GroovyRecursiveElementVisitor visitor = createGroovyFileVisitor(myProblemHolder, myIsOnTheFly);
        ((GroovyFileBase)psiFile).accept(visitor);
      }
    }
  }
}
