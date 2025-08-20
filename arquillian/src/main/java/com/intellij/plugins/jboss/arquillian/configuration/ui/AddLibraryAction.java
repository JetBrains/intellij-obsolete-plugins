package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.intellij.openapi.util.NlsActions.ActionText;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianLibraryModel;

import javax.swing.*;
import java.util.Collection;

abstract class AddLibraryAction {
  private final Icon icon;
  private final @ActionText String text;

  AddLibraryAction(Icon icon, @ActionText String text) {
    this.icon = icon;
    this.text = text;
  }

  Icon getIcon() {
    return icon;
  }

  @ActionText
  String getText() {
    return text;
  }

  abstract Collection<ArquillianLibraryModel> execute();
}
