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
