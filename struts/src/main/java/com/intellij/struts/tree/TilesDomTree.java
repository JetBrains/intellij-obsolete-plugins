/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
