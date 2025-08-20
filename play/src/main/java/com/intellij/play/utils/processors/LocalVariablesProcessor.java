package com.intellij.play.utils.processors;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.HashSet;
import java.util.Set;

public class LocalVariablesProcessor implements PlayDeclarationsProcessor {

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    for (PlayImplicitVariable playImplicitVariable : getLocalVariables(scope)) {
      if (!ResolveUtil.processElement(processor, playImplicitVariable, state)) return false;
    }
    return true;
  }

  public static Set<PlayImplicitVariable> getLocalVariables(PsiElement scope) {
    final Set<PlayImplicitVariable> set = new HashSet<>();
    final PsiFile file = scope.getContainingFile().getOriginalFile();
    final PsiMethod renderMethod = getTemplateRenderMethod(file);
    if (renderMethod != null) {
      renderMethod.acceptChildren(new PsiRecursiveElementVisitor() {
        @Override
        public void visitElement(@NotNull PsiElement element) {
          if (element instanceof PsiVariable psiVariable) {
            set.add(new PlayImplicitVariable(psiVariable.getName(), psiVariable.getType(), psiVariable));
          }
          super.visitElement(element);
        }
      });
    }

    return set;
  }

  @Nullable
  public static PsiMethod getTemplateRenderMethod(@Nullable PsiFile file) {
    if (file != null) {
      final PsiClass controller = PlayPathUtils.getCorrespondingController(file);
      if (controller != null) {
        for (PsiMethod psiMethod : controller.getAllMethods()) {
          final VirtualFile virtualFile = file.getVirtualFile();
          if (virtualFile != null) {
            if (virtualFile.getNameWithoutExtension().equals(psiMethod.getName())) {
              return psiMethod;
            }
          }
        }
      }
    }
    return null;
  }
}
