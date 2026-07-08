package com.intellij.lang.puppet.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.puppet.PuppetHeredocParser;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.lexer.PuppetHeredocLexerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.ParsingDiagnostics;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PuppetQQHeredocElementType extends ILazyParseableElementType {
  public PuppetQQHeredocElementType(@NotNull @NonNls String debugName) {
    super(debugName, PuppetLanguage.INSTANCE);
  }

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement parentElement) {
    Project project = parentElement.getProject();
    PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
      project,
      chameleon,
      new PuppetHeredocLexerAdapter(project),
      getLanguage(),
      chameleon.getText());

    var startTime = System.nanoTime();
    var result = PuppetHeredocParser.INSTANCE.parse(this, builder).getFirstChildNode();
    ParsingDiagnostics.registerParse(builder, getLanguage(), System.nanoTime() - startTime);
    return result;
  }
}
