package com.intellij.play.utils.processors;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.language.psi.PlayNameValueCompositeElement;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.play.references.PlayFakeRenameableReferenceProvider;
import com.intellij.play.utils.PlayUtils;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.play.utils.beans.PlayRenameableImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.impl.RenameableFakePsiElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.HashSet;
import java.util.Set;

public class ListTagImplicitVariablesProcessor implements PlayDeclarationsProcessor {

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    final Set<PlayImplicitVariable> listVariables = getParentListVariables(scope);
    if (listVariables != null) {
      for (PlayImplicitVariable playImplicitVariable : listVariables) {
        if (!ResolveUtil.processElement(processor, playImplicitVariable, state)) return false;
      }
    }
    return true;
  }

  private static Set<PlayTag> getParentListTags(PsiElement expressionElement) {
    Set<PlayTag> listTags = new HashSet<>();
    PsiElement parent = expressionElement.getParent();
    while (parent != null) {
      if (parent instanceof PlayTag && "list".equals(((PlayTag)parent).getName())) {
        listTags.add((PlayTag)parent);
      }
      parent = parent.getParent();
    }

    return listTags;
  }

  @Nullable
  public static Set<PlayImplicitVariable> getParentListVariables(final PsiElement expressionElement) {
    return RecursionManager.doPreventingRecursion(expressionElement, true, () -> {
        Set<PlayImplicitVariable> vars = new HashSet<>();

        Set<PlayTag> listTags = getParentListTags(expressionElement);
        for (PlayTag listTag : listTags) {
          vars.addAll(getListTagIterableVariables(listTag));
        }

        return vars;
      });
  }

  @NotNull
  public static Set<PlayImplicitVariable> getListTagIterableVariables(@Nullable PsiElement psiElement) {
    Set<PlayImplicitVariable> variables = new HashSet<>();
    if (psiElement instanceof PlayTag listTag) {
      if ("list".equals(listTag.getName())) {
        final Project project = listTag.getProject();
        final GrExpression grExpression = getGrExpression(listTag, "items");
        if (grExpression != null) {
          PsiClass psiClass = getListTagCollectionType(grExpression);

          if (psiClass != null) {
            final GrExpression nameExpression = getGrExpression(listTag, "as");
            // IDEA-78777
            if (nameExpression == null) {
              variables.add(new PlayImplicitVariable("_", JavaPsiFacade.getElementFactory(project).createType(psiClass), grExpression));
            }
            else if (nameExpression instanceof GrLiteral) {
              String name = getStringValue(nameExpression);
              if (!StringUtil.isEmptyOrSpaces(name)) {

                final RenameableFakePsiElement element = PlayFakeRenameableReferenceProvider.getOrCreateRenamebaleFakeElement(
                  (GrLiteral)nameExpression);

                variables
                  .add(new PlayRenameableImplicitVariable(name, JavaPsiFacade.getElementFactory(project).createType(psiClass), element));
                variables
                  .add(new PlayRenameableImplicitVariable(name + "_index", JavaPsiFacade.getElementFactory(project).createTypeByFQClassName(
                    CommonClassNames.JAVA_LANG_INTEGER), element));
                variables.add(
                  new PlayRenameableImplicitVariable(name + "_isLast", JavaPsiFacade.getElementFactory(project).createTypeByFQClassName(
                    CommonClassNames.JAVA_LANG_BOOLEAN), element));
                variables.add(
                  new PlayRenameableImplicitVariable(name + "_isFirst", JavaPsiFacade.getElementFactory(project).createTypeByFQClassName(
                    CommonClassNames.JAVA_LANG_BOOLEAN), element));
                variables.add(new PlayRenameableImplicitVariable(name + "_parity", JavaPsiFacade.getElementFactory(project)
                  .createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT), element));
              }
            }
          }
        }
      }
    }
    return variables;
  }

  @Nullable
  private static String getStringValue(GrExpression nameExpression) {
    if (nameExpression instanceof GrLiteral) {
      final Object value = ((GrLiteral)nameExpression).getValue();
      if (value instanceof String) {
        return ((String)value);
      }
    }
    return null;
  }

  @Nullable
  private static PsiClass getListTagCollectionType(@NotNull GrExpression grExpression) {
    final PsiType type = grExpression.getType();
    if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)) {
      final PsiType psiType = PsiUtil.extractIterableTypeParameter(type, true);
      if (psiType instanceof PsiClassType) {
        return ((PsiClassType)psiType).resolve();
      }
    }

    return PlayUtils.getObjectClass(grExpression.getProject());
  }

  @Nullable
  private static GrExpression getGrExpression(@NotNull PlayTag tag, String name) {
    final PlayNameValueCompositeElement nameValue = tag.findNameValue(name);
    if (nameValue == null) return null;
    final PsiElement valueElement = nameValue.getValueElement();
    return valueElement == null ? null : PsiTreeUtil.getChildOfType(valueElement, GrExpression.class);
  }
}
