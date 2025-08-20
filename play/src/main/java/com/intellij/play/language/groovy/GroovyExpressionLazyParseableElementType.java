package com.intellij.play.language.groovy;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.play.language.PlayLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyLexer;
import org.jetbrains.plugins.groovy.lang.parser.GroovyParser;
import org.jetbrains.plugins.groovy.lang.parser.GroovyParserDefinition;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementTypes;

public class GroovyExpressionLazyParseableElementType extends ILazyParseableElementType {
  public GroovyExpressionLazyParseableElementType(String debugName) {
    super(debugName, PlayLanguage.INSTANCE);
  }

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
    final PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
      psi.getProject(), chameleon, new GroovyLexer(), GroovyLanguage.INSTANCE, chameleon.getText()
    );
    final PsiBuilder.Marker mark = builder.mark();
    new GroovyParser().parseLight(GroovyElementTypes.EXPRESSION, builder);
    mark.done(GroovyParserDefinition.GROOVY_FILE);
    return builder.getTreeBuilt().getFirstChildNode();
  }
}
