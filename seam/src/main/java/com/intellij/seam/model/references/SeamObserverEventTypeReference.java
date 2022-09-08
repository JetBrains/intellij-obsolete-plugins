package com.intellij.seam.model.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamModel;
import com.intellij.seam.model.jam.context.SeamJamRaiseEvent;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class SeamObserverEventTypeReference<T extends PsiElement> extends BasicEventTypeReference<T> {
  public SeamObserverEventTypeReference(final T element) {
    super(element);
  }

  @Override
  public Object @NotNull [] getVariants() {
    List<String> eventTypes = new ArrayList<>();
    final Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module != null) {
      addRaisedEventTypeFromMethodCalls(eventTypes, module);
      addAnnotatedRaisedEvents(eventTypes, module);
    }
    return ArrayUtil.toObjectArray(eventTypes);
  }

  private static void addAnnotatedRaisedEvents(final List<String> eventTypes, final Module module) {
    for (SeamJamComponent seamJamComponent : SeamJamModel.getModel(module).getSeamComponents()) {
      for (SeamJamRaiseEvent event : seamJamComponent.getRaiseEvents()) {
        final String eventType = event.getEventType();
        if (!StringUtil.isEmptyOrSpaces(eventType)) {
          eventTypes.add(eventType);
        }
      }
    }
  }

  private static void addRaisedEventTypeFromMethodCalls(final List<String> eventTypes, final Module module) {
    final PsiClass eventClass = JavaPsiFacade.getInstance(module.getProject())
      .findClass(SeamEventTypeReferenceProvider.SEAM_EVENTS_CLASSNAME, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false));

    if (eventClass != null) {
      for (PsiMethod psiMethod : eventClass.getMethods()) {
        if (psiMethod.getName().startsWith("raise")) {
          for (PsiReference reference : MethodReferencesSearch.search(psiMethod, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module), false)) {
            final PsiElement element = reference.getElement().getParent();
            if (element instanceof PsiMethodCallExpression) {
              final PsiMethodCallExpression expression = (PsiMethodCallExpression)element;
              final PsiExpression[] psiExpressions = expression.getArgumentList().getExpressions();
              if (psiExpressions.length > 0 && psiExpressions[0] instanceof PsiLiteralExpression) {
                eventTypes.add((String)((PsiLiteralExpression)psiExpressions[0]).getValue());
              }
            }
          }
        }
      }
    }
  }
}
