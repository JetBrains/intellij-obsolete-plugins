// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.util.IncorrectOperationException;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

public class InjectionPointDescriptor {
  private final SmartPsiElementPointer<PsiElement> myOwnerPointer;
  /**
   * When {@code true}, this IP originates from a binding-declaration call ({@code .to()},
   * {@code .toProvider()}) rather than a real injection site ({@code @Inject} field/param,
   * {@code getProvider()}).  Binding-declaration IPs should NOT have their type unwrapped
   * via {@code GuiceUtils.getProviderType()} because the referenced class is the exact
   * binding target (e.g., the provider class itself, not what it provides).
   */
  private final boolean myIsBindingCall;

  public InjectionPointDescriptor(@NotNull PsiElement owner) {
    this(owner, false);
  }

  public InjectionPointDescriptor(@NotNull PsiElement owner, boolean isBindingCall) {
    myOwnerPointer = SmartPointerManager.createPointer(owner);
    myIsBindingCall = isBindingCall;
  }

  public @Nullable PsiType getType() {
    PsiElement owner = myOwnerPointer.getElement();
    switch (owner) {
      case null -> {
        return null;
      }
      case PsiField psiField -> {
        return psiField.getType();
      }
      case PsiParameter psiParameter -> {
        return psiParameter.getType();
      }
      default -> {
      }
    }

    final UCallExpression uCall = GuiceUtils.getCallExpression(owner);
    if (uCall == null) return null;

    final String methodName = uCall.getMethodName();
    if (!"to".equals(methodName) && !"toProvider".equals(methodName) && !"getProvider".equals(methodName)) {
      return null;
    }

    // Try value arguments first (Java-style: .to(Bar.class), .to(Key.get(Bar.class)))
    PsiType type = extractTypeFromValueArgs(uCall);

    // Fallback: type arguments (Kotlin reified style: .to<Bar>(), .getProvider<Bar>())
    if (type == null) {
      type = extractTypeFromTypeArgs(uCall);
    }

    if (type == null) return null;

    if ("getProvider".equals(methodName)) {
      return wrapInProviderType(type, owner);
    }
    return type;
  }

  private @Nullable PsiType extractTypeFromValueArgs(@NotNull UCallExpression uCall) {
    final java.util.List<UExpression> args = uCall.getValueArguments();
    if (args.isEmpty()) return null;
    return GuiceUtils.getBindingTypeFromExpression(args.getFirst());
  }

  private @Nullable PsiType extractTypeFromTypeArgs(@NotNull UCallExpression uCall) {
    final java.util.List<PsiType> typeArgs = uCall.getTypeArguments();
    if (typeArgs.isEmpty()) return null;
    return typeArgs.getFirst();
  }

  private @Nullable PsiType wrapInProviderType(@NotNull PsiType elementType, @NotNull PsiElement context) {
    final PsiClass providerClass = JavaPsiFacade.getInstance(context.getProject())
      .findClass("com.google.inject.Provider", context.getResolveScope());
    if (providerClass == null) return null;
    return JavaPsiFacade.getInstance(context.getProject()).getElementFactory()
      .createType(providerClass, elementType);
  }

  public Set<PsiAnnotation> getBindingAnnotations() {
    PsiElement owner = myOwnerPointer.getElement();
    if (owner == null) return java.util.Collections.emptySet();
    if (owner instanceof PsiModifierListOwner) {
      return GuiceInjectorManager.getBindingAnnotations((PsiModifierListOwner)owner);
    }
    
    final UCallExpression uCall = GuiceUtils.getCallExpression(owner);
    if (uCall != null) {
      final java.util.List<UExpression> args = uCall.getValueArguments();
      if (!args.isEmpty()) {
        final PsiClass qualifier = GuiceUtils.getQualifierFromExpression(args.getFirst());
        if (qualifier != null && qualifier.getQualifiedName() != null) {
          try {
            final PsiAnnotation annotation = JavaPsiFacade.getInstance(owner.getProject()).getElementFactory()
              .createAnnotationFromText("@" + qualifier.getQualifiedName(), owner);
            return Set.of(annotation);
          } catch (IncorrectOperationException e) {
            // fallback
          }
        }
        final UExpression namedExpr = GuiceUtils.getNamedExpressionFromKeyGet(args.getFirst());
        if (namedExpr != null) {
          final PsiElement sourcePsi = namedExpr.getSourcePsi();
          if (sourcePsi instanceof PsiExpression) {
            final Pair<PsiElement, String> pair = StringExpressionHelper.evaluateExpression(sourcePsi);
            if (pair != null) {
              final String nameValue = pair.getSecond();
              if (nameValue != null) {
                try {
                  final PsiAnnotation annotation = JavaPsiFacade.getInstance(owner.getProject()).getElementFactory()
                    .createAnnotationFromText("@com.google.inject.name.Named(\"" + nameValue + "\")", owner);
                  return Set.of(annotation);
                } catch (IncorrectOperationException e) {
                  // fallback
                }
              }
            }
          }
        }
      }
    }
    return java.util.Collections.emptySet();
  }

  /**
   * Returns the PSI element this descriptor points to, or {@code null} if the
   * element has been deleted (e.g., the containing file was removed).
   */
  public @Nullable PsiElement getOwner() {
    return myOwnerPointer.getElement();
  }

  /**
   * Returns {@code true} if this IP originates from a binding call ({@code .to()},
   * {@code .toProvider()}) rather than a real injection site.  When {@code true},
   * Provider-type unwrapping should be skipped during binding lookup.
   */
  public boolean isBindingCall() {
    return myIsBindingCall;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InjectionPointDescriptor that)) return false;

    if (!myOwnerPointer.equals(that.myOwnerPointer)) return false;
    return myIsBindingCall == that.myIsBindingCall;
  }

  @Override
  public int hashCode() {
    return 31 * myOwnerPointer.hashCode() + (myIsBindingCall ? 1 : 0);
  }
}
