package com.intellij.gwt.actions;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderFactory;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.codeInsight.template.TemplateEditingListener;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public final class CreateGwtUiRendererAction extends GwtCreateActionBase {

  private static final Logger LOG = Logger.getInstance(CreateGwtUiRendererAction.class);
  private static final @NonNls String ELEMENT_TYPE = "ELEMENT_TYPE";

  @Override
  protected void showDialog(@NotNull GwtFacet facet, @NotNull PsiDirectory directory, @NotNull MyInputValidator validator) {
    Project project = facet.getModule().getProject();
    if (!facet.getSdkVersion().isUiRendererSupported()) {
      Messages.showErrorDialog(project, GwtBundle.message("error.message.uirenderer.is.supported.in.gwt.2.5.or.later"),
                               CommonBundle.getErrorTitle());
      return;
    }
    super.showDialog(facet, directory, validator);
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String name, PsiDirectory directory, GwtModule gwtModule) {
    return new PsiElement[]{doCreate0(name, directory)};
  }

  @Override
  protected boolean requireGwtModule() {
    return true;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("label.text.enter.name.for.new.gwt.uirenderer.class.and.ui.xml.file");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("dialog.title.create.new.gwt.uirenderer.class.and.ui.xml.file");
  }

  @Override
  public @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GwtBundle.message("action.name.create.gwt.uirenderer.0.and.1.ui.xml.file", newName, newName);
  }

  @VisibleForTesting
  public static @NotNull PsiFile doCreate0(String name, PsiDirectory directory) {
    PsiClass rendererClass = GwtCreateActionBase.createClassFromTemplate(directory, name, JavaFileType.INSTANCE, GwtTemplates.UI_RENDERER_JAVA);
    PsiFile rendererPsiFile = rendererClass.getContainingFile();
    Project project = directory.getProject();

    OpenFileDescriptor descriptor = new OpenFileDescriptor(project, rendererPsiFile.getVirtualFile());
    Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    if (editor != null) {
      PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

      PsiTypeElement parameter = getInheritedGenericParameter(rendererClass);
      //noinspection ConstantConditions
      editor.getCaretModel().moveToOffset(parameter.getNavigationElement().getTextOffset());

      //TODO can we avoid this cast?
      TemplateBuilderImpl templateBuilder =
        (TemplateBuilderImpl)TemplateBuilderFactory.getInstance().createTemplateBuilder(parameter);
      templateBuilder.replaceElement(parameter, "Object");

      Template template = templateBuilder.buildInlineTemplate();
      TemplateManager.getInstance(project).startTemplate(editor, template, false, null,
                                                         getTemplateFinishingCallback(name, directory, editor, rendererClass));
    }
    else {
      LOG.error("Text editor for file " + rendererPsiFile.getName() + " not found");
    }
    return rendererPsiFile;
  }

  private static @NotNull TemplateEditingListener getTemplateFinishingCallback(final String name, final PsiDirectory directory,
                                                                               final Editor editor, final PsiClass rendererClass) {
    return new TemplateEditingAdapter() {

      @Override
      public void templateFinished(@NotNull Template template, boolean brokenOff) {
        if (!brokenOff) {
          WriteAction.run(() -> {
            PsiTypeElement element = getInheritedGenericParameter(rendererClass);

            // "element" is null if user erased generic type from superclass
            String elementTypeName = element != null ? element.getText() : "Object";
            String elementQualifiedName = element != null ? element.getType().getCanonicalText() : "java.lang.Object";

            @NonNls String text = getTextByTemplate(directory, name, GwtTemplates.UI_RENDERER_CONTENT_JAVA, ELEMENT_TYPE, elementTypeName);

            // "onBrowserEvent" method won't compile if generic type is absent in superclass
            if (element == null) text = text.replace("<Object>", "");

            PsiElement rightBrace = rendererClass.getRBrace();

            // user deleted part of the class
            if (rightBrace == null) return;

            int offset = rightBrace.getTextOffset();
            editor.getCaretModel().moveToOffset(offset);
            EditorModificationUtil.insertStringAtCaret(editor, text);
            CodeStyleManager.getInstance(directory.getProject()).reformatText(rendererClass.getContainingFile(),
                                                                              offset, offset + text.length());

            createFromTemplateInternal(directory, name, name + UiBinderUtil.UI_XML_SUFFIX, XmlFileType.INSTANCE,
                                       GwtTemplates.UI_RENDERER_LAYOUT_UI_XML, ELEMENT_TYPE, elementQualifiedName);
          });
        }
      }
    };
  }

  private static @Nullable PsiTypeElement getInheritedGenericParameter(PsiClass psiClass) {
    PsiReferenceList extendsList = psiClass.getExtendsList();
    if (extendsList == null) return null;

    PsiJavaCodeReferenceElement[] extendsListReferences = extendsList.getReferenceElements();
    if (extendsListReferences.length != 1) return null;

    PsiJavaCodeReferenceElement superClassReference = extendsListReferences[0];
    PsiReferenceParameterList parametersList = superClassReference.getParameterList();
    if (parametersList == null) return null;

    PsiTypeElement[] superClassTypeParameters = parametersList.getTypeParameterElements();
    if (superClassTypeParameters.length != 1) return null;

    return superClassTypeParameters[0];
  }
}
