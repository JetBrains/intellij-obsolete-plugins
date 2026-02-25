// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class ReferenceGutterIconRenderer extends GutterIconRenderer implements DumbAware {

  private final Navigatable myElementToNavigate;

  private final Icon myIcon;

  private final @Tooltip String myTooltip;

  public ReferenceGutterIconRenderer(@Nullable Navigatable elementToNavigate, @NotNull Icon icon) {
    this(elementToNavigate, icon, null);
  }

  public ReferenceGutterIconRenderer(@Nullable Navigatable elementToNavigate, @NotNull Icon icon, @Nullable @Tooltip String tooltip) {
    myElementToNavigate = elementToNavigate;
    myIcon = icon;
    myTooltip = tooltip;
  }

  @Override
  public String getTooltipText() {
    return myTooltip;
  }

  @Override
  public AnAction getClickAction() {
    return new AnAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        if (myElementToNavigate != null) {
          myElementToNavigate.navigate(true);
        }
      }
    };
  }

  @Override
  public boolean isNavigateAction() {
    return true;
  }

  @Override
  public @NotNull Icon getIcon() {
    return myIcon;
  }

  @Override
  public int hashCode() {
    return myElementToNavigate == null ? 1 : myElementToNavigate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ReferenceGutterIconRenderer
           && Comparing.equal(myElementToNavigate, ((ReferenceGutterIconRenderer)obj).myElementToNavigate)
           && Comparing.equal(myIcon, ((ReferenceGutterIconRenderer)obj).getIcon());
  }
}
