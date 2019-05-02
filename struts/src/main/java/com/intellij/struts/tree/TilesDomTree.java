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

package com.intellij.struts.tree;

import com.intellij.javaee.model.xml.web.WebApp;
import com.intellij.openapi.project.Project;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.Icon;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.struts.dom.tiles.*;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Setup DOM-Tree for Tiles config files.
 *
 * @author Dmitry Avdeev
 */
public class TilesDomTree extends StrutsTreeBase<TilesDefinitions, TilesModel> {

  private final static Map<Class, Boolean> hiders = new HashMap<>();

  private final static List<Class> consolidated =
    Arrays.asList(new Class[]{Definition.class, Put.class, PutList.class, Item.class, Bean.class, Add.class}

    );

  static {
    hiders.put(DomElement.class, true);
    hiders.put(GenericDomValue.class, false);
    hiders.put(Icon.class, false);
  }

  public TilesDomTree(final Project project) {
    super(project, StrutsProjectComponent.getInstance(project).getTilesFactory(), hiders, consolidated, null,
          Arrays.asList(TilesDefinitions.class, StrutsConfig.class, WebApp.class));
  }
}
