package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UiRendererUtil {

  public static final @NonNls String ABSTRACT_CELL_CLASS = "com.google.gwt.cell.client.AbstractCell";
  public static final @NonNls String CELL_INTERFACE = "com.google.gwt.cell.client.Cell";
  public static final @NonNls String RENDER_METHOD_NAME = "render";

  public static @Nullable PsiMethod findRenderMethodImplementation(@NotNull PsiClass abstractCellInheritor) {
    PsiMethod[] renderMethods = abstractCellInheritor.findMethodsByName(RENDER_METHOD_NAME, false);
    for (PsiMethod renderMethod : renderMethods) {
      PsiType returnType = renderMethod.getReturnType();
      if (!PsiTypes.voidType().equals(returnType)) continue;

      if (renderMethod.getParameterList().getParametersCount() != 3) continue;

      PsiMethod[] superMethods = renderMethod.findDeepestSuperMethods();
      if (superMethods.length != 1) continue;

      PsiClass containingClass = superMethods[0].getContainingClass();
      if (containingClass == null) continue;
      if (!CELL_INTERFACE.equals(containingClass.getQualifiedName())) continue;

      return renderMethod;
    }

    PsiClass superClass = abstractCellInheritor.getSuperClass();
    if (superClass == null) return null;

    return findRenderMethodImplementation(superClass);
  }
}
