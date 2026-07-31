package com.intellij.gwt.inspections.eventListeners;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeprecatedListenerUsages {
  private final List<ListenerUsageInfo> myUsages = new ArrayList<>();
  private final Map<PsiVariable, ListenerUsageInfo> myVariable2Usage = new HashMap<>();
  private final DeprecatedEventListenersRegistry myRegistry;
  private final Set<PsiMethod> myMethodsWhichParametersAreUsed = new HashSet<>();

  public DeprecatedListenerUsages() {
    myRegistry = DeprecatedEventListenersRegistry.getInstance();
  }

  public Set<PsiMethod> getMethodsWhichParametersAreUsed() {
    return myMethodsWhichParametersAreUsed;
  }

  public @Nullable ListenerUsageInfo findOrRegisterDeclaration(@NotNull PsiVariable variable) {
    if (variable instanceof PsiField) {
      final PsiModifierList modifierList = variable.getModifierList();
      if (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.PRIVATE)) return null;
    }
    else if (!(variable instanceof PsiLocalVariable)) return null;

    ListenerUsageInfo usage = myVariable2Usage.get(variable);
    if (usage == null) {
      DeprecatedListener info = getListenerInfo(variable);
      if (info == null) return null;
      usage = new ListenerUsageInfo(info, variable.getTypeElement());
      myUsages.add(usage);
      myVariable2Usage.put(variable, usage);
    }
    return usage;
  }

  private @Nullable DeprecatedListener getListenerInfo(PsiVariable variable) {
    final PsiClassType type = (PsiClassType)variable.getType();
    final PsiClass psiClass = type.resolve();
    if (psiClass == null) return null;

    final String qualifiedName = psiClass.getQualifiedName();
    final DeprecatedListener listener = myRegistry.findByListenerClass(qualifiedName);
    if (listener != null) {
      return listener;
    }

    final PsiClass containingClass = psiClass.getContainingClass();
    if (containingClass == null) return null;

    final PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.PRIVATE)) return null;

    final PsiClass superClass = psiClass.getSuperClass();
    if (superClass != null) {
      final DeprecatedListener info = myRegistry.findByListenerClass(superClass.getQualifiedName());
      if (info != null) {
        return info;
      }
    }

    for (PsiClass anInterface : psiClass.getInterfaces()) {
      final DeprecatedListener info = myRegistry.findByListenerClass(anInterface.getQualifiedName());
      if (info != null) {
        return info;
      }
    }
    return null;
  }

  public void registerAssignment(PsiVariable variable, @Nullable PsiExpression variableReference, PsiExpression value) {
    final ListenerUsageInfo usageInfo = findOrRegisterDeclaration(variable);
    if (usageInfo != null) {
      final boolean processed = processNewListenerExpression(usageInfo.getListener(), value, usageInfo);
      if (processed && variableReference != null) {
        usageInfo.registerProcessedReference(variableReference);
      }
    }
  }

  public void registerReference(PsiVariable variable, PsiJavaCodeReferenceElement reference) {
    final ListenerUsageInfo usageInfo = findOrRegisterDeclaration(variable);
    if (usageInfo != null) {
      usageInfo.registerReference(reference);
    }
  }

  public void registerAddOrRemoveListenerCall(@NotNull DeprecatedListener listener, @NotNull PsiMethodCallExpression expression, boolean add) {
    final PsiExpression[] expressions = expression.getArgumentList().getExpressions();
    if (expressions.length != 1) {
      return;
    }

    final PsiExpression argument = expressions[0];
    final ListenerUsageInfo usageInfo = getInfoByReference(argument);
    if (usageInfo != null && usageInfo.getListener().equals(listener)) {
      usageInfo.registerProcessedReference(argument);
      usageInfo.setListenerAddedOrRemoved(add, expression);
    }
    else if (add) {
      final ListenerUsageInfo usage = new ListenerUsageInfo(listener, null);
      if (processNewListenerExpression(listener, argument, usage)) {
        usage.setListenerAddedOrRemoved(true, expression);
        myUsages.add(usage);
      }
    }
  }

  public void registerNewExpressionWithListenerAsParameter(DeprecatedListener listener, PsiExpression argument) {
    final ListenerUsageInfo usageInfo = getInfoByReference(argument);
    if (usageInfo != null && usageInfo.getListener().equals(listener)) {
      usageInfo.registerProcessedReference(argument);
    }
    else {
      final ListenerUsageInfo usage = new ListenerUsageInfo(listener, null);
      if (processNewListenerExpression(listener, argument, usage)) {
        myUsages.add(usage);
      }
    }
  }

  private boolean processNewListenerExpression(DeprecatedListener listener, PsiExpression expression, ListenerUsageInfo usage) {
    if (expression instanceof PsiNewExpression newExpression){
      final PsiExpressionList argumentsList = newExpression.getArgumentList();
      if (argumentsList == null) return false;

      final PsiAnonymousClass anonymousClass = newExpression.getAnonymousClass();
      if (anonymousClass != null) {
        if (argumentsList.getExpressions().length == 0) {
          final PsiClass psiClass = anonymousClass.getBaseClassType().resolve();
          if (psiClass != null && listener.equals(myRegistry.findByListenerClass(psiClass.getQualifiedName()))) {
            usage.setCreatedAsAnonymous(newExpression, anonymousClass);
            return true;
          }
        }
      }
      else {
        final PsiJavaCodeReferenceElement classReference = newExpression.getClassReference();
        if (classReference != null) {
          final PsiElement resolved = classReference.resolve();
          if (resolved instanceof PsiClass psiClass) {
            final PsiReferenceList implementsList = psiClass.getImplementsList();
            final PsiReferenceList extendsList = psiClass.getExtendsList();
            final PsiModifierList modifierList = psiClass.getModifierList();
            if (modifierList != null && modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
              if (!processReferenceToImplementedListener(listener, usage, psiClass, extendsList, true)) {
                if (!processReferenceToImplementedListener(listener, usage, psiClass, implementsList, false)) {
                  return false;
                }
              }
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean processReferenceToImplementedListener(DeprecatedListener listener, ListenerUsageInfo usage, PsiClass psiClass,
                                                        PsiReferenceList referenceList, boolean inExtendsList) {
    if (referenceList != null) {
      for (PsiJavaCodeReferenceElement interfaceReference : referenceList.getReferenceElements()) {
        final DeprecatedListener info = myRegistry.findByListenerClass(interfaceReference.getQualifiedName());
        if (listener.equals(info)) {
          usage.setCreatedAsInner(interfaceReference, psiClass, inExtendsList);
          return true;
        }
      }
    }
    return false;
  }

  private @Nullable ListenerUsageInfo getInfoByReference(PsiExpression argument) {
    if (argument instanceof PsiReferenceExpression) {
      final PsiElement element = ((PsiReferenceExpression)argument).resolve();
      if (element instanceof PsiVariable) {
        return findOrRegisterDeclaration((PsiVariable)element);
      }
    }
    return null;
  }

  public ListenerUsageInfo[] getUsages() {
    return myUsages.toArray(new ListenerUsageInfo[0]);
  }

  public void registerParameterReference(@NotNull PsiMethod method) {
    myMethodsWhichParametersAreUsed.add(method);
  }
}
