package com.intellij.gwt.inspections;

import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.gwt.uiBinder.references.GwtUiFieldFromHandlerReference;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GwtUiHandlerErrorsInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null) return null;

    List<ProblemDescriptor> problems = new SmartList<>();
    for (PsiMethod method : aClass.getMethods()) {
      final PsiAnnotation annotation = method.getModifierList().findAnnotation(UiBinderUtil.UI_HANDLER_ANNOTATION);
      if (annotation == null) continue;

      checkReferencesToUiFields(annotation, manager, isOnTheFly, problems);
      final String methodDescription = PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS, PsiFormatUtilBase.SHOW_TYPE);
      if (method.hasModifierProperty(PsiModifier.PRIVATE)) {
        LocalQuickFix fix = IntentionManager.getInstance().convertToFix(QuickFixFactory.getInstance().createModifierListFix(method, PsiModifier.PRIVATE, false, false));
        problems.add(manager.createProblemDescriptor(getElementToHighlight(method), GwtBundle
                                                       .message("problem.descriptor.description.template.uihandler.0.should.not.be.private", methodDescription),
                                                     fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
        continue;
      }

      final PsiParameter[] parameters = method.getParameterList().getParameters();
      if (parameters.length != 1) {
        UiBinderMappingService mappingService = UiBinderMappingService.getInstance(gwtFacet.getModule());
        if (mappingService.isUiRendererComponent(aClass)) {
          if (parameters.length == 0) {
            problems.add(manager.createProblemDescriptor(getElementToHighlight(method),
                                                         GwtBundle.message(
                                                           "problem.descriptor.description.template.uihandler.0.must.have.at.least.one.parameter.defined",
                                                           methodDescription),
                isOnTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
            continue;
          }
        }
        else {
          problems.add(manager.createProblemDescriptor(getElementToHighlight(method),
                                                       GwtBundle.message(
                                                         "problem.descriptor.description.template.uihandler.0.must.have.a.single.event.parameter.defined",
                                                         methodDescription),
               isOnTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
          continue;
        }
      }

      final PsiClass gwtEventClass = JavaPsiFacade.getInstance(manager.getProject()).findClass(UiBinderUtil.GWT_EVENT_CLASS, method.getResolveScope());
      if (gwtEventClass == null) continue;
      final PsiTypeParameter[] typeParameters = gwtEventClass.getTypeParameters();
      if (typeParameters.length != 1) continue;

      final PsiParameter parameter = parameters[0];
      final PsiType type = parameter.getType();
      final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(manager.getProject());
      final PsiClassType gwtEventType = elementFactory.createType(gwtEventClass);
      if (!TypeConversionUtil.isAssignable(gwtEventType, type)) {
        problems.add(manager.createProblemDescriptor(parameter, GwtBundle
                                                       .message("problem.descriptor.description.template.parameter.0.is.not.an.event.subclass.of.gwtevent", parameter.getName()),
                                                     isOnTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        continue;
      }

      final PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(gwtEventClass, (PsiClassType)type);
      final PsiType handlerType = substitutor.substitute(typeParameters[0]);
      if (!(handlerType instanceof PsiClassType)) {
        problems.add(manager.createProblemDescriptor(getElementToHighlight(method), GwtBundle
                                                       .message("problem.descriptor.description.template.cannot.get.eventhandler.type.for.02", type.getCanonicalText()),
                                                     isOnTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        continue;
      }

      final PsiClass handlerClass = ((PsiClassType)handlerType).resolve();
      if (handlerClass == null || handlerClass instanceof PsiTypeParameter) {
        problems.add(manager.createProblemDescriptor(getElementToHighlight(method), GwtBundle
                                                       .message("problem.descriptor.description.template.cannot.get.eventhandler.type.for.0", type.getCanonicalText()),
                                                     isOnTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        continue;
      }

      final PsiMethod[] handlerMethods = handlerClass.getMethods();
      if (handlerMethods.length != 1) {
        String message = GwtBundle.message("problem.descriptor.description.template.handler.class.0.1", handlerClass.getQualifiedName(), handlerMethods.length);
        problems.add(manager.createProblemDescriptor(getElementToHighlight(method), message, isOnTheFly, LocalQuickFix.EMPTY_ARRAY,
                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        continue;
      }

      final PsiParameter[] handlerMethodParameters = handlerMethods[0].getParameterList().getParameters();
      if (handlerMethodParameters.length != 1 || !TypeConversionUtil.erasure(handlerMethodParameters[0].getType()).equals(TypeConversionUtil.erasure(type))) {
        final String handlerMethodInfo = PsiFormatUtil
          .formatMethod(handlerMethods[0], PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME, PsiFormatUtilBase.SHOW_CONTAINING_CLASS, 0);
        final String currentMethodInfo =
          PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME, PsiFormatUtilBase.SHOW_CONTAINING_CLASS, 0);
        problems.add(manager.createProblemDescriptor(getElementToHighlight(method), GwtBundle
                                                       .message("problem.descriptor.description.template.handler.method.0.signature.does.not.match.1.signature", handlerMethodInfo,
                                                                currentMethodInfo),
                                                     isOnTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        continue;
      }

      PsiType handlerRegistration = elementFactory.createTypeByFQClassName(UiBinderUtil.HANDLER_REGISTRATION_INTERFACE, method.getResolveScope());
      for (GwtUiFieldFromHandlerReference reference : collectFieldReferences(annotation)) {
        final PsiElement resolved = reference.resolve();
        if (resolved instanceof PsiField field) {
          final PsiType fieldType = field.getType();
          if (fieldType instanceof PsiClassType) {
            final List<String> addHandlerMethods = findAddHandlerMethods((PsiClassType)fieldType, handlerType, handlerRegistration);
            final PsiLiteralExpression literalExpression = reference.getElement();
            if (addHandlerMethods.isEmpty()) {
              problems.add(manager.createProblemDescriptor(literalExpression, getReferenceRange(literalExpression),
                                                           GwtBundle.message(
                                                             "problem.descriptor.description.template.field.0.does.not.have.addhandler.method.for.1",
                                                             field.getName(), handlerType.getCanonicalText()),
                                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
            }
            else if (addHandlerMethods.size() >= 2) {
              String method1 = addHandlerMethods.get(0);
              String method2 = addHandlerMethods.get(1);
              problems.add(manager.createProblemDescriptor(literalExpression, getReferenceRange(literalExpression), GwtBundle
                                                             .message("problem.descriptor.description.template.handler.0.cannot.be.registered.methods.1.and.2.are.ambiguous",
                                                                      method.getName(), method1, method2),
                                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
            }
          }
        }
      }

    }

    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  public static List<String> findAddHandlerMethods(@NotNull PsiClassType fieldType, @NotNull PsiType handlerType, @NotNull PsiType handlerRegistration) {
    final PsiClassType.ClassResolveResult resolveResult = fieldType.resolveGenerics();
    final PsiClass fieldClass = resolveResult.getElement();
    if (fieldClass == null) return Collections.emptyList();

    List<String> result = new SmartList<>();
    Set<MethodSignature> signatures = new HashSet<>();
    for (Pair<PsiMethod, PsiSubstitutor> pair : fieldClass.getAllMethodsAndTheirSubstitutors()) {
      final PsiMethod method = pair.getFirst();
      final MethodSignature signature = method.getSignature(MethodSignatureUtil.combineSubstitutors(pair.second, resolveResult.getSubstitutor()));
      if (!signatures.add(signature)) continue;

      final PsiType[] parameters = signature.getParameterTypes();
      if (handlerRegistration.equals(method.getReturnType()) && parameters.length == 1 && handlerType.equals(parameters[0])) {
        result.add(PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_CONTAINING_CLASS
                                                                            | PsiFormatUtilBase.SHOW_PARAMETERS, PsiFormatUtilBase.SHOW_TYPE));
      }
    }

    return result;
  }

  private static List<GwtUiFieldFromHandlerReference> collectFieldReferences(@NotNull PsiAnnotation annotation) {
    final List<GwtUiFieldFromHandlerReference> result = new SmartList<>();
    annotation.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitLiteralExpression(@NotNull PsiLiteralExpression expression) {
        final PsiReference[] references = expression.getReferences();
        for (PsiReference reference : references) {
          if (reference instanceof GwtUiFieldFromHandlerReference) {
            result.add((GwtUiFieldFromHandlerReference)reference);
          }
        }
      }

      @Override
      public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
        visitExpression(expression);
      }
    });
    return result;
  }

  private static void checkReferencesToUiFields(@NotNull PsiAnnotation annotation, final InspectionManager manager, final boolean onTheFly,
                                                final List<ProblemDescriptor> problems) {
    for (GwtUiFieldFromHandlerReference reference : collectFieldReferences(annotation)) {
      if (reference.resolve() == null) {
        final PsiLiteralExpression expression = reference.getElement();
        TextRange range = getReferenceRange(expression);
        problems.add(manager.createProblemDescriptor(expression, range, GwtBundle
          .message("problem.descriptor.description.template.cannot.resolve.0.field", expression.getValue()), ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, onTheFly));
      }
    }
  }

  private static TextRange getReferenceRange(PsiLiteralExpression expression) {
    final String text = expression.getText();
    int start = text.indexOf('\"');
    int end = text.lastIndexOf('\"');
    if (start != -1 && end != -1) {
      return new TextRange(start + 1, end);
    }
    return new TextRange(0, expression.getTextLength());
  }
}
