package com.intellij.gwt.clientBundle;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.clientBundle.css.GwtCssDeclarationsManager;
import com.intellij.gwt.clientBundle.css.language.GwtCssLanguage;
import com.intellij.gwt.clientBundle.css.language.psi.GwtCssDef;
import com.intellij.gwt.clientBundle.jam.CssResourceClassJamElement;
import com.intellij.gwt.clientBundle.jam.CssResourceMethodJamElement;
import com.intellij.gwt.codeInsight.GwtMethodGenerationUtil;
import com.intellij.gwt.inspections.BaseGwtInspection;
import com.intellij.gwt.inspections.BaseGwtLocalQuickFixOnPsiElement;
import com.intellij.gwt.inspections.CreateCssClassLocalQuickFix;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.CssRulesetList;
import com.intellij.psi.css.CssStylesheet;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class CssResourceClassErrorsInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!shouldCheck(aClass)) return null;

    CssResourceClassJamElement element = CssResourceClassJamElement.getJamElement(aClass);
    if (element == null) return null;

    final Set<StylesheetFile> stylesheetFiles = element.findStylesheetFiles(false, false);
    final StylesheetFile mainStylesheetFile = ContainerUtil.getFirstItem(stylesheetFiles, null);
    if (mainStylesheetFile == null) {
      //todo highlight class?
      return null;
    }

    final MultiMap<String, CssClass> unusedCssClasses = new MultiMap<>();
    for (StylesheetFile stylesheetFile : stylesheetFiles) {
      GwtCssDeclarationsManager.collectDeclarations(stylesheetFile, CssClass.class, unusedCssClasses);
    }

    List<ProblemDescriptor> result = new ArrayList<>();
    for (CssResourceMethodJamElement methodElement : element.getCssMethods()) {
      final String className = methodElement.getCssClassName();
      unusedCssClasses.remove(className);

      if (methodElement.findCssElements().isEmpty()) {
        final PsiMethod method = methodElement.getPsiElement();
        if (aClass.getManager().areElementsEquivalent(aClass, method.getContainingClass())) {
          LocalQuickFix[] quickFixes;
          if (isOnTheFly) {
            quickFixes = new LocalQuickFix[]{
              new CreateCssClassLocalQuickFix(mainStylesheetFile, className),
              new CreateCssDefLocalQuickFix(className, mainStylesheetFile)
            };
          }
          else {
            quickFixes = LocalQuickFix.EMPTY_ARRAY;
          }
          final String message = GwtBundle.message("quickfix.name.0.css.class.or.def.element.not.found", className);

          final PsiElement place;
          final ProblemHighlightType type;
          final PsiLiteral literal = methodElement.getClassNameAttributeElement().getPsiLiteral();
          if (literal != null) {
            place = literal;
            type = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
          }
          else {
            place = getElementToHighlight(method);
            type = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
          }
          result.add(manager.createProblemDescriptor(place, message, isOnTheFly, quickFixes, type));
        }
      }
    }

    if (!unusedCssClasses.isEmpty()) {
      final Set<String> classNames = unusedCssClasses.keySet();
      String message;
      String quickFixName;
      if (classNames.size() == 1) {
        final String className = classNames.iterator().next();
        message = GwtBundle.message("inspection.message.css.class.0.does.not.have.corresponding.method", className);
        quickFixName = GwtBundle.message("quickfix.name.create.method.for.0", className);
      }
      else {
        message = GwtBundle.message("inspection.message.0.css.classes.do.not.have.corresponding.methods", classNames.size());
        quickFixName = GwtBundle.message("quickfix.name.create.missing.methods");
      }
      LocalQuickFix fix = new CreateMissingMethodsQuickFix(aClass, classNames, quickFixName);
      result.add(manager.createProblemDescriptor(getElementToHighlight(aClass), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
    }

    return result.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static class CreateMissingMethodsQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private final Set<String> myClassNames;

    CreateMissingMethodsQuickFix(PsiClass aClass, Set<String> classNames, @IntentionName String name) {
      super(GwtBundle.message("quickfix.family.name.create.missing.methods"), name, aClass);
      myClassNames = classNames;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiClass psiClass)) return;

      final LanguageLevel languageLevel = PsiUtil.getLanguageLevel(psiClass);
      for (String className : myClassNames) {
        final String methodName = GwtMethodGenerationUtil.convertStringToMethodName(className, PsiNameHelper.getInstance(project),
                                                                                    languageLevel, "class");
        final PsiMethod method = GwtMethodGenerationUtil.addStringMethod(psiClass, methodName);
        if (!methodName.equals(className)) {
          final String annotationText = "@" + ClientBundleUtil.CLASS_NAME_ANNOTATION + "(\"" + className + "\")";
          PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
          method.getModifierList().add(elementFactory.createAnnotationFromText(annotationText, psiClass));
        }

        CodeStyleManager.getInstance(project).reformat(method);
      }
    }
  }

  private static class CreateCssDefLocalQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private static final Logger LOG = Logger.getInstance(CreateCssDefLocalQuickFix.class);

    private final String myDefName;

    CreateCssDefLocalQuickFix(String defName, StylesheetFile cssFile) {
      super(GwtBundle.message("quickfix.family.name.create.def.declaration"), GwtBundle.message("quickfix.name.create.def.declaration.for.0", defName), cssFile);
      myDefName = defName;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof StylesheetFile cssFile)) return;

      VirtualFile virtualFile = cssFile.getOriginalFile().getVirtualFile();
      if (virtualFile == null) {
        return;
      }

      if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Collections.singletonList(virtualFile)).hasReadonlyFiles()) {
        return;
      }

      try {
        CssStylesheet stylesheet = cssFile.getStylesheet();
        final GwtCssDef cssDef = (GwtCssDef)stylesheet.getRulesetList().add(createCssDef(project, myDefName));
        final GwtCssDef processed = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(cssDef);
        Navigatable descriptor = PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile,
                                                                                      processed.getTextRange()
                                                                                               .getEndOffset() - 1);
        descriptor.navigate(true);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    private static GwtCssDef createCssDef(Project project, final String defName) {
      PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("_dummy_.css", GwtCssLanguage.GWT_CSS_LANGUAGE,
                                                                            "@def " + defName + ";");
      final CssRulesetList rulesetList = ((StylesheetFile)file).getStylesheet().getRulesetList();
      return PsiTreeUtil.findChildOfType(rulesetList, GwtCssDef.class);
    }
  }
}
