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

package com.intellij.struts.dom;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.StrutsManager;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.xml.DomElement;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Set;

/**
 * DOM FileDescription for struts-config.
 *
 * @author Dmitry Avdeev
 */
public class StrutsDomFileDescription extends StrutsFileDescriptionBase<StrutsConfig> {
  private static class Lazy {
    private static final LayeredIcon STRUTS_CONFIG_FILE = JBUI.scale(new LayeredIcon(2));
    static {
      STRUTS_CONFIG_FILE.setIcon(StdFileTypes.XML.getIcon(), 0);
      STRUTS_CONFIG_FILE.setIcon(StrutsApiIcons.ActionMapping_small, 1, 6, 7);
    }
  }

  public StrutsDomFileDescription() {
    super(StrutsConfig.class, StrutsConfig.STRUTS_CONFIG);
  }

  @Override
  public Icon getFileIcon(@Iconable.IconFlags int flags) {
    return Lazy.STRUTS_CONFIG_FILE;
  }

  @Override
  @NotNull
  protected Set<XmlFile> getFilesToMerge(final DomElement element) {
    return StrutsManager.getInstance().getStrutsConfigFiles(element.getXmlElement());
  }

}
