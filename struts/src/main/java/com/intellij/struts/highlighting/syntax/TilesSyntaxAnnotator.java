/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.highlighting.syntax;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.struts.dom.tiles.Add;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.dom.tiles.Put;
import com.intellij.struts.dom.tiles.TilesDefinitions;

/**
 * Provides additional syntax highlighting for Tiles definitions files.
 *
 * @author Yann C�bron
 */
public class TilesSyntaxAnnotator extends DomAnnotatorComponentBase<TilesDefinitions> {

  public TilesSyntaxAnnotator() {
    super(TilesDefinitions.class);
  }

  @Override
  protected DomAnnotatorVisitor buildVisitor(final AnnotationHolder holder) {
    return new DomAnnotatorVisitor(holder) {

      public void visitAdd(final Add add) {
        checkDeprecatedAttribute(add.getContent(), add.getAttributeValue(), null);
        checkDeprecatedAttribute(add.getDirect(), add.getType(), "string");

        checkMutuallyExclusiveAttributes(add.getContent(), add.getAttributeValue());
      }

      public void visitDefinition(final Definition definition) {
        checkDeprecatedAttribute(definition.getPath(), definition.getTemplate(), null);

        checkMutuallyExclusiveAttributes(definition.getControllerClass(), definition.getControllerUrl());
        checkMutuallyExclusiveAttributes(definition.getPage(), definition.getPath());

        // TODO temporary fix for STRUTS-129
        final Definition extendingDefinition = definition.getExtends().getValue();
        if (extendingDefinition != null) {
          if (extendingDefinition.ensureTagExists() == definition.getXmlTag()) {
            holder.createErrorAnnotation(definition.ensureTagExists(), "Definition cannot extend itself");
          }
        }
      }

      public void visitPut(final Put put) {
        checkDeprecatedAttribute(put.getContent(), put.getAttributeValue(), null);
        checkDeprecatedAttribute(put.getDirect(), put.getType(), "string");

        checkMutuallyExclusiveAttributes(put.getContent(), put.getAttributeValue());
      }
    };
  }
}
