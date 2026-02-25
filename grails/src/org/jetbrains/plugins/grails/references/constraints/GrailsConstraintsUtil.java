// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.HashMap;
import java.util.Map;

public final class GrailsConstraintsUtil {

  public static final Object CONSTRAINT_METHOD_MARKER = "Grails:Constraint:method";

  public static final String GRAILS_GORM_DEFAULT_CONSTRAINTS = "grails.gorm.default.constraints";

  private GrailsConstraintsUtil() {
  }

  public static boolean isConstraintsMethod(@Nullable PsiElement method) {
    return GrLightMethodBuilder.checkKind(method, CONSTRAINT_METHOD_MARKER);
  }

  public static boolean processImportFromMethod(PsiScopeProcessor processor, ResolveState state, @NotNull PsiElement context, String nameHint) {
    if (nameHint != null && !nameHint.equals("importFrom")) return true;

    GrLightMethodBuilder importFromMethod = new GrLightMethodBuilder(context.getManager(), "importFrom");
    importFromMethod.addOptionalParameter("map", CommonClassNames.JAVA_UTIL_MAP);
    importFromMethod.addParameter("clazz", CommonClassNames.JAVA_LANG_CLASS);

    Map<String, NamedArgumentDescriptor> map = new HashMap<>();
    map.put("include", NamedArgumentDescriptor.TYPE_LIST);
    map.put("exclude", NamedArgumentDescriptor.TYPE_LIST);
    importFromMethod.setNamedParameters(map);

    return processor.execute(importFromMethod, state);
  }

  public static PsiMethod createMethod(String name, PsiElement navigationElement, @Nullable PsiType valueType, @Nullable PsiClass validatedClass) {
    GrLightMethodBuilder res = new GrLightMethodBuilder(navigationElement.getManager(), name);
    res.addOptionalParameter("constraints", CommonClassNames.JAVA_UTIL_MAP);
    res.setNavigationElement(navigationElement);

    res.setMethodKind(CONSTRAINT_METHOD_MARKER);
    res.setData(Pair.create(valueType, validatedClass));
    res.setContainingClass(validatedClass);

    return res;
  }

  public static @Nullable PsiClass getValidatedClass(@Nullable PsiElement constraintMethod) {
    Pair<PsiType, PsiClass> data = GrLightMethodBuilder.getData(constraintMethod, CONSTRAINT_METHOD_MARKER);
    return Pair.getSecond(data);
  }

  public static @Nullable PsiType getValidatedValueType(@Nullable PsiElement constraintMethod) {
    Pair<PsiType, PsiClass> data = GrLightMethodBuilder.getData(constraintMethod, CONSTRAINT_METHOD_MARKER);
    return Pair.getFirst(data);
  }
}
