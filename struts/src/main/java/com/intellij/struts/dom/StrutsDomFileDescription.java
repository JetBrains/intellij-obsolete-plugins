/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
