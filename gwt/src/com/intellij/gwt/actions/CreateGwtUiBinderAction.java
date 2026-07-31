package com.intellij.gwt.actions;

import com.intellij.CommonBundle;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.gwt.uiBinder.GwtHtmlElementClassesFinder;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateGwtUiBinderAction extends GwtCreateActionBase {
  private static final Logger LOG = Logger.getInstance(CreateGwtUiBinderAction.class);
  private final Ref<String> myRootElementType = new Ref<>(null);

  @Override
  protected void showDialog(@NotNull GwtFacet facet, @NotNull PsiDirectory directory, @NotNull MyInputValidator validator) {
    final Project project = facet.getModule().getProject();
    if (!facet.getSdkVersion().isUiBinderSupported()) {
      Messages.showErrorDialog(project, GwtBundle.message("error.message.uibinder.is.supported.in.gwt.2.0.or.later"), CommonBundle.getErrorTitle());
      return;
    }
    final CreateGwtUiBinderDialog dialog = new CreateGwtUiBinderDialog(project, facet, getDialogTitle(), validator, myRootElementType);
    dialog.show();
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String name, PsiDirectory directory, GwtModule gwtModule) throws Exception {
    final Project project = directory.getProject();
    GwtFacet facet = GwtFacet.findFacetBySourceFile(project, directory.getVirtualFile());
    LOG.assertTrue(facet != null, "GWT Facet not found for " + directory.getVirtualFile());
    final PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
    String packageName = psiPackage != null ? psiPackage.getQualifiedName() : "";

    String rootElementTypeName = myRootElementType.get();
    @NonNls String rootTagName = "div";
    final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(facet.getModule());
    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
    PsiClass rootElementType = psiFacade.findClass(rootElementTypeName, scope);
    boolean isWidget;
    if (rootElementType != null) {
      final List<String> tagNames = GwtHtmlElementClassesFinder.getTagNames(rootElementType);
      if (!tagNames.isEmpty()) {
        rootTagName = tagNames.get(0);
      }

      if (InheritanceUtil.isInheritor(rootElementType, true, UiBinderUtil.WIDGET_BASE_CLASS)) {
        rootTagName = "g:" + rootElementType.getName();
      }
      isWidget = InheritanceUtil.isInheritor(rootElementType, false, UiBinderUtil.WIDGET_CLASS);
    }
    else {
      isWidget = false;
    }

    String qualifiedClassName = !packageName.isEmpty() ? packageName + "." + name : name;
    PsiClass binderClass = createClassFromTemplate(directory, name, JavaFileType.INSTANCE, GwtTemplates.UI_BINDER_JAVA,
                                                   "QUALIFIED_NAME", qualifiedClassName,
                                                   "ROOT_ELEMENT_TYPE", rootElementTypeName,
                                                   "IS_WIDGET", isWidget);
    PsiElement binderFile = JavaCodeStyleManager.getInstance(project).shortenClassReferences(binderClass.getContainingFile());
    final PsiFile uiXml = createFromTemplateInternal(directory, name, name + UiBinderUtil.UI_XML_SUFFIX, XmlFileType.INSTANCE,
                                                     GwtTemplates.LAYOUT_UI_XML, "ROOT_TAG_NAME", rootTagName);
    return new PsiElement[] {binderFile, uiXml};
  }


  @Override
  protected boolean requireGwtModule() {
    return true;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("label.text.enter.name.for.new.gwt.uibinder.class.and.ui.xml.file");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("dialog.title.create.new.gwt.uibinder.class.and.ui.xml.file");
  }

  @Override
  public @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GwtBundle.message("action.name.create.gwt.uibinder.0.and.1.ui.xml.file", newName, newName);
  }
}
