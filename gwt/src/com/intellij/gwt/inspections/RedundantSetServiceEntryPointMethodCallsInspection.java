package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.patterns.PsiMethodCallPattern;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiBinaryExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiClass;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.psiExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiExpressionStatement;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;
import static com.intellij.patterns.PsiJavaPatterns.psiType;
import static com.intellij.patterns.PsiJavaPatterns.psiTypeCastExpression;
import static com.intellij.patterns.PsiJavaPatterns.string;

public final class RedundantSetServiceEntryPointMethodCallsInspection extends BaseGwtInspection {
  private static final Key<String> SERVICE_PATH_KEY = Key.create("GWT_SERVICE_PATH");
  private static final Key<PsiClass> GWT_ASYNC_SERVICE_CLASS_KEY = Key.create("GWT_ASYNC_SERVICE_CLASS");
  private static final PsiMethodCallPattern SET_SERVICE_ENTRY_POINTS_CALL_PATTERN;

  static {
    PsiMethodPattern setEntryPointMethod =
        psiMethod().withName("setServiceEntryPoint").definedInClass("com.google.gwt.user.client.rpc.ServiceDefTarget");
    PsiMethodCallPattern getModuleBaseUrlCall = psiExpression()
        .methodCall(psiMethod().withName("getModuleBaseURL").definedInClass(GwtSdkUtil.GWT_CLASS_NAME));
    SET_SERVICE_ENTRY_POINTS_CALL_PATTERN =
        psiExpression().methodCall(setEntryPointMethod)
            .withArguments(psiBinaryExpression()
                .operation(psiElement(JavaTokenType.PLUS))
                .left(getModuleBaseUrlCall)
                .right(literalExpression(string().save(SERVICE_PATH_KEY))))
            .withQualifier(psiExpression().skipParentheses(
                psiTypeCastExpression()
                    .withOperand(psiExpression().ofType(psiType().classType(psiClass().save(GWT_ASYNC_SERVICE_CLASS_KEY))))))
            .withParent(psiExpressionStatement());
  }


  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    GwtFacet facet = getFacet(file);
    if (facet == null || !facet.getSdkVersion().isGenericsSupported()) {
      return null;
    }

    final List<ProblemDescriptor> problems = new ArrayList<>();
    file.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitDocComment(@NotNull PsiDocComment comment) {
      }

      @Override
      public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
        final ProcessingContext context = new ProcessingContext();
        if (SET_SERVICE_ENTRY_POINTS_CALL_PATTERN.accepts(expression, context)) {
          final String servicePath = context.get(SERVICE_PATH_KEY);
          final PsiClass asyncInterface = context.get(GWT_ASYNC_SERVICE_CLASS_KEY);
          final PsiClass serviceInterface = RemoteServiceUtil.findSynchronousInterface(asyncInterface);
          if (serviceInterface != null) {
            checkSetServiceEntryPointCall(expression, servicePath, serviceInterface, problems, manager, isOnTheFly);
          }
        }
        super.visitMethodCallExpression(expression);
      }
    });
    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static void checkSetServiceEntryPointCall(PsiMethodCallExpression expression, String servicePath, PsiClass serviceInterface, List<ProblemDescriptor> problems,
                                                    InspectionManager manager,
                                                    boolean onTheFly) {
    String message =
        GwtBundle.message("error.message.set.service.entry.point.method.call.can.be.replaced.by.remote.service.relative.path.annotation");
    final PsiModifierList list = serviceInterface.getModifierList();
    boolean addAnnotation = true;
    if (list != null) {
      final PsiAnnotation annotation = list.findAnnotation(RemoteServiceUtil.SERVICE_PATH_ANNOTATION_NAME);
      if (annotation != null) {
        final PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        if (attributes.length == 1) {
          final PsiAnnotationMemberValue value = attributes[0].getValue();
          if (value instanceof PsiLiteralExpression && servicePath.equals(((PsiLiteralExpression)value).getValue())) {
            message = GwtBundle.message("error.message.set.service.entry.point.method.call.is.redundant");
            addAnnotation = false;
          }
          else {
            return;
          }
        }
        else {
          return;
        }
      }
    }
    LocalQuickFix fix = new ReplaceByAnnotationQuickFix(expression, addAnnotation, serviceInterface, servicePath);
    problems.add(manager.createProblemDescriptor(expression, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, onTheFly));
  }

  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  private static final class ReplaceByAnnotationQuickFix extends BaseGwtLocalQuickFix {
    private final SmartPsiElementPointer<PsiMethodCallExpression> myExpression;
    private final boolean myAddAnnotation;
    private final SmartPsiElementPointer<PsiClass> myServiceInterface;
    private final String myServicePath;

    private ReplaceByAnnotationQuickFix(PsiMethodCallExpression expression, boolean addAnnotation, PsiClass serviceInterface,
                                        String servicePath) {
      super(addAnnotation ? GwtBundle.message("quickfix.name.replace.set.service.entry.point.call.by.annotation")
                          : GwtBundle.message("quickfix.name.remove.redundant.set.service.entry.point.call"));
      myExpression = SmartPointerManager.createPointer(expression);
      myAddAnnotation = addAnnotation;
      myServiceInterface = SmartPointerManager.createPointer(serviceInterface);
      myServicePath = servicePath;
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      PsiMethodCallExpression expression = myExpression.getElement();
      PsiClass serviceInterface = myServiceInterface.getElement();
      if (expression == null || serviceInterface == null) return;

      if (myAddAnnotation &&
          ReadonlyStatusHandler.getInstance(project)
            .ensureFilesWritable(Collections.singletonList(serviceInterface.getContainingFile().getVirtualFile())).hasReadonlyFiles()) {
        return;
      }
      expression.getParent().delete();
      if (myAddAnnotation) {
        final String annotationText = "@" + RemoteServiceUtil.SERVICE_PATH_ANNOTATION_NAME + "(\"" + myServicePath + "\")";
        final PsiAnnotation annotation =
            JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(annotationText, serviceInterface);
        final PsiModifierList modifierList = serviceInterface.getModifierList();
        if (modifierList != null) {
          final PsiElement added = modifierList.addBefore(annotation, modifierList.getFirstChild());
          JavaCodeStyleManager.getInstance(project).shortenClassReferences(added);
        }
      }
    }
  }

}
