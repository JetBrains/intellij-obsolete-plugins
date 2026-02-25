// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.perspectives.create;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassNode;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.grails.references.domain.DomainClassUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.typedef.members.GrMethodImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation;


public class RelationsCreationsProvider {
  private static final Logger LOG = Logger.getInstance(RelationsCreationsProvider.class);

  private final String newNodeTypeText;
  private final GrTypeDefinition typeDefinition;
  private final Project myProject;

  private final @NotNull String enteredName;
  private final GroovyPsiElementFactory factory;

  public RelationsCreationsProvider(String newNodeTypeText, GrTypeDefinition typeDefinition, Project project, @NotNull String enteredName) {
    this.newNodeTypeText = newNodeTypeText;
    this.typeDefinition = typeDefinition;
    myProject = project;
    this.enteredName = enteredName;

    factory = GroovyPsiElementFactory.getInstance(myProject);
  }

  public @NotNull String getEnteredName() {
    return enteredName;
  }

  public void createHasManyRelation() {
    GrField hasManyField = (GrField) typeDefinition.findFieldByName(DomainClassRelationsInfo.HAS_MANY_NAME, true);

    if (hasManyField == null) {
      addNewRelationNode(Relation.HAS_MANY, null, "[" + getEnteredName() + ":" + newNodeTypeText + "]");
    } else {
      assert hasManyField.getInitializerGroovy() instanceof GrListOrMap;

      GrExpression initializerGroovy = hasManyField.getInitializerGroovy();
      assert initializerGroovy instanceof GrListOrMap;

      GrListOrMap belongsToList = (GrListOrMap) initializerGroovy;
      GrNamedArgument[] namedArguments = belongsToList.getNamedArguments();
      StringBuilder newInitializerText = new StringBuilder();

      newInitializerText.append("[");
      for (int i = 0; i < namedArguments.length; i++) {
        GrNamedArgument namedArgument = namedArguments[i];
        GrExpression initializer = namedArgument.getExpression();
        GrArgumentLabel argumentLabel = namedArgument.getLabel();

        assert argumentLabel != null;
        newInitializerText.append(argumentLabel.getText());
        newInitializerText.append(": ");
        newInitializerText.append(initializer.getText());

        if (i != namedArguments.length - 1) {
          newInitializerText.append(", ");
        }
      }
      newInitializerText.append(", ");
      newInitializerText.append(getEnteredName());
      newInitializerText.append(": ");
      newInitializerText.append(newNodeTypeText);
      newInitializerText.append("]");

      addNewRelationNode(Relation.HAS_MANY, hasManyField, newInitializerText.toString());
    }
  }

  public void createBelongsToRelation() {
    GrField belongsToField = (GrField) typeDefinition.findFieldByName(DomainClassRelationsInfo.BELONGS_TO_NAME, true);

    // there is no belongsTo variable
    if (belongsToField == null) {
      addNewRelationNode(Relation.BELONGS_TO, null, "[" + newNodeTypeText + "]");
    } else {

      if (!(belongsToField.getInitializerGroovy() instanceof GrListOrMap)) {
        // static belongsTo = Author
        GrExpression initializerGroovy = belongsToField.getInitializerGroovy();
        assert initializerGroovy != null;
        String newInitializerText = "[" + initializerGroovy.getText() + ", " + newNodeTypeText + "]";

        addNewRelationNode(Relation.BELONGS_TO, belongsToField, newInitializerText);
      } else {
        GrExpression initializerGroovy = belongsToField.getInitializerGroovy();
        assert initializerGroovy instanceof GrListOrMap;

        GrListOrMap belongsToList = (GrListOrMap) initializerGroovy;
        GrExpression[] belongsToListInitializers = belongsToList.getInitializers();

        String newInitializerText = "[";
        for (int i = 0; i < belongsToListInitializers.length; i++) {
          GrExpression initializer = belongsToListInitializers[i];

          newInitializerText += initializer.getText();
          if (i != belongsToListInitializers.length - 1) {
            newInitializerText += ", ";
          }
        }
        newInitializerText += ", " + newNodeTypeText;
        newInitializerText += "]";
        addNewRelationNode(Relation.BELONGS_TO, belongsToField, newInitializerText);
      }
    }
  }

  private void addNewRelationNode(final Relation relationType, final GrField oldRelationField, final String newInitializerText) {
    //    adding new "static belongsTo = [RefExpr1, RefExpr2]" and "RefExpr1 varName" to fields
    if (oldRelationField != null) {
      assert typeDefinition.getBody() == oldRelationField.getParent().getParent();
    }

    WriteCommandAction.runWriteCommandAction(myProject, () -> {
      GrVariableDeclaration treeRelationField = null;
      GrVariableDeclaration treeBelongsToField = null;

      if (Relation.BELONGS_TO == relationType) {
        treeBelongsToField = createRelationFieldDefinition(DomainClassRelationsInfo.BELONGS_TO_NAME, newInitializerText);
        treeBelongsToField = addRelationFieldToPsi(oldRelationField, treeBelongsToField);

        //adding simple var declaration
        treeRelationField = addSimpleVarDeclaration(factory.createSimpleVariableDeclaration(getEnteredName(), newNodeTypeText), treeBelongsToField);

      } else if (Relation.HAS_MANY == relationType) {
        treeRelationField = createRelationFieldDefinition(DomainClassRelationsInfo.HAS_MANY_NAME, newInitializerText);
        treeRelationField = addRelationFieldToPsi(oldRelationField, treeRelationField);

      } else if (Relation.STRONG == relationType) {
        final GrVariableDeclaration newStrongRelation = factory.createSimpleVariableDeclaration(enteredName, newNodeTypeText);
        treeRelationField = addSimpleVarDeclaration(newStrongRelation, getAppropriateAnchor());
      }

      if (treeRelationField != null) {
        JavaCodeStyleManager.getInstance(myProject).shortenClassReferences(treeRelationField);
      }
      if (treeBelongsToField != null) {
        JavaCodeStyleManager.getInstance(myProject).shortenClassReferences(treeBelongsToField);
      }
    });
  }

  private GrVariableDeclaration addSimpleVarDeclaration(GrVariableDeclaration simpleVarDef, PsiElement anchor) {
    try {
      return (GrVariableDeclaration)typeDefinition.addBefore(simpleVarDef, anchor);
    } catch (IncorrectOperationException e) {
      LOG.error(e);
      return null;
    }
  }

  private GrVariableDeclaration addRelationFieldToPsi(final GrField oldRelationNode, final GrVariableDeclaration newBelongsToVarDef) {
    if (oldRelationNode != null) {
      typeDefinition.getBody().removeVariable(oldRelationNode);
    }

    PsiElement anchor = getAppropriateAnchor();

    try {
      return (GrVariableDeclaration)typeDefinition.addBefore(newBelongsToVarDef, anchor);
    } catch (IncorrectOperationException e) {
      LOG.error(e);
      return null;
    }
  }

  private PsiElement getAppropriateAnchor() {
    PsiElement lastBodyChild;
    GrField[] fields = typeDefinition.getFields();
    if (fields.length > 0) {
      PsiElement field = fields[fields.length - 1];
      assert field != null;
      lastBodyChild = field.getParent();
    } else {
      PsiMethod[] methods = typeDefinition.getMethods();
      lastBodyChild = typeDefinition.getBody().getLastChild();

      for (PsiMethod method : methods) {
        if (method instanceof GrMethodImpl){
          lastBodyChild =  method;
          break;
        }
      }
    }
    return lastBodyChild;
  }

  private GrVariableDeclaration createRelationFieldDefinition(String name, String initializerText) {
    GrExpression newInitializer = factory.createExpressionFromText(initializerText);

    return factory.createFieldDeclaration(new String[]{PsiModifier.STATIC}, name, newInitializer, null);
  }

  public boolean canCreateBelongsToRelation() {
    final GrField[] fields = typeDefinition.getFields();


    for (GrField field : fields) {
      PsiType fieldType = field.getType();
      if (fieldType instanceof PsiClassType && newNodeTypeText.equals(((PsiClassType) fieldType).getClassName()))
        return false;

      if (checkForExistingHasManyRelation(field)) return false;
    }

    GrVariable belongsTo = (GrVariable) typeDefinition.findFieldByName(DomainClassRelationsInfo.BELONGS_TO_NAME, true);
    if (belongsTo == null) return true;

    GrExpression list = belongsTo.getInitializerGroovy();

    if (list instanceof GrListOrMap initsList) {
      GrExpression[] initializers = initsList.getInitializers();

      for (GrExpression expression : initializers) {
        if (newNodeTypeText.equals(expression.getText())) return false;
      }
    } else if (list instanceof GrReferenceExpression initializer) {

      if (newNodeTypeText.equals(initializer.getReferenceName())) return false;
    } else {
      return false;
    }

    return true;
  }

  private boolean checkForExistingHasManyRelation(GrField field) {
    Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges = new HashMap<>();
    if (DomainClassUtils.isHasManyField(field)) {
      DomainClassUtils.buildHasManySourcesToOutEdgesMap(sourcesToOutEdges, field);

      final Collection<List<DomainClassRelationsInfo>> outEdges = sourcesToOutEdges.values();
      final List<DomainClassRelationsInfo> relationsInfoList = ContainerUtil.flatten(outEdges);
      for (DomainClassRelationsInfo domainClassRelationsInfo : relationsInfoList) {
        if (newNodeTypeText.equals(domainClassRelationsInfo.getTarget().getTypeDefinition().getName())) return true;
      }
    }
    return false;
  }

  public boolean canCreateHasManyRelation() {
    GrField hasManyField = (GrField) typeDefinition.findFieldByName(DomainClassRelationsInfo.HAS_MANY_NAME, true);
    if (hasManyField == null) return true;

    GrExpression map = hasManyField.getInitializerGroovy();
    assert map instanceof GrListOrMap;
    GrListOrMap namesMap = (GrListOrMap) map;
    GrNamedArgument[] arguments = namesMap.getNamedArguments();

    GrArgumentLabel argumentLabel;
    GrExpression type;
    for (GrNamedArgument argument : arguments) {
      argumentLabel = argument.getLabel();
      type = argument.getExpression();

      assert argumentLabel != null;
      if (!(type instanceof GrReferenceExpression)) return false;

      if (newNodeTypeText.equals(((GrReferenceExpression) type).getReferenceName())) return false;
      if (enteredName.equals(argumentLabel.getText())) return false;
    }

    return true;
  }

  public boolean canCreateStrongRelation() {
    final GrField[] fields = typeDefinition.getFields();
    for (GrField field : fields) {
      if (enteredName.equals(field.getName())) return false;

      if (checkForExistingHasManyRelation(field)) return false;
    }

    return true;
  }

  public void createStrongRelation() {
    addNewRelationNode(Relation.STRONG, null, null);
  }
}
