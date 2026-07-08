// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;

public class PsiPuppetVisitor extends PsiElementVisitor {

  public void visitAnonymousBlock(@NotNull PsiPuppetAnonymousBlock o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitAnyNameWrapper(@NotNull PsiPuppetAnyNameWrapper o) {
    visitExpression(o);
    // visitPuppetNameWrapper(o);
  }

  public void visitArgument(@NotNull PsiPuppetArgument o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitArray(@NotNull PsiPuppetArray o) {
    visitExpression(o);
  }

  public void visitBlock(@NotNull PsiPuppetBlock o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitBracedAnyArgumentsListBlock(@NotNull PsiPuppetBracedAnyArgumentsListBlock o) {
    visitBlock(o);
  }

  public void visitBracedCaseOptsBlock(@NotNull PsiPuppetBracedCaseOptsBlock o) {
    visitBlock(o);
  }

  public void visitBracedResourceByClassnameContentsBlock(@NotNull PsiPuppetBracedResourceByClassnameContentsBlock o) {
    visitBlock(o);
  }

  public void visitBracedSelectorValuesBlock(@NotNull PsiPuppetBracedSelectorValuesBlock o) {
    visitBlock(o);
  }

  public void visitBracedStatementsBlock(@NotNull PsiPuppetBracedStatementsBlock o) {
    visitBlock(o);
  }

  public void visitCapitalizedNameWrapper(@NotNull PsiPuppetCapitalizedNameWrapper o) {
    visitAnyNameWrapper(o);
    // visitPuppetCapitalizedNameWrapper(o);
  }

  public void visitCaseValues(@NotNull PsiPuppetCaseValues o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitCaseExpression(@NotNull PsiPuppetCaseExpression o) {
    visitExpression(o);
  }

  public void visitCaseOption(@NotNull PsiPuppetCaseOption o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitClassDefinition(@NotNull PsiPuppetClassDefinition o) {
    visitExpression(o);
    // visitPuppetClassDefinition(o);
  }

  public void visitCollExpr(@NotNull PsiPuppetCollExpr o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitCollectRHand(@NotNull PsiPuppetCollectRHand o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitCollection(@NotNull PsiPuppetCollection o) {
    visitExpression(o);
  }

  public void visitCollectionStatement(@NotNull PsiPuppetCollectionStatement o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitConditionalBranch(@NotNull PsiPuppetConditionalBranch o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitConsumesStatement(@NotNull PsiPuppetConsumesStatement o) {
    visitExpression(o);
  }

  public void visitDataType(@NotNull PsiPuppetDataType o) {
    visitExpression(o);
    // visitPuppetDataType(o);
  }

  public void visitDataTypeParametersBlock(@NotNull PsiPuppetDataTypeParametersBlock o) {
    visitBlock(o);
  }

  public void visitDefaultSelectorValue(@NotNull PsiPuppetDefaultSelectorValue o) {
    visitSelectorValue(o);
  }

  public void visitDefaultWrapper(@NotNull PsiPuppetDefaultWrapper o) {
    visitAnyNameWrapper(o);
    // visitPuppetDefaultWrapper(o);
  }

  public void visitExpression(@NotNull PsiPuppetExpression o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitExpressionAdditive(@NotNull PsiPuppetExpressionAdditive o) {
    visitExpression(o);
  }

  public void visitExpressionAssignment(@NotNull PsiPuppetExpressionAssignment o) {
    visitExpression(o);
  }

  public void visitExpressionBinary(@NotNull PsiPuppetExpressionBinary o) {
    visitExpression(o);
  }

  public void visitExpressionCall(@NotNull PsiPuppetExpressionCall o) {
    visitExpression(o);
  }

  public void visitExpressionComp(@NotNull PsiPuppetExpressionComp o) {
    visitExpression(o);
  }

  public void visitExpressionFalse(@NotNull PsiPuppetExpressionFalse o) {
    visitExpression(o);
  }

  public void visitExpressionIn(@NotNull PsiPuppetExpressionIn o) {
    visitExpression(o);
  }

  public void visitExpressionIndex(@NotNull PsiPuppetExpressionIndex o) {
    visitExpression(o);
  }

  public void visitExpressionIndexed(@NotNull PsiPuppetExpressionIndexed o) {
    visitExpression(o);
  }

  public void visitExpressionMultiplicative(@NotNull PsiPuppetExpressionMultiplicative o) {
    visitExpression(o);
  }

  public void visitExpressionNumeric(@NotNull PsiPuppetExpressionNumeric o) {
    visitExpression(o);
  }

  public void visitExpressionParen(@NotNull PsiPuppetExpressionParen o) {
    visitExpression(o);
  }

  public void visitExpressionRegex(@NotNull PsiPuppetExpressionRegex o) {
    visitExpression(o);
  }

  public void visitExpressionRelation(@NotNull PsiPuppetExpressionRelation o) {
    visitExpression(o);
  }

  public void visitExpressionTrue(@NotNull PsiPuppetExpressionTrue o) {
    visitExpression(o);
  }

  public void visitExpressionUnary(@NotNull PsiPuppetExpressionUnary o) {
    visitExpression(o);
  }

  public void visitExpressionUndef(@NotNull PsiPuppetExpressionUndef o) {
    visitExpression(o);
  }

  public void visitFqnContainer(@NotNull PsiPuppetFqnContainer o) {
    visitPuppetFqnContainer(o);
  }

  public void visitFunctionCallExpression(@NotNull PsiPuppetFunctionCallExpression o) {
    visitExpression(o);
  }

  public void visitFunctionCallStatement(@NotNull PsiPuppetFunctionCallStatement o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitFunctionDefinition(@NotNull PsiPuppetFunctionDefinition o) {
    visitExpression(o);
    // visitPuppetFunctionDefinition(o);
  }

  public void visitFunctionReturnType(@NotNull PsiPuppetFunctionReturnType o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitHashArrayAccess(@NotNull PsiPuppetHashArrayAccess o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitHashArrayAccesses(@NotNull PsiPuppetHashArrayAccesses o) {
    visitExpression(o);
  }

  public void visitHashPair(@NotNull PsiPuppetHashPair o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitHashValue(@NotNull PsiPuppetHashValue o) {
    visitExpression(o);
  }

  public void visitHashArgument(@NotNull PsiPuppetHashArgument o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitHashArrayIndex(@NotNull PsiPuppetHashArrayIndex o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitHeredocExpression(@NotNull PsiPuppetHeredocExpression o) {
    visitExpression(o);
  }

  public void visitIfStatement(@NotNull PsiPuppetIfStatement o) {
    visitExpression(o);
  }

  public void visitImportStatement(@NotNull PsiPuppetImportStatement o) {
    visitExpression(o);
  }

  public void visitIncludeClassExpression(@NotNull PsiPuppetIncludeClassExpression o) {
    visitExpression(o);
  }

  public void visitIncludeClassStatement(@NotNull PsiPuppetIncludeClassStatement o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitInterpolatedBlock(@NotNull PsiPuppetInterpolatedBlock o) {
    visitBlock(o);
  }

  public void visitNamespaceDefinition(@NotNull PsiPuppetNamespaceDefinition o) {
    visitPuppetNamespaceDefinition(o);
  }

  public void visitNodeDefinition(@NotNull PsiPuppetNodeDefinition o) {
    visitExpression(o);
    // visitPuppetNodeDefinition(o);
  }

  public void visitNodeNamesList(@NotNull PsiPuppetNodeNamesList o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitParameter(@NotNull PsiPuppetParameter o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitParentNode(@NotNull PsiPuppetParentNode o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitParenthesizedExpressionsListBlock(@NotNull PsiPuppetParenthesizedExpressionsListBlock o) {
    visitBlock(o);
  }

  public void visitParenthesizedParametersListBlock(@NotNull PsiPuppetParenthesizedParametersListBlock o) {
    visitBlock(o);
  }

  public void visitPipedParametersListBlock(@NotNull PsiPuppetPipedParametersListBlock o) {
    visitBlock(o);
  }

  public void visitProducesStatement(@NotNull PsiPuppetProducesStatement o) {
    visitExpression(o);
  }

  public void visitQuotedText(@NotNull PsiPuppetQuotedText o) {
    visitExpression(o);
    // visitPuppetQuotedString(o);
  }

  public void visitRegexp(@NotNull PsiPuppetRegexp o) {
    visitExpression(o);
  }

  public void visitRegularNameWrapper(@NotNull PsiPuppetRegularNameWrapper o) {
    visitAnyNameWrapper(o);
    // visitPuppetRegularNameWrapper(o);
  }

  public void visitResourceArgumentsList(@NotNull PsiPuppetResourceArgumentsList o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitResourceDeclaration(@NotNull PsiPuppetResourceDeclaration o) {
    visitExpression(o);
    // visitPuppetResourceDeclaration(o);
  }

  public void visitResourceDefaultStatement(@NotNull PsiPuppetResourceDefaultStatement o) {
    visitExpression(o);
    // visitPuppetResourceDeclarationBase(o);
  }

  public void visitResourceInstanceDeclaration(@NotNull PsiPuppetResourceInstanceDeclaration o) {
    visitPuppetResourceInstanceDeclaration(o);
  }

  public void visitResourceLikeClassDeclarationBlock(@NotNull PsiPuppetResourceLikeClassDeclarationBlock o) {
    visitBlock(o);
  }

  public void visitResourceLikeClassDescription(@NotNull PsiPuppetResourceLikeClassDescription o) {
    visitPuppetResourceLikeClassDescription(o);
  }

  public void visitSelector(@NotNull PsiPuppetSelector o) {
    visitExpression(o);
  }

  public void visitSelectorValue(@NotNull PsiPuppetSelectorValue o) {
    visitPuppetCompositePsiElement(o);
  }

  public void visitSiteCompound(@NotNull PsiPuppetSiteCompound o) {
    visitExpression(o);
  }

  public void visitTypeDefinition(@NotNull PsiPuppetTypeDefinition o) {
    visitExpression(o);
    // visitPuppetTypeDefinition(o);
  }

  public void visitUnlessStatement(@NotNull PsiPuppetUnlessStatement o) {
    visitExpression(o);
  }

  public void visitVarWrapper(@NotNull PsiPuppetVarWrapper o) {
    visitExpression(o);
    // visitPuppetVariable(o);
  }

  public void visitPuppetFqnContainer(@NotNull PuppetFqnContainer o) {
    visitElement(o);
  }

  public void visitPuppetNamespaceDefinition(@NotNull PuppetNamespaceDefinition o) {
    visitElement(o);
  }

  public void visitPuppetResourceInstanceDeclaration(@NotNull PuppetResourceInstanceDeclaration o) {
    visitElement(o);
  }

  public void visitPuppetResourceLikeClassDescription(@NotNull PuppetResourceLikeClassDescription o) {
    visitElement(o);
  }

  public void visitPuppetCompositePsiElement(@NotNull PuppetCompositePsiElement o) {
    visitElement(o);
  }

}
