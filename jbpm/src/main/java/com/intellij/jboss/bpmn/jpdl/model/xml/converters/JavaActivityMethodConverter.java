package com.intellij.jboss.bpmn.jpdl.model.xml.converters;

import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.JavaActivity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class JavaActivityMethodConverter extends ResolvingConverter<PsiMethod> {

  @Override
  @NotNull
  public Collection<? extends PsiMethod> getVariants(final ConvertContext context) {
    return getAllMethods(context);
  }

  @Override
  public PsiMethod fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s != null) {
      for (PsiMethod method : getAllMethods(context)) {
        if (s.equals(method.getName())) {
          return method;
        }
      }
    }
    return null;
  }

  @Override
  public String toString(@Nullable final PsiMethod psiMethod, final ConvertContext context) {
    return psiMethod != null ? psiMethod.getName() : null;
  }

  private static Collection<? extends PsiMethod> getAllMethods(final ConvertContext context) {
    final JavaActivity javaActivity = context.getInvocationElement().getParentOfType(JavaActivity.class, false);

    Set<PsiMethod> methods = new HashSet<>();
    if (javaActivity != null) {
      final PsiClass psiClass = getPsiClass(javaActivity);
      if (psiClass != null) {
        methods.addAll(Arrays.asList(psiClass.getAllMethods()));
      }
    }
    return methods;
  }

  private static PsiClass getPsiClass(JavaActivity javaActivity) {
    //todo analyse javaActivity.getEjbJndiName()
    return javaActivity.getClazz().getValue();
  }
}
