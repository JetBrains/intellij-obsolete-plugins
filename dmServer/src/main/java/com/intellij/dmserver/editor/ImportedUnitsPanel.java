package com.intellij.dmserver.editor;

import com.intellij.dmserver.artifacts.ManifestUpdater;
import com.intellij.dmserver.editor.wrapper.ClauseWrapper;
import com.intellij.dmserver.editor.wrapper.HeaderWrapper;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.dmserver.util.VersionUtils;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SortedListModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.osgi.framework.Constants;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * @author michael.golubev
 */
public abstract class ImportedUnitsPanel {
  private final static Comparator<ImportedUnit> UNITS_COMPARATOR =
    Comparator.comparing(ImportedUnit::getSymbolicName).thenComparing(o -> o.getVersionRange().toString());

  private JPanel myMainPanel;
  private JList<ImportedUnit> myUnitsList;
  private JButton myAddButton;
  private JButton myRemoveButton;
  private JLabel myTitleLabel;
  private JButton myAttributesButton;

  private Project myProject;
  private ManifestFile myManifestFile;
  private ManifestUpdater myManifestUpdater;

  private final SortedListModel<ImportedUnit> myUnitsListModel;

  private boolean myInSave = false;

  public ImportedUnitsPanel() {
    myUnitsListModel = SortedListModel.create(UNITS_COMPARATOR);
    myUnitsList.setModel(myUnitsListModel);
    myUnitsList.setCellRenderer(new ImportedUnitRenderer());
    myUnitsList.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        updateEnablement();
      }
    });

    myAddButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        onAdd();
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        onRemove();
      }
    });
    myAttributesButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        onAttributes();
      }
    });
  }

  private void updateEnablement() {
    boolean someUnitsSelected = myUnitsList.getSelectedIndex() >= 0;
    myRemoveButton.setEnabled(someUnitsSelected);
    myAttributesButton.setEnabled(someUnitsSelected);
  }

  public void init(@NotNull Project project, @NotNull ManifestFile manifestFile) {
    myProject = project;
    myManifestFile = manifestFile;
    myManifestUpdater = new ManifestUpdater(manifestFile);

    myTitleLabel.setText(getListTitle());

    load();

    updateEnablement();
  }

  private Project getProject() {
    return myProject;
  }

  public void notifyFileChanged() {
    if (myInSave) {
      return;
    }

    load();
  }

  private void load() {
    myUnitsListModel.clear();
    HeaderWrapper importHeaderWrapper = new HeaderWrapper(myManifestFile, getHeaderName());
    for (ClauseWrapper clause : importHeaderWrapper.getClauses()) {
      myUnitsListModel.add(
        new ImportedUnitImpl(clause.getName(),
                             VersionUtils.loadVersionRange(clause.getAttributeValue(ManifestUtils.VERSION_RANGE_ATTRIBUTE_NAME)),
                             Constants.RESOLUTION_OPTIONAL.equals(clause.getDirectiveValue(Constants.RESOLUTION_DIRECTIVE))));
    }
  }

  private void save() {
    myInSave = true;
    try {
      StringBuilder headerValue = new StringBuilder();
      for (ImportedUnit existingUnit : myUnitsListModel.getItems()) {
        if (headerValue.length() > 0) {
          headerValue.append(",\n ");
        }
        headerValue.append(existingUnit.getSymbolicName());

        if (!VersionUtils.emptyRange.equals(existingUnit.getVersionRange())) {
          headerValue.append(";");
          headerValue.append(ManifestUtils.VERSION_RANGE_ATTRIBUTE_NAME);
          headerValue.append("=\"");
          headerValue.append(existingUnit.getVersionRange().toString());
          headerValue.append("\"");
        }

        if (existingUnit.isOptional()) {
          headerValue.append(";");
          headerValue.append(Constants.RESOLUTION_DIRECTIVE);
          headerValue.append(":=");
          headerValue.append(Constants.RESOLUTION_OPTIONAL);
        }
      }
      myManifestUpdater.updateHeader(getHeaderName(), headerValue.toString(), false);
    }
    finally {
      myInSave = false;
    }
  }

  private void onAdd() {
    final Map<String, ExportedUnit> nameVersionKey2unit = new HashMap<>();
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      Set<String> existingUnitSymbolicNames = new HashSet<>();
      for (ImportedUnit existingUnit : myUnitsListModel.getItems()) {
        existingUnitSymbolicNames.add(existingUnit.getSymbolicName());
      }
      existingUnitSymbolicNames.addAll(getOwnSymbolicNames(myManifestFile));

      AvailableBundlesProvider provider = AvailableBundlesProvider.getInstance(getProject());
      provider.resetAll();
      UnitsCollector unitsCollector = getUnitsCollector(provider);
      for (ExportedUnit availableUnit : unitsCollector.getAvailableUnits()) {
        if (existingUnitSymbolicNames.contains(availableUnit.getSymbolicName())) {
          continue;
        }
        nameVersionKey2unit.put(availableUnit.getSymbolicName() + "###" + availableUnit.getVersion(), availableUnit);
      }
    }, getCollectingStatus(), true, getProject())) {
      return;
    }

    ChooseImportedUnitsDialog addDialog =
      new ChooseImportedUnitsDialog(getProject(), new ArrayList<>(nameVersionKey2unit.values()), getAddDialogTitle());
    if (!addDialog.showAndGet()) {
      return;
    }

    for (ExportedUnit addedUnit : addDialog.getChosenElements()) {
      myUnitsListModel.add(new ImportedUnitImpl(addedUnit.getSymbolicName(),
                                                VersionUtils.version2range(addedUnit.getVersion()),
                                                false));
    }

    save();
  }

  private void onRemove() {
    for (ImportedUnit selectedUnitElement : myUnitsList.getSelectedValuesList()) {
      myUnitsListModel.remove(selectedUnitElement);
    }
    myUnitsList.setSelectedIndices(new int[0]);

    save();
  }

  private void onAttributes() {
    List<ImportedUnit> selectedUnits = myUnitsList.getSelectedValuesList();
    ImportedUnitAttributesDialog attributesDialog = new ImportedUnitAttributesDialog(getProject(), selectedUnits);
    if (!attributesDialog.showAndGet()) {
      return;
    }
    save();
  }

  @Nls
  protected abstract String getListTitle();

  @NonNls
  protected abstract String getHeaderName();

  @Nls
  protected abstract String getAddDialogTitle();

  @Nls
  protected abstract String getCollectingStatus();

  protected abstract UnitsCollector getUnitsCollector(AvailableBundlesProvider provider);

  protected abstract Collection<String> getOwnSymbolicNames(ManifestFile manifest);

  private static class ImportedUnitRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      JLabel component = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      ImportedUnit importedUnit = (ImportedUnit)value;
      component.setText(DmServerBundle.message(importedUnit.isOptional()
                                               ? "ImportedUnitsPanel.list.item.optional"
                                               : "ImportedUnitsPanel.list.item",
                                               importedUnit.getSymbolicName(), importedUnit.getVersionRange().toString()));
      return component;
    }
  }
}
