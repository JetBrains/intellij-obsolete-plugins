/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

package com.intellij.gwt.i18n;

import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;

public class PropertiesFilesListCellRenderer extends PsiElementListCellRenderer<PropertiesFileImpl> {
  @Override
  public String getElementText(final PropertiesFileImpl element) {
    return element.getName();
  }

  @Override
  protected String getContainerText(final PropertiesFileImpl element, final String name) {
    return null;
  }
}
