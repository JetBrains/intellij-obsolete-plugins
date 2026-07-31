/*
 * @author max
 */
package com.intellij.gwt.jsinject.parser;

import com.intellij.lang.javascript.refactoring.JSNamesValidator;

public final class GwtNamesValidator extends JSNamesValidator {
  public GwtNamesValidator() {
    super(GwtLanguageDialect.DIALECT_OPTION_HOLDER);
  }
}