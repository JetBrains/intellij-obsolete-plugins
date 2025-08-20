package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainer;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

class ScopeNode extends MasterDetailsComponent.MyNode {
  private boolean visible;

  ScopeNode(ArquillianContainer.Scope scope) {
    super(new NamedConfigurable(false, null) {
      @Override
      public Object getEditableObject() {
        return null;
      }

      @Override
      public String getBannerSlogan() {
        return null;
      }

      @Override
      public JComponent createOptionsPanel() {
        return new JPanel();
      }

      @Nls
      @Override
      public String getDisplayName() {
        return scope.getDescription().get();
      }

      @Override
      public void setDisplayName(String name) {
      }

      @Override
      public boolean isModified() {
        return false;
      }

      @Override
      public void apply() {
      }
    }, true);
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }
}
