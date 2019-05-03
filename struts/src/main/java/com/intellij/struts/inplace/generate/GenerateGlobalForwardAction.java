/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.generate;

import com.intellij.openapi.editor.Editor;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.dom.Forward;
import com.intellij.struts.dom.GlobalForwards;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.DomElement;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class GenerateGlobalForwardAction extends GenerateMappingAction<Forward> {

  public GenerateGlobalForwardAction() {

    super(new GenerateMappingProvider<Forward>(StrutsBundle.message("generate.global.forward"), Forward.class, "struts-forward", GlobalForwards.class, StrutsConfig.class) {

      @Override
      public Forward generate(@Nullable final DomElement parent, final Editor editor) {
        final GlobalForwards mappings;
        if (parent instanceof StrutsConfig) {
          mappings = ((StrutsConfig)parent).getGlobalForwards();
          mappings.ensureTagExists();
        }
        else if (parent instanceof GlobalForwards) {
          mappings = (GlobalForwards)parent;
        }
        else {
          return null;
        }
        return mappings.addForward();
      }

    }, StrutsApiIcons.GlobalForwards);
  }
}
