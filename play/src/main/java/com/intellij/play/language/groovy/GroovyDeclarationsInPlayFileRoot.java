/*
 * Copyright 2000-2007 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.play.language.groovy;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.play.language.lexer.PlayScriptLexer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyLanguage;

public class GroovyDeclarationsInPlayFileRoot extends IFileElementType {
  public GroovyDeclarationsInPlayFileRoot(String debugName) {
    super(debugName, GroovyLanguage.INSTANCE);
  }

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
    final PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(psi.getProject(), new PlayScriptLexer(), chameleon);
    return new PlayAwareGroovyParser().parse(this, builder).getFirstChildNode();
  }
}
