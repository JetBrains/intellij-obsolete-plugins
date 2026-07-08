package com.intellij.lang.puppet;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser;
import org.jetbrains.annotations.NotNull;

public class PuppetHeredocParser extends PuppetParser {
  public static final PuppetHeredocParser INSTANCE = new PuppetHeredocParser();

  @Override
  public boolean parseFileContents(@NotNull PsiBuilder b, int l, Parser parser) {
    return quoted_text(b, l);
  }
}
