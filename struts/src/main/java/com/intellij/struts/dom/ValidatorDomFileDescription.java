/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.struts.dom.validator.FormValidation;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.xml.DomElement;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Set;

/**
 * DOM FileDescription for validator plugin.
 *
 * @author Dmitry Avdeev
 */
public class ValidatorDomFileDescription extends StrutsPluginDescriptorBase<FormValidation> {

  private static class Lazy {
    private static final LayeredIcon VALIDATOR_CONFIG_FILE = JBUI.scale(new LayeredIcon(2));

    static {
      VALIDATOR_CONFIG_FILE.setIcon(StdFileTypes.XML.getIcon(), 0);
      VALIDATOR_CONFIG_FILE.setIcon(StrutsApiIcons.Validator.Validator_small, 1, 6, 7);
    }
  }

  public ValidatorDomFileDescription() {
    super(FormValidation.class, FormValidation.FORM_VALIDATION);
  }

  @Override
  public Icon getFileIcon(@Iconable.IconFlags int flags) {
    return Lazy.VALIDATOR_CONFIG_FILE;
  }

  @Override
  @NotNull
  protected Set<XmlFile> getFilesToMerge(final DomElement element) {
    return StrutsProjectComponent.getInstance(element.getManager().getProject()).getValidatorFactory()
      .getConfigFiles(element.getXmlElement());
  }
}
