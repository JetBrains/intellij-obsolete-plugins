package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PsiPuppetIncludeClassStatement;
import com.intellij.lang.puppet.psi.PuppetDataType;
import com.intellij.lang.puppet.psi.PuppetIncludeClassStatement;
import com.intellij.lang.puppet.psi.PuppetPsiUtil;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedPsiElementBase;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetIncludeClassStatementStub;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.intellij.lang.puppet.PuppetTokenTypes.DATA_TYPE;
import static com.intellij.lang.puppet.PuppetTokenTypes.EXPRESSION_PAREN;
import static com.intellij.lang.puppet.PuppetTokenTypes.QUOTED_TEXT;
import static com.intellij.lang.puppet.PuppetTokenTypes.REGULAR_NAME_WRAPPER;

public abstract class PuppetIncludeClassStatementMixin extends PuppetStubBasedPsiElementBase<PuppetIncludeClassStatementStub>
  implements PuppetIncludeClassStatement, PsiPuppetIncludeClassStatement {

  // name holders from which we can extract classname with manipulator
  private static final TokenSet SIMPLE_NAME_HOLDERS = TokenSet.create(
    REGULAR_NAME_WRAPPER,
    QUOTED_TEXT
  );

  // all supported classname holders
  private static final TokenSet SUPPORTED_NAME_HOLDERS = TokenSet.orSet(
    TokenSet.create(DATA_TYPE),
    SIMPLE_NAME_HOLDERS
  );

  private static final TokenSet SUPPORTED_NAME_CONTAINERS = TokenSet.create(
    EXPRESSION_PAREN
  );

  public PuppetIncludeClassStatementMixin(@NotNull PuppetIncludeClassStatementStub stub,
                                          @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetIncludeClassStatementMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull Collection<String> getClassNames() {
    PsiElement firstChild = getFirstChild();
    if (firstChild == null) {
      return Collections.emptyList();
    }

    List<String> result = new ArrayList<>();
    PuppetPsiUtil.processPsiElementsRecursively(
      firstChild.getNextSibling(),
      element -> {
        if (element instanceof PuppetDataType) {
          String effectiveClassName = ((PuppetDataType)element).getEffectiveClassName();
          if (StringUtil.isNotEmpty(effectiveClassName)) {
            result.add(effectiveClassName);
          }
        }
        else {
          result.add(ElementManipulators.getValueText(element));
        }
        return true;
      },
      SUPPORTED_NAME_HOLDERS,
      SUPPORTED_NAME_CONTAINERS);
    return result;
  }
}
