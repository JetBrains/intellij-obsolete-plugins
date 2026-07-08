package com.intellij.lang.puppet;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser;
import org.jetbrains.annotations.NotNull;

public class PuppetParser extends PuppetParserGenerated {
  public boolean parseFileContents(@NotNull PsiBuilder b, int l, Parser parser) {
    return parser.parse(b, l);
  }

  public boolean recoverParameter(@NotNull PsiBuilder b, int l, Parser parser) {
    return parser.parse(b, l);
  }

  public boolean recoverTypedParameter(@NotNull PsiBuilder b, int l, Parser parser) {
    return parser.parse(b, l);
  }
}
