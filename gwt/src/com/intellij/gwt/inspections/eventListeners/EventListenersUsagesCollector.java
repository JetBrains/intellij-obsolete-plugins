package com.intellij.gwt.inspections.eventListeners;

import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EventListenersUsagesCollector extends JavaRecursiveElementWalkingVisitor {
  private final DeprecatedEventListenersRegistry myListenersRegistry;
  private final DeprecatedListenerUsages myUsages;

  EventListenersUsagesCollector() {
    myListenersRegistry = DeprecatedEventListenersRegistry.getInstance();
    myUsages = new DeprecatedListenerUsages();
  }

  public DeprecatedListenerUsages getListenerUsages() {
    return myUsages;
  }

  @Override
  public void visitVariable(@NotNull PsiVariable variable) {
    if (isDeprecatedListenerType(variable.getType(), true)) {
      myUsages.findOrRegisterDeclaration(variable);
      final PsiExpression initializer = variable.getInitializer();
      if (initializer != null) {
        myUsages.registerAssignment(variable, null, initializer);
      }
    }
    super.visitVariable(variable);
  }

  @Override
  public void visitAssignmentExpression(@NotNull PsiAssignmentExpression expression) {
    final PsiExpression left = expression.getLExpression();
    if (left instanceof PsiReferenceExpression) {
      final PsiElement resolved = ((PsiReferenceExpression)left).resolve();
      if (resolved instanceof PsiVariable variable) {
        final PsiType type = variable.getType();
        if (isDeprecatedListenerType(type, true)) {
          myUsages.registerAssignment(variable, left, expression.getRExpression());
        }
      }
    }

    super.visitAssignmentExpression(expression);
  }

  @Override
  public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
    final PsiElement resolved = reference.resolve();
    if (resolved instanceof PsiVariable variable) {
      final PsiType type = variable.getType();
      if (isDeprecatedListenerType(type, true)) {
        myUsages.registerReference(variable, reference);
      }
      else if (variable instanceof PsiParameter) {
        final PsiElement declaration = ((PsiParameter)variable).getDeclarationScope();
        if (declaration instanceof PsiMethod method) {
          if (myListenersRegistry.isEventMethodName(method.getName())) {
            final PsiMethod[] superMethods = method.findDeepestSuperMethods();
            for (PsiMethod superMethod : superMethods) {
              final PsiClass containingClass = superMethod.getContainingClass();
              if (containingClass != null && myListenersRegistry.isDeprecatedListener(containingClass.getQualifiedName())) {
                myUsages.registerParameterReference(method);
              }
            }
          }
        }
      }
    }

    super.visitReferenceElement(reference);
  }

  @Override
  public void visitNewExpression(@NotNull PsiNewExpression expression) {
    final PsiMethod constructor = expression.resolveConstructor();
    final PsiExpressionList argumentList = expression.getArgumentList();
    if (constructor != null && argumentList != null) {
      final PsiClass constructorClass = constructor.getContainingClass();

      if (constructorClass != null) {
        final PsiParameter[] parameters = constructor.getParameterList().getParameters();
        final PsiExpression[] arguments = argumentList.getExpressions();

        for (int i = 0; i < Math.min(parameters.length, arguments.length); i++) {
          final PsiType type = parameters[i].getType();
          if (type instanceof PsiClassType) {
            final PsiClass psiClass = ((PsiClassType)type).resolve();
            if (psiClass != null) {
              final DeprecatedListener listener = myListenersRegistry.findByListenerClass(psiClass.getQualifiedName());
              if (listener != null) {
                final String handlerClass = listener.getHandlerClassIfSingle();
                if (handlerClass != null) {
                  final PsiMethod[] constructors = constructorClass.findMethodsByName(constructor.getName(), false);
                  if (containsConstructorWithHandlerParameter(constructors, parameters, i, handlerClass)) {
                    myUsages.registerNewExpressionWithListenerAsParameter(listener, arguments[i]);
                  }
                }
              }
            }
          }
        }
      }
    }
    super.visitNewExpression(expression);
  }

  private static boolean containsConstructorWithHandlerParameter(PsiMethod[] constructors, PsiParameter[] parameters, int index,
                                                                 final String handlerClass) {
    for (PsiMethod constructor : constructors) {
      final PsiParameter[] constructorParameters = constructor.getParameterList().getParameters();
      if (parameters.length == constructorParameters.length) {
        boolean typesMatch = true;
        for (int i = 0; i < parameters.length; i++) {
          final PsiType constructorParameterType = constructorParameters[i].getType();
          if (i != index) {
            typesMatch &= constructorParameterType.equals(parameters[i].getType());
          }
          else {
            typesMatch &= constructorParameterType.equalsToText(handlerClass);
          }
        }
        if (typesMatch) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
    final PsiElement resolved = expression.getMethodExpression().resolve();
    if (resolved instanceof PsiMethod method) {
      final String methodName = method.getName();
      if (myListenersRegistry.isAddOrRemoveListenerMethodName(methodName)) {
        final PsiMethod[] methods = method.findDeepestSuperMethods();
        for (PsiMethod psiMethod : methods) {
          final PsiClass psiClass = psiMethod.getContainingClass();
          if (psiClass != null) {
            final DeprecatedListener info = myListenersRegistry.findByHolderClass(psiClass.getQualifiedName());
            if (info != null) {
              final PsiExpression qualifierExpression = expression.getMethodExpression().getQualifierExpression();
              if (qualifierExpression != null) {
                final PsiType componentType = qualifierExpression.getType();
                if (componentType != null) {
                  if (info.getAddListenerMethod().equals(methodName)) {
                  myUsages.registerAddOrRemoveListenerCall(info, expression, true);
                }
                else if (info.getRemoveListenerMethod().equals(methodName)) {
                  myUsages.registerAddOrRemoveListenerCall(info, expression, false);
                }
                }
              }
            }
          }
        }
      }
    }

    super.visitMethodCallExpression(expression);
  }

  private boolean isDeprecatedListenerType(@Nullable PsiType type, final boolean directImplementationAllowed) {
    if (!(type instanceof PsiClassType classType)) return false;

    final PsiClass psiClass = classType.resolve();
    if (psiClass == null) return false;

    if (myListenersRegistry.isDeprecatedListener(psiClass.getQualifiedName())) {
      return true;
    }

    if (!directImplementationAllowed) {
      return false;
    }
    final PsiClass superClass = psiClass.getSuperClass();
    if (superClass != null && myListenersRegistry.isDeprecatedListener(superClass.getQualifiedName())) {
      return true;
    }
    for (PsiClass anInterface : psiClass.getInterfaces()) {
      if (myListenersRegistry.isDeprecatedListener(anInterface.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }
}
