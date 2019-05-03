/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.generate;

import com.intellij.openapi.editor.Editor;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.Forward;
import com.intellij.util.xml.DomElement;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class GenerateForwardAction extends GenerateMappingAction<Forward> {

  public GenerateForwardAction() {

    super(new GenerateMappingProvider<Forward>(StrutsBundle.message("generate.forward"), Forward.class, "struts-forward", Action.class) {

      @Override
      public Forward generate(@Nullable final DomElement parent, final Editor editor) {
        if (parent instanceof Action) {
          return ((Action)parent).addForward();
        }
        return null;
      }

    }, StrutsApiIcons.Forward);
  }
}
