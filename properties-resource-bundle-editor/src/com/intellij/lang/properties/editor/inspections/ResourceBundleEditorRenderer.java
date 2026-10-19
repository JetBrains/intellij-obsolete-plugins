// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.properties.editor.inspections;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElementWrapper;
import com.intellij.lang.properties.editor.TextAttributesPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Renders properties with the text attributes of their {@link TextAttributesPresentation}.
 * <p>
 * Computing the presentation reads PSI and runs the incompleteness checks, which is not allowed while painting,
 * so it is computed in the background and cached. Until it is available the node's own presentation is rendered.
 * The cache is invalidated whenever the tree model changes; stale entries are still rendered while they are recomputed,
 * so the tree doesn't flicker.
 *
 * @author Dmitry Batkovich
 */
public class ResourceBundleEditorRenderer extends NodeRenderer {
  private record CachedPresentation(@Nullable String text, @Nullable TextAttributes attributes,
                                    @NotNull EditorColorsScheme scheme, int generation) {
  }

  private final JTree myTree;
  private final Disposable myParentDisposable;
  // accessed on EDT only
  private final Map<TreeElement, CachedPresentation> myCache = new WeakHashMap<>();
  private final Map<TreeElement, Integer> myPending = new WeakHashMap<>();
  private int myGeneration;

  public ResourceBundleEditorRenderer(@NotNull JTree tree, @NotNull Disposable parentDisposable) {
    myTree = tree;
    myParentDisposable = parentDisposable;
    TreeModelListener invalidator = new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) { myGeneration++; }

      @Override
      public void treeNodesInserted(TreeModelEvent e) { myGeneration++; }

      @Override
      public void treeNodesRemoved(TreeModelEvent e) { myGeneration++; }

      @Override
      public void treeStructureChanged(TreeModelEvent e) { myGeneration++; }
    };
    tree.getModel().addTreeModelListener(invalidator);
    PropertyChangeListener modelChangeListener = e -> {
      if (e.getOldValue() instanceof TreeModel oldModel) oldModel.removeTreeModelListener(invalidator);
      if (e.getNewValue() instanceof TreeModel newModel) newModel.addTreeModelListener(invalidator);
      myGeneration++;
    };
    tree.addPropertyChangeListener(JTree.TREE_MODEL_PROPERTY, modelChangeListener);
  }

  @Override
  public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    if (customize(value)) {
      return;
    }
    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
  }

  private boolean customize(Object value) {
    if (!(value instanceof DefaultMutableTreeNode node) || !(node.getUserObject() instanceof TreeElementWrapper wrapper)) {
      return false;
    }
    final TreeElement treeElement = wrapper.getValue();
    if (treeElement == null) {
      return false;
    }
    final EditorColorsScheme scheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme();
    final CachedPresentation cached = myCache.get(treeElement);
    if (cached == null || cached.generation() != myGeneration || cached.scheme() != scheme) {
      scheduleComputation(treeElement, scheme);
    }
    if (cached == null || cached.text() == null) {
      return false;
    }
    SimpleTextAttributes attr = SimpleTextAttributes.fromTextAttributes(cached.attributes());
    append(cached.text(), new SimpleTextAttributes(attr.getBgColor(), attr.getFgColor(), attr.getWaveColor(),
                                                   attr.getStyle() | SimpleTextAttributes.STYLE_OPAQUE));
    return true;
  }

  private void scheduleComputation(@NotNull TreeElement treeElement, @NotNull EditorColorsScheme scheme) {
    final int generation = myGeneration;
    final Integer pending = myPending.get(treeElement);
    if (pending != null && pending == generation) {
      return;
    }
    myPending.put(treeElement, generation);
    ReadAction.nonBlocking(() -> compute(treeElement, scheme, generation))
      .expireWith(myParentDisposable)
      .finishOnUiThread(ModalityState.any(), result -> {
        final Integer stillPending = myPending.get(treeElement);
        if (stillPending != null && stillPending == generation) {
          myPending.remove(treeElement);
        }
        myCache.put(treeElement, result);
        myTree.repaint();
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  private static @NotNull CachedPresentation compute(@NotNull TreeElement treeElement,
                                                     @NotNull EditorColorsScheme scheme,
                                                     int generation) {
    final ItemPresentation presentation = treeElement.getPresentation();
    if (presentation instanceof TextAttributesPresentation textAttributesPresentation) {
      final String text = textAttributesPresentation.getPresentableText();
      if (text != null) {
        return new CachedPresentation(text, textAttributesPresentation.getTextAttributes(scheme), scheme, generation);
      }
    }
    return new CachedPresentation(null, null, scheme, generation);
  }
}
