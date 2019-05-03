/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.designer.inspector.impl;

import com.intellij.designer.inspector.Property;
import com.intellij.designer.inspector.PropertyEditor;
import com.intellij.designer.inspector.RenderingContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author mike
 */
public abstract class EditorTextFieldPropertyEditor<P extends Property> implements PropertyEditor<P> {
  private static final Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(1, 5, 1, 5);
  private final Project myProject;
  private PsiFile myPsiFile;
  private final EditorTextField myEditorTextField;
  private final JPanel myWrappingPanel;
  private P myProperty;

  public EditorTextFieldPropertyEditor(Project project, final FileType fileType) {
    myProject = project;
    myWrappingPanel = new JPanel();
    myWrappingPanel.setLayout(new BorderLayout());
    myEditorTextField = createEditorTextField(project, fileType);
    myWrappingPanel.add(myEditorTextField, BorderLayout.SOUTH);
  }

  protected Project getProject() {
    return myProject;
  }

  protected JComponent getWrappingPanel() {
    return myWrappingPanel;
  }

  protected EditorTextField getEditorTextField() {
    return myEditorTextField;
  }

  private static EditorTextField createEditorTextField(final Project project, final FileType fileType) {
    final EditorTextField result = new EditorTextField("", project, fileType);
    result.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

    result.setFontInheritedFromLAF(false);

    return result;
  }

  protected void update(@NotNull final P property, @NotNull final RenderingContext context) {
    myPsiFile = createPsiFile(property);

    assert myPsiFile != null;

    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
    final Document document = documentManager.getDocument(myPsiFile);

    assert document != null : "document can't be null!";

    final EditorTextField textField = getEditorTextField();
    textField.setDocument(document);

    final Color backgroundColor = context.getPresentationManager().getBackgroundColor(property, context.isSelected());

    if (!context.isEditor()) {
      textField.setBackground(backgroundColor);
      myWrappingPanel.setBackground(backgroundColor);
    }

    textField.setBorder(context.hasFocus()
                        ? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, backgroundColor),
                                                             BorderFactory.createMatteBorder(0, 4, 0, 4, backgroundColor))
                        : DEFAULT_BORDER);
  }

  protected abstract PsiFile createPsiFile(final P property);

  protected P getProperty() {
    return myProperty;
  }

  @Override
  public JComponent getEditorComponent(final P property, final RenderingContext context) {
    myProperty = property;
    update(property, context);
    return myWrappingPanel;
  }

  @Override
  public JComponent getFocusableComponent() {
    return getEditorTextField();
  }

  @Override
  public Object getEditingValue() {
    return myProperty;
  }

  protected void writeValue(final P p, final String value) {
    // overwrite
  }

  @Override
  public boolean stopEditing(final boolean cancelled) {
    if (!cancelled) {
      final EditorTextField field = getEditorTextField();
      final String value = field.getDocument().getText();
      WriteCommandAction.writeCommandAction(getProject(), getContainingFile(myProperty)).run(() -> writeValue(myProperty, value));
    }

    return true;
  }

  protected PsiFile getContainingFile(final P property) {
    return null;
  }

  @Override
  public boolean canEdit(P property) {
    return true;
  }

  @Override
  public boolean accepts(final P property) {
    return true;
  }

  protected boolean isErrorValue() {
    final PsiElement[] children = myPsiFile.getChildren();
    assert children.length == 1;

    final PsiElement[] psiElements = PsiTreeUtil.collectElements(children[0], new PsiElementFilter() {
      @Override
      public boolean isAccepted(@NotNull PsiElement element) {
        return element instanceof PsiErrorElement;
      }
    });

    return psiElements.length != 0;
  }
}
