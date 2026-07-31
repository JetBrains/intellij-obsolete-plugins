package com.intellij.gwt.inspections;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.gwt.uiBinder.references.QualifiedUiXmlReference;
import com.intellij.gwt.uiBinder.references.QualifiedUiXmlReferenceProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_BINDER_NAMESPACE;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_STYLE_TAG;
import static com.intellij.util.containers.ContainerUtil.findInstance;

public class CreateLocalCssClassLocalQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private final String myClassName;

  CreateLocalCssClassLocalQuickFix(XmlAttributeValue element, String className) {
    super(element);
    this.myClassName = className;
  }

  @Override
  public @NotNull String getText() {
    return GwtBundle.message("quickfix.name.create.local.css.class.0", myClassName);
  }

  @Override
  public @NotNull String getFamilyName() {
    return GwtBundle.message("quickfix.family.name.create.local.css.class");
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor,
                     @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
    XmlAttribute attribute = (XmlAttribute)startElement.getParent();

    attribute.setValue("{" + UI_STYLE_TAG + "." + myClassName + "}");
    XmlAttributeValue attributeValue = attribute.getValueElement();

    //noinspection ConstantConditions
    PsiReference[] references = new QualifiedUiXmlReferenceProvider().getReferencesByElement(attributeValue, new ProcessingContext());
    QualifiedUiXmlReference uiXmlReference = findInstance(references, QualifiedUiXmlReference.class);

    if (uiXmlReference == null || uiXmlReference.multiResolve(false).length > 0) {
      return;
    }

    UiStyleElement uiStyleElement = uiXmlReference.findStyleElement();
    if (uiStyleElement == null) {
      XmlTag rootTag = ((XmlFile)attributeValue.getContainingFile()).getRootTag();
      if (rootTag == null) return;

      rootTag.addSubTag(rootTag.createChildTag(UI_STYLE_TAG, UI_BINDER_NAMESPACE, "\n." + myClassName + "{\n\n}\n", false), true);
    }
    else {
      List<StylesheetFile> stylesheetFiles = uiStyleElement.getStylesheetFiles();
      if (stylesheetFiles.isEmpty()) {
        XmlTagValue styleValue = uiStyleElement.getTag().getValue();
        styleValue.setText(styleValue.getText() + "\n." + myClassName + "{\n\n}\n");
      }
      else {
        new CreateCssClassLocalQuickFix(uiStyleElement.getTag(), myClassName).invoke(project, editor, psiFile);
      }
    }
  }
}
