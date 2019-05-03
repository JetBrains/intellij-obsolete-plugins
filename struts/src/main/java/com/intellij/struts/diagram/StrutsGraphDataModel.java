/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.diagram;

import com.intellij.openapi.graph.builder.CachedGraphDataModel;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.Exception;
import com.intellij.struts.dom.Forward;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.GenericValue;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class StrutsGraphDataModel extends CachedGraphDataModel<StrutsObject, StrutsObject> {

  private final StrutsConfig myStrutsConfig;

  public StrutsGraphDataModel(final StrutsConfig strutsConfig) {
    myStrutsConfig = strutsConfig;
  }


  /**
   * Creates node and corresponding edge.
   *
   * @param source      source node
   * @param name        Edge name.
   * @param pathValue   WebPath.
   * @param reverse     Reverts edge direction.
   */
  protected void createEdge(final StrutsObject source, final @NonNls String name, final GenericValue<PathReference> pathValue, boolean reverse) {
    final PathReference webPath = pathValue.getValue();
    @NotNull final StrutsObject target;
    if (webPath != null) {
      target = new StrutsNodeObject(webPath);
    } else {
      final String path = pathValue.getStringValue();
      if (path == null || path.length() == 0) {
        return;
      }
      target = new StrutsNodeObject(path);
    }

    createNode(target);

    final StrutsObject edge = new StrutsEdgeObject(source.getName(), StringUtil.notNullize(name, "<invalid>"));
    if (reverse) {
      createEdge(edge, target, source);
    } else {
      createEdge(edge, source, target);
    }
  }

  @Override
  public StrutsObject createEdge(@NotNull final StrutsObject from, @NotNull final StrutsObject to) {
    return super.createEdge(from, to);
  }

  @Override
  protected void buildGraph() {
    clear();

    for (final Forward forward : myStrutsConfig.getGlobalForwards().getForwards()) {
      final String name = forward.getName().getStringValue(); //forward.getGenericInfo().getElementName(forward);
      if (name != null) {
        final StrutsObject source = new StrutsNodeObject(name, StrutsApiIcons.GlobalForwards, forward.getXmlTag());
        createNode(source);
        createEdge(source, "", forward.getPath(), false);
      }
    }

    for (final Exception exception : myStrutsConfig.getGlobalExceptions().getExceptions()) {
      final String name = exception.getKey().getStringValue(); //exception.getGenericInfo().getElementName(exception);
      if (name != null) {
        final StrutsObject source = new StrutsNodeObject(name, StrutsApiIcons.GlobalException, exception.getXmlTag());
        createNode(source);
        createEdge(source, "", exception.getPath(), false);
      }
    }

    for (final Action action : myStrutsConfig.getActionMappings().getActions()) {
      final String name = action.getPath().getStringValue(); //action.getGenericInfo().getElementName(action);
      if (name != null) {
        final StrutsObject source = new StrutsNodeObject(name, StrutsApiIcons.ActionMapping, action.getXmlTag());
        createNode(source);

        for (final Forward forward : action.getForwards()) {
          createEdge(source, forward.getName().getStringValue(), forward.getPath(), false);
        }

        for (final Exception exception : action.getExceptions()) {
          createEdge(source, exception.getKey().getStringValue(), exception.getPath(), false);
        }

        createEdge(source, "forward", action.getForward(), false);
        createEdge(source, "include", action.getInclude(), false);
        createEdge(source, "input", action.getInput(), true);
      }
    }
  }

  @Override
  @NotNull
  public String getNodeName(final StrutsObject strutsObject) {
    return strutsObject.getName();
  }

  @Override
  @NotNull
  public String getEdgeName(final StrutsObject strutsObject) {
    return strutsObject.getName();
  }
}
