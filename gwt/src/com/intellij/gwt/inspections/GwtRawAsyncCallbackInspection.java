package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GwtRawAsyncCallbackInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkClass(final @NotNull PsiClass aClass, final @NotNull InspectionManager manager,
                                        final boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null || !gwtFacet.getSdkVersion().isGenericsSupported()) {
      return null;
    }

    PsiClass sync = RemoteServiceUtil.findSynchronousInterface(aClass);
    if (sync != null) {
      return checkAsynchronousInterface(aClass, sync, manager, isOnTheFly);
    }

    final List<ProblemDescriptor> problems = new SmartList<>();
    JavaRecursiveElementWalkingVisitor visitor = new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitReferenceExpression(final @NotNull PsiReferenceExpression expression) {
      }

      @Override
      public void visitMethodCallExpression(final @NotNull PsiMethodCallExpression expression) {
        PsiMethod method = expression.resolveMethod();
        if (method != null) {
          PsiClass async = method.getContainingClass();
          PsiClass sync = RemoteServiceUtil.findSynchronousInterface(async);
          if (sync != null) {
            checkAsyncMethod(method, sync, manager, problems, expression, isOnTheFly);
          }
        }
      }
    };
    aClass.accept(visitor);

    return problems.isEmpty() ? null : problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static ProblemDescriptor[] checkAsynchronousInterface(final @NotNull PsiClass async, final @NotNull PsiClass sync, final InspectionManager manager,
                                                                boolean isOnTheFly) {
    final List<ProblemDescriptor> problems = new ArrayList<>();

    for (PsiMethod method : async.getMethods()) {
      checkAsyncMethod(method, sync, manager, problems, null, isOnTheFly);
    }

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static void checkAsyncMethod(final PsiMethod method, final PsiClass sync, final InspectionManager manager, final List<ProblemDescriptor> problems,
                                       final @Nullable PsiMethodCallExpression expression,
                                       boolean onTheFly) {
    PsiParameter[] parameters = method.getParameterList().getParameters();
    if (parameters.length == 0) {
      return;
    }


    PsiParameter lastParameter = parameters[parameters.length - 1];
    PsiType type = lastParameter.getType();
    if (!(type instanceof PsiClassType classType)) {
      return;
    }

    PsiClass psiClass = classType.resolve();
    if (psiClass == null || !RemoteServiceUtil.ASYNC_CALLBACK_INTERFACE_NAME.equals(psiClass.getQualifiedName())) {
      return;
    }

    PsiMethod syncMethod = RemoteServiceUtil.findMethodInSync(method, sync);
    if (syncMethod == null) return;
    PsiType returnType = syncMethod.getReturnType();
    if (PsiTypes.voidType().equals(returnType) || returnType == null) return;

    PsiAnonymousClass rawAnonymous = null;
    if (expression != null) {
      PsiExpression[] arguments = expression.getArgumentList().getExpressions();
      if (arguments.length == parameters.length) {
        PsiExpression lastArg = arguments[arguments.length - 1];
        if (lastArg instanceof PsiNewExpression) {
          PsiAnonymousClass anonymousClass = ((PsiNewExpression)lastArg).getAnonymousClass();
          if (anonymousClass != null) {
            final PsiReferenceParameterList parameterList = anonymousClass.getBaseClassReference().getParameterList();
            if (parameterList != null && parameterList.getTypeParameterElements().length == 0) {
              rawAnonymous = anonymousClass;
            }
          }
        }
      }
    }


    if (classType.isRaw() || rawAnonymous != null) {
      final PsiMethod methodToFix = classType.isRaw() ? method : null;
      LocalQuickFix fix = (methodToFix != null && rawAnonymous != null)
                        ? new GenerifyAsyncCallbackFix(returnType, methodToFix, rawAnonymous)
                        : new GenerifyAsyncCallbackFix(returnType, methodToFix != null ? methodToFix : rawAnonymous);
      final String methodDescription = PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY,
                                                                  PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS,
                                                                  PsiFormatUtilBase.SHOW_TYPE);
      final String message = GwtBundle.message("problem.description.raw.use.of.async.callback.interface", methodDescription);
      PsiElement place = rawAnonymous != null ? getElementToHighlight(rawAnonymous) : expression != null ? expression : lastParameter;
      problems.add(manager.createProblemDescriptor(place, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, onTheFly));
    }
  }

  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  private static final class GenerifyAsyncCallbackFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final PsiType myType;

    private GenerifyAsyncCallbackFix(final @NotNull PsiType type, final @NotNull PsiElement element) {
      super(getFamilyNameText(), GwtBundle.message("quickfix.name.replace.async.callback.by.async.callback.0", type.getCanonicalText()), element);
      myType = type;
    }

    private GenerifyAsyncCallbackFix(final @NotNull PsiType type, final @NotNull PsiMethod methodToFix, final @NotNull PsiAnonymousClass anonymousToFix) {
      super(getFamilyNameText(), GwtBundle.message("quickfix.name.replace.async.callback.by.async.callback.0", type.getCanonicalText()), methodToFix, anonymousToFix);
      myType = type;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      PsiMethod methodToFix = ContainerUtil.findInstance(Arrays.asList(startElement, endElement), PsiMethod.class);
      PsiAnonymousClass anonymousToFix = ContainerUtil.findInstance(Arrays.asList(startElement, endElement), PsiAnonymousClass.class);
      if (methodToFix == null && anonymousToFix == null) return;

      List<VirtualFile> affectedFiles = new ArrayList<>();
      if (methodToFix != null) {
        affectedFiles.add(methodToFix.getContainingFile().getVirtualFile());
      }
      if (anonymousToFix != null) {
        affectedFiles.add(anonymousToFix.getContainingFile().getVirtualFile());
      }
      if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(affectedFiles).hasReadonlyFiles()) {
        return;
      }

      try {
        PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
        if (methodToFix != null) {
          generifyMethod(methodToFix, myType, elementFactory);
        }
        if (anonymousToFix != null) {
          generifyAnonymous(anonymousToFix, myType, project, elementFactory);
        }
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    private static void generifyAnonymous(final @NotNull PsiAnonymousClass anonymous, final @NotNull PsiType type, final Project project,
                                          final PsiElementFactory elementFactory)
        throws IncorrectOperationException {
      PsiReferenceParameterList list = anonymous.getBaseClassReference().getParameterList();
      if (list != null) {
        list.add(elementFactory.createTypeElement(type));
      }

      PsiMethod[] methods = anonymous.findMethodsByName("onSuccess", false);
      for (PsiMethod method : methods) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length == 1) {
          PsiParameter parameter = parameters[0];
          if (PsiType.getJavaLangObject(PsiManager.getInstance(project), anonymous.getResolveScope()).equals(parameter.getType())) {
            parameter.getTypeElement().replace(elementFactory.createTypeElement(type));
            break;
          }
        }
      }
    }

    private static void generifyMethod(final @NotNull PsiMethod method, final @NotNull PsiType type, final PsiElementFactory elementFactory) throws IncorrectOperationException {
      PsiParameter[] parameters = method.getParameterList().getParameters();
      if (parameters.length == 0) return;

      PsiParameter last = parameters[parameters.length - 1];
      last.getTypeElement().replace(elementFactory.createTypeElement(RemoteServiceUtil.createAsyncCallbackType(method, type)));
    }

    private static @IntentionFamilyName String getFamilyNameText() {
      return GwtBundle.message("quickfix.family.name.replace.async.callback.by.generic.async.callback");
    }
  }
}
