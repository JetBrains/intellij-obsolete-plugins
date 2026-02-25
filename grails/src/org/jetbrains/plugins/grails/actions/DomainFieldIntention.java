// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.constraints.GrailsConstraintsUtil;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinitionBody;

public abstract class DomainFieldIntention implements IntentionAction {

  private final String myLabel;
  private final boolean myValue;

  protected DomainFieldIntention(String label, boolean value) {
    myLabel = label;
    myValue = value;
  }

  @Override
  public @NotNull String getFamilyName() {
    return getText();
  }

  protected abstract boolean isAppropriateField(@NotNull GrField field, @NotNull PsiType fieldType);

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
    if (!PsiImplUtil.isLeafElementOfType(element, GroovyTokenTypes.mIDENT)) return false;

    PsiElement eField = element.getParent();
    if (!(eField instanceof GrField field)) return false;

    PsiClass aClass = field.getContainingClass();
    if (!(aClass instanceof GrTypeDefinition)) return false; // Domain can be a java class.

    String name = field.getName();
    PsiType fieldType;

    if (GormUtils.isGormBean(aClass)) {
      Pair<PsiType,PsiElement> pair = DomainDescriptor.getPersistentProperties(aClass).get(name);
      if (pair == null) return false;

      fieldType = pair.first;
    }
    else {
      if (!GrailsUtils.isValidatedClass(aClass)) return false;

      fieldType = field.getTypeGroovy();
    }

    if (fieldType != null && !isAppropriateField(field, fieldType)) {
      return false;
    }

    PsiField constraintsField = aClass.findFieldByName("constraints", false);
    if (!(constraintsField instanceof GrField)) return true;

    GrExpression initializerGroovy = ((GrField)constraintsField).getInitializerGroovy();

    if (initializerGroovy == null) return true;

    if (initializerGroovy instanceof GrLiteral) {
      return ((GrLiteral)initializerGroovy).getValue() == ObjectUtils.NULL;
    }

    if (!(initializerGroovy instanceof GrClosableBlock)) return false;

    for (PsiElement child = initializerGroovy.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof GrMethodCall call) {

        GrExpression invokedExpression = call.getInvokedExpression();
        if (invokedExpression instanceof GrReferenceExpression) {
          if (name.equals(((GrReferenceExpression)invokedExpression).getReferenceName())) {
            PsiMethod method = call.resolveMethod();
            if (GrailsConstraintsUtil.isConstraintsMethod(method)) {
              for (GrNamedArgument namedArgument : call.getNamedArguments()) {
                GrArgumentLabel label = namedArgument.getLabel();
                if (label != null && myLabel.equals(label.getName())) {
                  GrExpression expression = namedArgument.getExpression();
                  if (expression == null) return true;
                  if (!(expression instanceof GrLiteral)) return false;

                  return !Boolean.valueOf(myValue).equals(((GrLiteral)expression).getValue());
                }
              }

              return true;
            }
          }
        }
      }
    }

    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
    assert element != null;

    GrField field = (GrField)element.getParent();

    GrTypeDefinition aClass = (GrTypeDefinition)field.getContainingClass();
    assert aClass != null;

    String name = field.getName();
    assert name != null;

    GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);

    GrField constraintsField = (GrField)aClass.findFieldByName("constraints", false);

    if (constraintsField == null) {
      GrVariableDeclaration constraintDefinition = factory.createFieldDeclarationFromText("static constraints = {}");
      GrTypeDefinitionBody body = aClass.getBody();
      PsiElement lastChild = body.getLastChild();

      if (!PsiImplUtil.isLeafElementOfType(lastChild, GroovyTokenTypes.mRCURLY)) return;
      assert lastChild != null;

      PsiElement prev = lastChild.getPrevSibling();
      int lineBreakCount = 0;

      if (prev instanceof LeafPsiElement) {
        if (org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil.isWhiteSpaceOrNls(prev)) {
          CharSequence text = ((LeafPsiElement)prev).getChars();
          for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
              lineBreakCount++;
            }
          }
        }
      }

      for (int i = lineBreakCount; i < 2; i++) {
        body.getNode().addLeaf(GroovyTokenTypes.mNLS, "\n", lastChild.getNode());
      }

      constraintDefinition = (GrVariableDeclaration)body.addBefore(constraintDefinition, lastChild);

      //constraintDefinition = aClass.addMemberDeclaration(constraintDefinition, null);
      constraintsField = (GrField)constraintDefinition.getVariables()[0];
    }

    GrExpression initializerGroovy = constraintsField.getInitializerGroovy();
    if (initializerGroovy == null || initializerGroovy instanceof GrLiteral) {
      GrVariableDeclaration constraintDefinition = factory.createFieldDeclarationFromText("""

                                                                                              static constraints = {
                                                                                              }
                                                                                            """);
      GrField newConstraintField = (GrField)constraintDefinition.getVariables()[0];
      constraintsField = (GrField)constraintsField.replace(newConstraintField);

      initializerGroovy = constraintsField.getInitializerGroovy();
      assert initializerGroovy != null;
    }

    GrNamedArgument newNamedArgument = factory.createNamedArgument(myLabel, factory.createLiteralFromValue(myValue));

    for (PsiElement child = initializerGroovy.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof GrMethodCall call) {

        GrExpression invokedExpression = call.getInvokedExpression();
        if (invokedExpression instanceof GrReferenceExpression) {
          if (name.equals(((GrReferenceExpression)invokedExpression).getReferenceName())) {
            PsiMethod method = call.resolveMethod();
            if (GrailsConstraintsUtil.isConstraintsMethod(method)) {
              for (GrNamedArgument namedArgument : call.getNamedArguments()) {
                GrArgumentLabel label = namedArgument.getLabel();
                if (label != null && myLabel.equals(label.getName())) {
                  namedArgument.replace(newNamedArgument);
                  return;
                }
              }

              GrArgumentList argumentList = call.getArgumentList();

              argumentList.addNamedArgument(newNamedArgument);
              return;
            }
          }
        }
      }
    }

    GrStatement statement = factory.createStatementFromText(name + '(' + newNamedArgument.getText() + ')');
    ((GrClosableBlock)initializerGroovy).addStatementBefore(statement, null);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
