package com.intellij.dmserver.editor;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.PsiTreeChangedAdapter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.ManifestFile;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

public class ManifestFileEditor extends UserDataHolderBase implements FileEditor {

  private JPanel myMainPanel;
  private ImportedUnitsPanel myImportedPackagesPanel;
  private ImportedUnitsPanel myImportedBundlesPanel;
  private ImportedUnitsPanel myImportedLibrariesPanel;

  private final List<ImportedUnitsPanel> myImportedUnitsPanels;

  private final ManifestFile myManifestFile;

  public ManifestFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
    myManifestFile = (ManifestFile)PsiManager.getInstance(project).findFile(file);

    myImportedUnitsPanels = Arrays.asList(myImportedPackagesPanel, myImportedBundlesPanel, myImportedLibrariesPanel);

    for (ImportedUnitsPanel importedUnitsPanel : myImportedUnitsPanels) {
      importedUnitsPanel.init(project, myManifestFile);
    }

    PsiManager psiManager = PsiManager.getInstance(project);
    PsiTreeChangeListener psiTreeChangeListener = new PsiTreeChangedAdapter() {

      @Override
      protected void treeChanged(PsiTreeChangeEvent event) {
        if (myManifestFile != event.getFile()) {
          return;
        }

        for (ImportedUnitsPanel importedUnitsPanel : myImportedUnitsPanels) {
          importedUnitsPanel.notifyFileChanged();
        }
      }
    };
    psiManager.addPsiTreeChangeListener(psiTreeChangeListener, this);
  }

  private void createUIComponents() {
    myImportedPackagesPanel = new ImportedPackagesPanel();
    myImportedBundlesPanel = new ImportedBundlesPanel();
    myImportedLibrariesPanel = new ImportedLibrariesPanel();
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMainPanel;
  }

  @NotNull
  @Override
  public String getName() {
    return DmServerBundle.message("ManifestFileEditor.name");
  }

  @Override
  public void setState(@NotNull FileEditorState state) {

  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void selectNotify() {

  }

  @Override
  public void deselectNotify() {

  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

  }

  @Override
  public FileEditorLocation getCurrentLocation() {
    return new FileEditorLocation() {

      @Override
      @NotNull
      public FileEditor getEditor() {
        return ManifestFileEditor.this;
      }

      @Override
      public int compareTo(final FileEditorLocation fileEditorLocation) {
        return 0;
      }
    };
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return null;
  }

  @Override
  public @NotNull VirtualFile getFile() {
    return myManifestFile.getVirtualFile();
  }

  @Override
  public void dispose() {

  }
}
