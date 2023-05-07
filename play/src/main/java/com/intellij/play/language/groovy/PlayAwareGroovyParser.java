/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.play.language.groovy;

import com.intellij.lang.PsiBuilder;
import com.intellij.play.language.PlayFileElementTypes;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.groovy.lang.parser.GroovyParser;

import static com.intellij.play.language.PlayFileElementTypes.GROOVY_DATA;

public class PlayAwareGroovyParser extends GroovyParser {

  @Override
  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    if (t == GROOVY_DATA) {
      return block_levels(b, 0);
    }
    else {
      throw new IllegalArgumentException("Unexpected element type: " + t);
    }
  }

  @Override
  public boolean parseDeep() {
    return true;
  }

  @Override
  protected boolean isExtendedSeparator(final IElementType tokenType) {
    return tokenType == PlayFileElementTypes.OUTER_GROOVY_EXPRESSION_ELEMENT_TYPE;
  }

  @Override
  protected boolean parseExtendedStatement(PsiBuilder builder) {
    IElementType tt = builder.getTokenType();
    if (tt == PlayFileElementTypes.OUTER_GROOVY_EXPRESSION_ELEMENT_TYPE) {
      builder.advanceLexer();
      return true;
    }

    return false;
  }
}
