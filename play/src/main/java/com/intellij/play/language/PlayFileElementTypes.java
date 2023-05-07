/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.play.language.groovy.GroovyDeclarationsInPlayFileRoot;
import com.intellij.play.language.groovy.PlayGroovyOuterElementType;
import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.OuterLanguageElementType;

public final class PlayFileElementTypes {
  public static final IElementType OUTER_ELEMENT_TYPE = new OuterLanguageElementType("PLAY_FRAGMENT", PlayLanguage.INSTANCE);
  public static final PlayGroovyOuterElementType OUTER_GROOVY_EXPRESSION_ELEMENT_TYPE = new PlayGroovyOuterElementType();

  public static final TemplateDataElementType TEMPLATE_DATA =
    new TemplateDataElementType("PLAY_TEMPLATE_DATA", PlayLanguage.INSTANCE, PlayElementTypes.TEMPLATE_TEXT, OUTER_ELEMENT_TYPE);

 public static final GroovyDeclarationsInPlayFileRoot GROOVY_DATA = new GroovyDeclarationsInPlayFileRoot("GROOVY_ROOT");

  private PlayFileElementTypes() {
  }
}
