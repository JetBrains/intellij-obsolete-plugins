/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RegExpValidator;
import com.intellij.refactoring.rename.RenameInputValidator;
import com.intellij.util.ProcessingContext;

public class ActionRenameInputValidator implements RenameInputValidator {
  private final RegExpValidator myValidator;

  public ActionRenameInputValidator() {
    myValidator = new RegExpValidator("/[\\d\\w\\_\\.\\-/]+");
  }

  @Override
  public ElementPattern<? extends PsiElement> getPattern() {
    return XmlPatterns.xmlTag().withNamespace(StrutsConstants.STRUTS_DTDS).withLocalName("action");
  }

  @Override
  public boolean isInputValid(final String newName, final PsiElement element, final ProcessingContext context) {
    return myValidator.value(newName);
  }
}
