// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the just-in-time (JIT) constructor binding that Guice creates for a key with no explicit binding.
 *
 * <p>The navigation index holds the {@code @Inject} constructors of the source classes.
 * It does not hold the classes that Guice builds through a no-argument constructor,
 * or the classes in libraries. This class finds such a binding when the user asks for it.
 * The binding has no reverse navigation: the constructor does not show its injection points.
 *
 * <p>Guice creates no JIT binding for a qualified key, an interface, an abstract class,
 * an enum or an inner class. A private no-argument constructor is usable only in a private class.
 */
final class GuiceJitBindings {
  private GuiceJitBindings() {}

  /**
   * Returns a {@link EntryRole#BINDING_SITE} entry for the JIT binding of the key,
   * or {@code null} when Guice creates no JIT binding for the key.
   *
   * @param context an element in the module whose resolve scope finds the class of the key
   */
  static @Nullable GuiceEntry findConstructorBinding(@NotNull GuiceBindingKey key, @NotNull PsiElement context) {
    if (key.getQualifier() != null) return null;
    String fqn = key.getTypeFqn();
    // The JDK and Kotlin classes, for example String, make noise and are not Guice bindings in practice.
    if (fqn == null || fqn.startsWith("java.") || fqn.startsWith("kotlin.")) return null;

    PsiClass psiClass = JavaPsiFacade.getInstance(context.getProject()).findClass(fqn, context.getResolveScope());
    if (psiClass == null || !isConstructable(psiClass)) return null;

    PsiElement target = findConstructor(psiClass);
    if (target == null) return null;
    return new GuiceEntry(GuiceBindingKey.forClass(psiClass), target, target, EntryRole.BINDING_SITE,
                          SymbolPresentationUtil::getSymbolPresentableText);
  }

  private static boolean isConstructable(@NotNull PsiClass psiClass) {
    if (psiClass.isInterface() || psiClass.isEnum() || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) return false;
    if (PsiUtil.isLocalOrAnonymousClass(psiClass)) return false;
    return psiClass.getContainingClass() == null || psiClass.hasModifierProperty(PsiModifier.STATIC);
  }

  /**
   * Returns the {@code @Inject} constructor, the usable no-argument constructor,
   * or the class itself when it declares no constructor. Returns {@code null} when Guice cannot build the class.
   */
  private static @Nullable PsiElement findConstructor(@NotNull PsiClass psiClass) {
    PsiMethod injectConstructor = GuiceUtils.getJitConstructor(psiClass);
    if (injectConstructor != null) return injectConstructor;

    // The implicit canonical constructor of a record takes the components, so Guice cannot call it.
    if (psiClass.isRecord()) return null;
    PsiMethod[] constructors = psiClass.getConstructors();
    if (constructors.length == 0) return psiClass;
    for (PsiMethod constructor : constructors) {
      if (!constructor.getParameterList().isEmpty()) continue;
      if (constructor.hasModifierProperty(PsiModifier.PRIVATE) && !psiClass.hasModifierProperty(PsiModifier.PRIVATE)) {
        return null;
      }
      return constructor;
    }
    return null;
  }
}
