/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.generate;

import com.intellij.openapi.editor.Editor;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.ActionMappings;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.DomElement;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class GenerateActionMappingAction extends GenerateMappingAction<Action> {

  public GenerateActionMappingAction() {

    super(new GenerateMappingProvider<Action>(StrutsBundle.message("generate.action"), Action.class, "struts-action-mapping", ActionMappings.class, StrutsConfig.class) {

      @Override
      public Action generate(@Nullable final DomElement parent, final Editor editor) {
        final ActionMappings mappings;
        if (parent instanceof StrutsConfig) {
          mappings = ((StrutsConfig)parent).getActionMappings();
          mappings.ensureTagExists();
        }
        else if (parent instanceof ActionMappings) {
          mappings = (ActionMappings)parent;
        }
        else {
          return null;
        }
        return mappings.addAction();
      }

    }, StrutsApiIcons.ActionMapping);
  }

}
