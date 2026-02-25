// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.SortedComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.structure.GrailsApplication;

import java.util.Collection;

import static org.jetbrains.plugins.grails.structure.UtilKt.COMPARATOR;

public class GrailsApplicationCombobox extends ComboBox<GrailsApplication> {

  private final SortedComboBoxModel<GrailsApplication> myModel;
  private boolean myEmptySelectionAllowed = true;

  @SuppressWarnings("unused") // used in org/jetbrains/plugins/grails/runner/ui/GrailsRunConfigurationEditor.form
  public GrailsApplicationCombobox() {
    this(new SortedComboBoxModel<>(COMPARATOR));
  }

  private GrailsApplicationCombobox(final SortedComboBoxModel<GrailsApplication> model) {
    super(model);
    myModel = model;
    ComboboxSpeedSearch search = new ComboboxSpeedSearch(this, null) {
      @Override
      protected String getElementText(Object element) {
        if (element instanceof Module) {
          return ((Module)element).getName();
        }
        else if (element == null) {
          return "";
        }
        return super.getElementText(element);
      }
    };
    search.setupListeners();
    setRenderer(SimpleListCellRenderer.create((label, grailsApplication, index) -> {
      if (grailsApplication == null) {
        label.setText(GrailsBundle.message("combobox.label.none.selected"));
      }
      else {
        label.setIcon(grailsApplication.getIcon());
        @NlsSafe String applicationName = grailsApplication.getName();
        label.setText(applicationName);
        @NlsSafe String path = grailsApplication.getRoot().getPath();
        label.setToolTipText(path);
      }
    }));
  }

  public void disallowEmptySelection() {
    myEmptySelectionAllowed = false;
  }

  public void setApplications(@NotNull Collection<GrailsApplication> grailsApplications) {
    myModel.clear();
    if (myEmptySelectionAllowed) {
      myModel.add(null);
    }
    myModel.setAll(grailsApplications);
  }

  public @Nullable GrailsApplication getSelectedApplication() {
    return myModel.getSelectedItem();
  }

  public void setSelectedApplication(@Nullable GrailsApplication grailsApplication) {
    myModel.setSelectedItem(grailsApplication);
  }
}
