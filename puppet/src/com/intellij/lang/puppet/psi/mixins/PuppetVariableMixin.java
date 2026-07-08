package com.intellij.lang.puppet.psi.mixins;

import com.intellij.icons.AllIcons;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.PsiPuppetExpression;
import com.intellij.lang.puppet.psi.PsiPuppetExpressionAssignment;
import com.intellij.lang.puppet.psi.PuppetCapitalizedNameWrapper;
import com.intellij.lang.puppet.psi.PuppetDataType;
import com.intellij.lang.puppet.psi.PuppetNamedScopeHolder;
import com.intellij.lang.puppet.psi.PuppetPsiFileImpl;
import com.intellij.lang.puppet.psi.PuppetScopeHolder;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedPsiElementBase;
import com.intellij.lang.puppet.psi.stubs.PuppetVariableStub;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

public abstract class PuppetVariableMixin extends PuppetStubBasedNamedPsiElementBase<PuppetVariableStub>
  implements PuppetVariable {

  private static final TokenSet DECLARATION_CONTAINERS = TokenSet.create(
    EXPRESSION_ASSIGNMENT
  );

  public PuppetVariableMixin(@NotNull ASTNode node) {
    super(node);
  }

  public PuppetVariableMixin(@NotNull PuppetVariableStub stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  @Override
  protected @Nullable String computeName() {
    return getNameRange().substring(getText());
  }

  @Override
  public @Nullable String getFullQualifiedName() {
    // fixme this is for debugging porposes, PSIViewer does not see default methods
    return super.getFullQualifiedName();
  }

  /**
   * @return namespace or null if it's lexical variable/usage
   */
  @Override
  protected @Nullable String computeNamespaceName() {
    String identifierText = getNameIdentifierText();
    if (StringUtil.isEmpty(identifierText)) {
      return null;
    }

    identifierText = PuppetQualifiedNamesUtil.getCanonicalVariableFullQualifiedName(identifierText); // to handle ::var

    int lastSeparatorIndex = identifierText.lastIndexOf(SEPARATOR);
    if (lastSeparatorIndex != -1) {
      return StringUtil.toLowerCase(identifierText.substring(0, lastSeparatorIndex));
    }

    if (!isDeclaration()) {
      return null;
    }

    PuppetScopeHolder closestScopeHolder = getScopeHolder();

    return closestScopeHolder instanceof PuppetNamedScopeHolder
           ? ((PuppetNamedScopeHolder)closestScopeHolder).getScopeFullQualifiedName()
           : null;
  }

  @Override
  public @NotNull TextRange getNameRange() {
    PsiElement identifier = getNameIdentifier();
    if (identifier == null) {
      return TextRange.EMPTY_RANGE;
    }
    String identifierText = identifier.getText();
    int separatorOffset = identifierText.lastIndexOf(SEPARATOR);
    TextRange nameRange = separatorOffset == -1 ?
                          TextRange.allOf(identifierText) :
                          TextRange.create(separatorOffset + SEPARATOR.length(), identifierText.length());
    return nameRange.shiftRight(identifier.getStartOffsetInParent());
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    return ElementManipulators.handleContentChange(this, name);
  }

  @Override
  public boolean isFullQualified() {
    String identifierText = getNameIdentifierText();
    return StringUtil.isNotEmpty(identifierText) && identifierText.lastIndexOf(SEPARATOR) != -1;
  }

  @Override
  public int getTextOffset() {
    return getNameRange().getStartOffset() + getTextRange().getStartOffset();
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    return findChildByType(PuppetTokenTypes.VARIABLE_NAME);
  }

  @Override
  public boolean isDeclaration() {
    PuppetVariableStub stub = getGreenStub();
    if (stub != null) {
      return stub.isDeclaration();
    }

    if (isFullQualified()) {
      return false;
    }

    return DECLARATION_CONTAINERS.contains(PsiUtilCore.getElementType(getParent())) && getPrevSibling() == null ||
           isParameter() ||
           isAssignmentExpressionLvalue();
  }

  private boolean isAssignmentExpressionLvalue() {
    PsiPuppetExpressionAssignment assignment = PsiTreeUtil.getParentOfType(this, PsiPuppetExpressionAssignment.class);
    if (assignment == null) {
      return false;
    }

    PsiElement rightSide = assignment.getLastChild();
    return rightSide != null && !PsiTreeUtil.isAncestor(rightSide, this, false);
  }

  @Override
  public boolean isParameter() {
    PuppetVariableStub stub = getGreenStub();
    if (stub != null) {
      return stub.isParameter();
    }

    PsiElement parent = getParent();
    IElementType parentType = PsiUtilCore.getElementType(parent);
    if (!PARAMETER.equals(parentType)) {
      return false;
    }

    PsiElement[] children = parent.getChildren();
    return children[0] == this ||
           (children[0] instanceof PuppetDataType || children[0] instanceof PuppetCapitalizedNameWrapper) &&
           children.length > 1 &&
           children[1] == this;
  }

  @Override
  public boolean isMetaparameter() {
    PuppetTypeDefinition resourceDefinition = PsiTreeUtil.getStubOrPsiParentOfType(this, PuppetTypeDefinition.class);
    return resourceDefinition != null && resourceDefinition.isMetaparameterContainingType();
  }

  @Override
  public boolean isCoreFact() {
    return isDeclaration()
           && !isParameter()
           && PuppetPsiFileImpl.isInBuiltInStubsFile(this);
  }

  @Override
  public boolean isBuiltIn() {
    return PuppetPsiFileImpl.isInBuiltinVariablesStubsFile(this);
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    if (isParameter()) {
      return AllIcons.Nodes.Parameter;
    }
    else {
      return AllIcons.Nodes.Variable;
    }
  }

  @Override
  public boolean hasReferences() {
    return true;
  }

  // these methods just to make Grammar-Kit properly work; Variable
  public @Nullable PsiPuppetExpression getExpression() {
    return null;
  }

  @Override
  public boolean isLexicalDeclaration() {
    PuppetScopeHolder scopeHolder = getScopeHolder();
    return !(scopeHolder instanceof PuppetNamedScopeHolder) || ((PuppetNamedScopeHolder)scopeHolder).isLocalScope();
  }

  @Override
  public @Nullable PuppetScopeHolder getScopeHolder() {
    return PsiTreeUtil.getStubOrPsiParentOfType(this, PuppetScopeHolder.class);
  }
}
