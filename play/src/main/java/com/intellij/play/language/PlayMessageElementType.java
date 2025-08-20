package com.intellij.play.language;

import com.intellij.play.language.groovy.GroovyExpressionLazyParseableElementType;

public class PlayMessageElementType extends GroovyExpressionLazyParseableElementType {
  public PlayMessageElementType() {
    super("MESSAGE_EXPRESSION");
  }
}
