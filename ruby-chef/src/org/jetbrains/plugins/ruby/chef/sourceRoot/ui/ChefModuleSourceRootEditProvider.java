package org.jetbrains.plugins.ruby.chef.sourceRoot.ui;

import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.ui.JBColor;
import icons.RubyChefIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefBundle;
import org.jetbrains.plugins.ruby.chef.sourceRoot.CookbooksRootType;
import org.jetbrains.plugins.ruby.settings.RubyModuleSourceRootEditProvider;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public final class ChefModuleSourceRootEditProvider extends RubyModuleSourceRootEditProvider {
  private ChefModuleSourceRootEditProvider() {
    super(CookbooksRootType.COOKBOOKS);
  }

  @Override
  public @NotNull String getRootTypeName() {
    return ChefBundle.message("cookbooks.source.root.name");
  }

  @Override
  public @NotNull Icon getRootIcon() {
    return RubyChefIcons.ChefCookbook;
  }

  @Override
  public @Nullable Icon getFolderUnderRootIcon() {
    return null;
  }

  @Override
  public @Nullable CustomShortcutSet getMarkRootShortcutSet() {
    return new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
  }

  @Override
  public @NotNull String getRootsGroupTitle() {
    return ChefBundle.message("cookbooks.roots.group.name");
  }

  @Override
  public @NotNull Color getRootsGroupColor() {
    return new JBColor(new Color(67, 84, 100), new Color(242, 139, 32));
  }

  @Override
  public @NotNull String getUnmarkRootButtonText() {
    return ChefBundle.message("cookbooks.source.roots.unmark.text");
  }
}
