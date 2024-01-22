package com.intellij.jboss.bpmn.jpdl.el;

import com.intellij.javaee.el.util.ELImplicitVariable;
import com.intellij.javaee.model.psi.JavaeeImplicitVariable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class JpdlELVariablesCollectorUtil {
  private static final List<Pair<String, String>> predefinedVars = new ArrayList<>();

  static {
    // todo: discover all vars
    predefinedVars.add(Pair.create("processInstance", "org.jbpm.api.ProcessInstance"));
    predefinedVars.add(Pair.create("processDefinition", "org.jbpm.api.ProcessDefinition"));
  }

  public static List<JavaeeImplicitVariable> collectPredefinedVariables(@NotNull final PsiFile psiFile) {
    List<JavaeeImplicitVariable> vars = new ArrayList<>();

    final PsiType objectClassType = getObjectClassType(psiFile.getProject());
    for (Pair<String, String> pair : predefinedVars) {
      final String varName = pair.first;
      final String className = pair.second;

      PsiType psiType = getPsiClassTypeByName(psiFile.getProject(), className);
      if (psiType == null) {
        psiType = objectClassType;
      }

      addImplicitVariable(new FakePsiElement() {
        @Override
        public PsiElement getParent() {
          return psiFile;
        }
      }, varName, vars, psiType, psiFile);
    }

    return vars;
  }

  @Nullable
  private static PsiType getPsiClassTypeByName(final Project project, final String className) {
    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);

    final PsiClass psiClass = psiFacade.findClass(className, GlobalSearchScope.allScope(project));

    return psiClass == null ? null : psiFacade.getElementFactory().createType(psiClass);
  }

  private static PsiType getObjectClassType(final Project project) {
    final PsiType psiType = getPsiClassTypeByName(project, CommonClassNames.JAVA_LANG_OBJECT);

    return psiType == null ? PsiTypes.voidType() : psiType;
  }

  private static void addImplicitVariable(final PsiElement psiElement,
                                          @Nullable final String name,
                                          final List<JavaeeImplicitVariable> result,
                                          @Nullable final PsiType type,
                                          final PsiFile file) {

    if (name == null || name.length() == 0 || type == null) return;

    result.add(new ELImplicitVariable(file, name, type, psiElement, ELImplicitVariable.NESTED_RANGE));
  }
}
