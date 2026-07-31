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
package com.intellij.gwt.module;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

public final class GwtDomFileDescription extends DomFileDescription<GwtModule> {
  public GwtDomFileDescription() {
    super(GwtModule.class, "module");
  }

  @Override
  public boolean isMyFile(@NotNull XmlFile file, final Module module) {
    return file.getName().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX) && super.isMyFile(file, module);
  }

  @Override
  public boolean isAutomaticHighlightingEnabled() {
    return false;
  }
}
