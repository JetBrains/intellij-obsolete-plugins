package com.intellij.play.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.play.constants.PlayConstants;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.play.references.PlayFakeRenameableReferenceProvider;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.play.utils.beans.PlayRenameableImplicitVariable;
import com.intellij.play.utils.processors.*;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayUtils {

  public static final String CONTROLLERS_PKG = "controllers";

  public static PlayDeclarationsProcessor[] myDeclarationsProcessors = new PlayDeclarationsProcessor[] {
    new ImplicitVariablesProcessor(),
    new ControllerMethodsProcessor(),
    new TopLevelControllersProcessor() ,
    new TopLevelControllerPackagesProcessor(),
    new LocalVariablesProcessor(),
    new RenderArgProcessor(),
    new ListTagImplicitVariablesProcessor(),
    new FieldTagImplicitVariablesProcessor()
  };

  public static boolean isController(@Nullable PsiClass psiClass) {
    return psiClass != null && InheritanceUtil.isInheritor(psiClass, PlayConstants.CONTROLLER_CLASS);
  }

  public static boolean isPlayInstalled(Project project) {
    return !project.isDefault() && PackageIndex.getInstance(project).getDirectoriesByPackageName("play.mvc", true).length > 0 &&
           PackageIndex.getInstance(project).getDirectoriesByPackageName("play.api.mvc", true).length == 0;
  }

  public static @Nullable PsiClass getObjectClass(Project project) {
    return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, GlobalSearchScope.allScope(project));
  }

  public static @Nullable PlayTag getContainingPlayTag(PsiElement expressionElement) {
    return  PsiTreeUtil.getParentOfType(expressionElement, PlayTag.class);
  }

  public static boolean processPlayDeclarations(PsiScopeProcessor processor, ResolveState state, final PsiElement scope) {
    for (PlayDeclarationsProcessor declarationsProcessor : myDeclarationsProcessors) {
       if(!declarationsProcessor.processElement(processor, state, scope)) return false;
    }
    return true;
  }

  public static boolean hasSecondaryElements(PsiElement psiElement) {
    if (psiElement instanceof PsiClass) {
      return PlayPathUtils.getCorrespondingDirectory((PsiClass)psiElement) != null;
    }
    else if (psiElement instanceof PsiMethod) {
      return PlayPathUtils.getCorrespondingView((PsiMethod)psiElement) != null;
    }
    else if (psiElement instanceof PlayPsiFile) {
      return PlayPathUtils.getCorrespondingControllerMethods((PsiFile)psiElement).length > 0;
    }

    return false;
  }

  public static LocalSearchScope getPsiClassLocalScope(@NotNull PsiClass controller) {
    return new LocalSearchScope(ArrayUtil.append(controller.getSupers(), controller));
  }

  public static Map<String, PlayImplicitVariable> getPutMethodInitVariables(@NotNull PsiMethod psiMethod,
                                                                            @NotNull SearchScope searchScope) {
    final @NotNull Map<String, PlayImplicitVariable> set = new ConcurrentHashMap<>();
    MethodReferencesSearch.search(psiMethod, searchScope, true).forEach(psiReference -> {
      final PsiMethodCallExpression methodCallExpression =
        PsiTreeUtil.getParentOfType(psiReference.getElement(), PsiMethodCallExpression.class);

      if (methodCallExpression != null) {
        final PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
        if (expressions.length == 2) {
          final PsiExpression expression = expressions[0];
          if (expression instanceof PsiLiteralExpression) {
            final String name = (String)((PsiLiteralExpression)expression).getValue();
            final PsiType type = expressions[1].getType();
            if (name != null && type != null) {
              set.put(name,
                      new PlayRenameableImplicitVariable(name, type, PlayFakeRenameableReferenceProvider.getOrCreateRenamebaleFakeElement(
                        (PsiLiteralExpression)expression)));
            }
          }
        }
      }
      return true;
    });

    return set;
  }
}