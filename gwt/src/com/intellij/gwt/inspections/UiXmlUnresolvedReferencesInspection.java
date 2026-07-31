package com.intellij.gwt.inspections;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.codeInsight.GwtMethodGenerationUtil;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.gwt.uiBinder.references.QualifiedUiXmlReference;
import com.intellij.gwt.uiBinder.references.UiXmlVariableReference;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class UiXmlUnresolvedReferencesInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!shouldCheck(file)) return null;

    if (file instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)file)) {
      final List<ProblemDescriptor> problems = new SmartList<>();
      file.accept(new XmlRecursiveElementVisitor() {
        @Override
        public void visitXmlAttributeValue(@NotNull XmlAttributeValue value) {
          checkReferences(value, problems, manager, isOnTheFly);
        }

        @Override
        public void visitXmlTag(@NotNull XmlTag tag) {
          checkReference(tag, problems, manager, isOnTheFly);
          super.visitXmlTag(tag);
        }
      });
      return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
    }
    return null;
  }

  private static void checkReferences(XmlAttributeValue value, List<ProblemDescriptor> problems, InspectionManager manager,
                                      boolean isOnTheFly) {
    for (PsiReference reference : value.getReferences()) {
      if (reference instanceof QualifiedUiXmlReference classRef) {
        if (classRef.multiResolve(false).length > 0) continue;


        final PsiClass qualifierClass = PsiTypesUtil.getPsiClass(classRef.getQualifierReference().resolveVariableType());
        String lastName = classRef.getValue();
        LocalQuickFix[] fixes = LocalQuickFix.EMPTY_ARRAY;
        final UiStyleElement styleElement = classRef.findStyleElement();
        if (styleElement != null) {
          List<StylesheetFile> cssFiles = styleElement.getStylesheetFiles();
          if (!cssFiles.isEmpty()) {
            fixes = new LocalQuickFix[]{new CreateCssClassLocalQuickFix(styleElement.getTag(), lastName)};
          }
        }
        else if (qualifierClass != null) {
          fixes = new LocalQuickFix[]{new CreateMissingMethodQuickFix(qualifierClass, lastName)};
        }

        final String message;
        if (qualifierClass != null) {
          message = GwtBundle.message("problem.description.cannot.resolve.symbol.0.in.1", lastName, qualifierClass.getName());
        }
        else {
          message = GwtBundle.message("problem.description.cannot.resolve.0", lastName);
        }
        problems.add(manager.createProblemDescriptor(value, classRef.getRangeInElement(), message,
                                                     ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, isOnTheFly, fixes));
      }
      else if (reference instanceof UiXmlVariableReference variableReference) {
        if (reference.resolve() != null) continue;

        final String message = GwtBundle.message("problem.description.cannot.resolve.0", variableReference.getValue());
        problems.add(manager.createProblemDescriptor(value, reference.getRangeInElement(), message,
                                                      ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, isOnTheFly));
      }
    }
  }

  private static void checkReference(XmlTag tag, List<ProblemDescriptor> problems, InspectionManager manager, boolean isOnTheFly) {
    String namespace = tag.getNamespace();
    if (namespace.startsWith(UiBinderUtil.URN_IMPORT_PREFIX)) {
      PsiReference reference = tag.getReference();
      if (reference == null) return;

      String packageName = StringUtil.trimStart(namespace, UiBinderUtil.URN_IMPORT_PREFIX);
      String simpleName = tag.getLocalName();

      PsiElement resolvedReference = reference.resolve();
      if (resolvedReference == null || resolvedReference == tag) {
        String message = GwtBundle.message("problem.description.cannot.resolve.symbol.0.in.1", simpleName, packageName);
        problems.add(manager.createProblemDescriptor(tag, reference.getRangeInElement(), message,
                                                     ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, isOnTheFly));
      }
      else {
        if (resolvedReference instanceof PsiClass psiClass) {
          if (!InheritanceUtil.isInheritor(psiClass, UiBinderUtil.WIDGET_BASE_CLASS)) {
            String message = GwtBundle.message("problem.description.class.is.not.widget", simpleName);
            problems.add(manager.createProblemDescriptor(tag, reference.getRangeInElement(), message,
                                                         ProblemHighlightType.GENERIC_ERROR, isOnTheFly));
          }
        }
      }
    }
  }

  private static final class CreateMissingMethodQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final String myMethodName;

    private CreateMissingMethodQuickFix(PsiClass psiClass, String methodName) {
      super(GwtBundle.message("quickfix.family.name.create.missing.methods"), GwtBundle.message("quickfix.name.create.0.method", methodName), psiClass);
      myMethodName = methodName;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass psiClass)) return;

      if (!FileModificationService.getInstance().preparePsiElementForWrite(startElement.getContainingFile())) return;

      final PsiMethod method = GwtMethodGenerationUtil.addStringMethod(psiClass, myMethodName);
      CodeStyleManager.getInstance(project).reformat(method);
    }
  }
}
