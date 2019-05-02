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
