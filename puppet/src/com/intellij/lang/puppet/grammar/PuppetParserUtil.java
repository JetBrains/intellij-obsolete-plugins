package com.intellij.lang.puppet.grammar;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetParser;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.lexer.PuppetTokenTypeSets;
import com.intellij.lang.puppet.util.PuppetConfigurationUtil;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class PuppetParserUtil extends GeneratedParserUtilBase implements PuppetTokenTypes {
  private static final Set<String> ourIncludeFunctions =
    Set.of("include", "require", "contain", "hiera_include");

  /*
     Overriding this static method allows to disable the usage of a brace matcher while recovering the errors
     This fixes the situation when some good nodes containing errors inside were substituted with dummy blocks
   */
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static PsiBuilder adapt_builder_(IElementType root, PsiBuilder builder, PsiParser parser, TokenSet[] tokenSets) {
    PsiBuilder result = GeneratedParserUtilBase.adapt_builder_(root, builder, parser, tokenSets);
    ErrorState.get(result).braces = null;
    return result;
  }

  public static boolean isPuppet4(@NotNull PsiBuilder builder, int level) {
    return PuppetConfigurationUtil.getPuppetVersion(builder.getProject()) == PuppetLanguage.Version.PUPPET_4;
  }

  public static boolean isPuppet3(@NotNull PsiBuilder builder, int level) {
    return PuppetConfigurationUtil.getPuppetVersion(builder.getProject()) == PuppetLanguage.Version.PUPPET_3;
  }

  public static boolean consumeOneToken(@NotNull PsiBuilder builder, int level) {
    if (builder.eof()) {
      return false;
    }

    final PsiBuilder.Marker marker = builder.mark();
    builder.advanceLexer();
    marker.error(PuppetBundle.message("puppet.parser.unknown.token"));
    return true;
  }

  public static boolean checkNoSpaceBefore(@NotNull PsiBuilder builder, int level) {
    if (builder.rawLookup(-1) == TokenType.WHITE_SPACE || builder.rawLookup(0) == TokenType.WHITE_SPACE) {
      return false;
    }
    return true;
  }

  public static boolean checkHasSpaceBefore(@NotNull PsiBuilder builder, int level, @NotNull Parser parser) {
    if (builder.rawLookup(-1) != TokenType.WHITE_SPACE && builder.rawLookup(0) != TokenType.WHITE_SPACE) {
      return false;
    }
    return parser.parse(builder, level);
  }

  public static boolean myPinParse(@NotNull PsiBuilder builder, int level, @NotNull Parser delegate) {
    report_error_(builder, delegate.parse(builder, level));
    return true;
  }

  public static boolean isIncludeClassFunction(@NotNull PsiBuilder b, int l) {
    return b.getTokenType() == NAME && ourIncludeFunctions.contains(b.getTokenText());
  }

  public static boolean passHeredocBodies(@NotNull PsiBuilder builder, int level) {
    if (!isPuppet4(builder, level)) {
      return false;
    }

    boolean hasFoundSmth = false;
    while (builder.getTokenType() == PuppetTokenTypes.HEREDOC_BODY
           || builder.getTokenType() == PuppetTokenTypes.HEREDOC_ENDING) {
      hasFoundSmth = true;
      builder.advanceLexer();
    }
    return hasFoundSmth;
  }

  public static boolean isKeyword(@NotNull PsiBuilder b, int l) {
    if (PuppetTokenTypeSets.KEYWORDS.contains(b.getTokenType())) {
      b.advanceLexer();
      return true;
    }
    return false;
  }

  public static boolean parseFileContents(@NotNull PsiBuilder b, int l, Parser parser) {
    assert b instanceof Builder;
    assert ((Builder)b).parser instanceof PuppetParser;
    return ((PuppetParser)((Builder)b).parser).parseFileContents(b, l, parser);
  }

  public static boolean recoverParameter(@NotNull PsiBuilder b, int l, Parser parser) {
    return ((PuppetParser)((Builder)b).parser).recoverParameter(b, l, parser);
  }

  public static boolean recoverTypedParameter(@NotNull PsiBuilder b, int l, Parser parser) {
    return ((PuppetParser)((Builder)b).parser).recoverTypedParameter(b, l, parser);
  }
}
