package com.intellij.dmserver.editor;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.VersionUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.osgi.framework.VersionRange;

import javax.swing.*;
import java.util.Iterator;
import java.util.List;

/**
 * @author michael.golubev
 */
public class ImportedUnitAttributesDialog extends DialogWrapper {

  private JTextField myVersionRangeTextField;
  private JPanel myMainPanel;
  private JCheckBox myOptionalCheckBox;

  private final List<ImportedUnit> myImportedUnits;

  public ImportedUnitAttributesDialog(Project project, List<ImportedUnit> importedUnits) {
    super(project);
    myImportedUnits = importedUnits;

    setTitle(DmServerBundle.message("ImportedUnitAttributesDialog.title"));

    Iterator<ImportedUnit> itImportedUnit = importedUnits.iterator();
    VersionRange initialVersionRange = itImportedUnit.next().getVersionRange();
    while (itImportedUnit.hasNext()) {
      if (!initialVersionRange.equals(itImportedUnit.next().getVersionRange())) {
        initialVersionRange = null;
        break;
      }
    }
    if (initialVersionRange != null) {
      myVersionRangeTextField.setText(initialVersionRange.toString());
    }

    boolean initialOptional = true;
    for (ImportedUnit importedUnit : importedUnits) {
      if (!importedUnit.isOptional()) {
        initialOptional = false;
        break;
      }
    }
    myOptionalCheckBox.setSelected(initialOptional);

    init();
  }

  @Override
  protected void doOKAction() {
    boolean keepVersionRanges = "".equals(myVersionRangeTextField.getText());
    VersionRange versionRange = null;
    if (!keepVersionRanges) {
      versionRange = VersionUtils.parseVersionRange(myVersionRangeTextField.getText());
      if (versionRange == null) {
        setErrorText(DmServerBundle.message("ImportedUnitAttributesDialog.error.version.range.format.not.matched"), myVersionRangeTextField);
        return;
      }
    }

    for (ImportedUnit importedUnit : myImportedUnits) {
      if (!keepVersionRanges) {
        importedUnit.setVersionRange(versionRange);
      }
      importedUnit.setOptional(myOptionalCheckBox.isSelected());
    }

    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }
}
