/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
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

package com.intellij.gwt.jsinject.parser;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.javascript.DialectOptionHolder;
import com.intellij.lang.javascript.JSLanguageDialect;
import com.intellij.lang.javascript.JavascriptLanguage;
import com.intellij.lang.javascript.JsDialectWithJavaScriptFormatterSettings;
import org.jetbrains.annotations.NotNull;

public class GwtLanguageDialect extends JSLanguageDialect implements DependentLanguage,
                                                                     JsDialectWithJavaScriptFormatterSettings {
  public static final DialectOptionHolder DIALECT_OPTION_HOLDER = new DialectOptionHolder("GWT");
  public static final JSLanguageDialect GWT_DIALECT = new GwtLanguageDialect();

  private GwtLanguageDialect() {
    super("GWT JavaScript", DIALECT_OPTION_HOLDER);
  }

  @Override
  public boolean isAtLeast(@NotNull JSLanguageDialect other) {
    return super.isAtLeast(other) || JavascriptLanguage.INSTANCE.isAtLeast(other);
  }
}
