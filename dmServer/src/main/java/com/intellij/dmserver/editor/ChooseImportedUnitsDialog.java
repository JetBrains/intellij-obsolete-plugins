package com.intellij.dmserver.editor;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.List;

public class ChooseImportedUnitsDialog extends ChooseElementsDialog<ExportedUnit> {

  public ChooseImportedUnitsDialog(Project project, List<? extends ExportedUnit> items, @Nls String title) {
    super(project, items, title, null, true);
  }

  @Override
  protected String getItemText(ExportedUnit item) {
    return DmServerBundle.message("ChooseImportedUnitsDialog.item.text", item.getSymbolicName(), item.getVersion().toString());
  }

  @Override
  protected Icon getItemIcon(ExportedUnit item) {
    return null;
  }
}
