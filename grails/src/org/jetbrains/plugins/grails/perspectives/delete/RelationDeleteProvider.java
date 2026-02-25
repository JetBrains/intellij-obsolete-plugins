// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.perspectives.delete;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.graph.builder.DeleteProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassNode;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.List;

public class RelationDeleteProvider extends DeleteProvider<DomainClassNode, DomainClassRelationsInfo> {
  private final Project myProject;

  public RelationDeleteProvider(Project project) {
    myProject = project;
  }

  @Override
  public boolean canDeleteNode(@NotNull DomainClassNode node) {
    return false;
  }

  @Override
  public boolean canDeleteEdge(@NotNull DomainClassRelationsInfo edge) {
    return true;
  }

  @Override
  public boolean deleteNode(@NotNull DomainClassNode node) {
    return false;
  }

  @Override
  public boolean deleteEdge(@NotNull DomainClassRelationsInfo edge) {
    final DomainClassNode targetCLassNode = edge.getTarget();
    final DomainClassNode sourceClassNode = edge.getSource();
    final String varName = edge.getVarName();

    final DomainClassRelationsInfo.Relation relationType = edge.getRelation();
    final Runnable runnable;

    if (DomainClassRelationsInfo.Relation.BELONGS_TO == relationType) {
      runnable = deleteBelongsToRelation(targetCLassNode, sourceClassNode);

    } else if (DomainClassRelationsInfo.Relation.HAS_MANY == relationType) {
      runnable = deleteHasManyRelation(varName, sourceClassNode);

    } else if (DomainClassRelationsInfo.Relation.STRONG == relationType) {
      runnable = deleteStrongRelation(varName, sourceClassNode);

    } else {
      runnable = EmptyRunnable.getInstance();
    }

    CommandProcessor.getInstance().executeCommand(
      myProject,
      () -> ApplicationManager.getApplication().runWriteAction(runnable), GrailsBundle.message("command.name.foo"), null);

    return false;
  }

  private static Runnable deleteStrongRelation(final String varName, final DomainClassNode sourceClassNode) {
    return () -> deleteStrongField(sourceClassNode.getTypeDefinition(), varName);
  }

  private static Runnable deleteBelongsToRelation(final DomainClassNode targetCLassNode, final DomainClassNode sourceClassNode) {
    return () -> {
      final PsiClass sourceTypeDefinition = sourceClassNode.getTypeDefinition();
      GrField belongsToField = (GrField) sourceTypeDefinition.findFieldByName(DomainClassRelationsInfo.BELONGS_TO_NAME, true);
      assert belongsToField != null;
      final ASTNode belongsToNode = belongsToField.getNode();
      assert belongsToNode != null;

      GrExpression initializerGroovy = belongsToField.getInitializerGroovy();
      String fieldBelongsToItemType = "";

      if (initializerGroovy instanceof GrListOrMap belongsToList) {
        GrExpression[] refExpressions;

        //before removing
        refExpressions = belongsToList.getInitializers();
        //elements list, reference expressions
        final String name = targetCLassNode.getTypeDefinition().getName();
        assert name != null;

        for (GrExpression initializer : refExpressions) {
          assert initializer instanceof GrReferenceExpression;


          if (name.equals(initializer.getText())) {
            final ASTNode node = initializer.getParent().getNode();
            fieldBelongsToItemType = ((GrReferenceExpression) initializer).getReferenceName();

            assert node != null;
            node.removeChild(initializer.getNode());

            break;
          }
        }

        //after removing
        refExpressions = belongsToList.getInitializers();
        if (refExpressions.length == 0) {
          ((GrTypeDefinition) sourceTypeDefinition).getBody().removeVariable(belongsToField);
        } else {
          removesSurplusCommas(belongsToList, GrReferenceExpression.class);
        }

      } else if (initializerGroovy instanceof GrReferenceExpression) {
        //removes belongsTo node
        fieldBelongsToItemType = initializerGroovy.getText();

        ((GrTypeDefinition) sourceTypeDefinition).getBody().removeVariable(belongsToField);
      } else {
        return;
      }

      deleteBelongsToItemField(sourceTypeDefinition, fieldBelongsToItemType);
      PsiUtil.reformatCode(sourceClassNode.getTypeDefinition());
    };
  }

  private static void deleteBelongsToItemField(PsiClass sourceTypeDefinition, String fieldBelongsToItemType) {
    if (!(sourceTypeDefinition instanceof GrTypeDefinition)) return; //??
    final GrField[] fields = ((GrTypeDefinition) sourceTypeDefinition).getFields();

    for (GrField field : fields) {
      if (fieldBelongsToItemType.equals(field.getType().getPresentableText())) {
        ((GrTypeDefinition) sourceTypeDefinition).getBody().removeVariable(field);
      }
    }
  }

  private static void deleteStrongField(PsiClass sourceTypeDefinition, String strongFieldName) {
    if (!(sourceTypeDefinition instanceof GrTypeDefinition)) return; //??
    final GrField[] fields = ((GrTypeDefinition) sourceTypeDefinition).getFields();

    for (GrField field : fields) {
      if (strongFieldName.equals(field.getName())) {
        ((GrTypeDefinition) sourceTypeDefinition).getBody().removeVariable(field);
      }
    }
  }

  private static Runnable deleteHasManyRelation(final String varName, final DomainClassNode sourceClassNode) {
    return () -> {
      final PsiClass sourceTypeDefinition = sourceClassNode.getTypeDefinition();
      GrField hasManyField = (GrField) sourceTypeDefinition.findFieldByName(DomainClassRelationsInfo.HAS_MANY_NAME, true);
      if (hasManyField == null || !(hasManyField.getInitializerGroovy() instanceof GrListOrMap)) {
        return;
      }

      GrExpression initializerGroovy = hasManyField.getInitializerGroovy();
      if (!(initializerGroovy instanceof GrListOrMap hasManyList)) {
        return;
      }

      GrNamedArgument[] namedArguments;

      //before removing
      namedArguments = hasManyList.getNamedArguments();

      assert varName != null;
      for (GrNamedArgument namedArgument : namedArguments) {
        GrArgumentLabel argumentLabel = namedArgument.getLabel();

        assert argumentLabel != null;

        if (varName.equals(argumentLabel.getName())) {
          final ASTNode node = namedArgument.getParent().getNode();
          if (node == null) {
            return;
          }

          node.removeChild(namedArgument.getNode());
        }
      }

      //after removing
      if (!PsiImplUtil.hasNamedArguments(hasManyList)) {
        ((GrTypeDefinition) sourceTypeDefinition).getBody().removeVariable(hasManyField);
      } else {
        removesSurplusCommas(hasManyList, GrNamedArgument.class);
      }

      PsiUtil.reformatCode(sourceClassNode.getTypeDefinition());
    };
  }

  private static void removesSurplusCommas(GrListOrMap hasManyList, Class listElementClass) {
    //removes ',' if needs
    final List<PsiElement> children = new ArrayList<>();

    PsiElement tempChild = hasManyList.getFirstChild();
    while (tempChild != null) {
      if (!(tempChild instanceof PsiWhiteSpace)) {
        children.add(tempChild);
      }
      tempChild = tempChild.getNextSibling();
    }

    int i = 1;
    while (i < children.size() - 1) {
      PsiElement child = children.get(i);
      final ASTNode node = child.getNode();
      assert node != null;
      if (GroovyTokenTypes.mCOMMA.equals(node.getElementType())) {
        final PsiElement prevSibling = children.get(i - 1);
        final PsiElement nextSibling = children.get(i + 1);

        if (!listElementClass.isAssignableFrom(prevSibling.getClass()) || !listElementClass.isAssignableFrom(nextSibling.getClass())) {
          node.getTreeParent().removeChild(node);
          children.remove(i);
          continue;
        }
      }
      i++;
    }
  }
}
