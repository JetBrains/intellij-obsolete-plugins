// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.perspectives.create;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiClass;
import com.intellij.ui.EditorComboBoxEditor;
import com.intellij.ui.EditorComboBoxRenderer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.StringComboboxEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassNode;
import org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.grails.references.domain.DomainClassUtils;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.Relation;

public class CreateNewRelation extends DialogWrapper {
  private JPanel myContentPane;
  private JRadioButton myBelongsToRadioButton;
  private ComboBox myNameComboBox;
  private JRadioButton myHasManyRadioButton;
  private JLabel myRelationFieldNameLable;
  private JRadioButton myStableRadioButton;

  private final EventListenerList myListenerList = new EventListenerList();
  private final DomainClassNode mySource;
  private final Project myProject;
  private Relation edgeRelationType;

  private final @NotNull String newNodeShortTypeText;
  private final GrTypeDefinition mySourceTypeDefinition;
  private final PsiClass myTargetTypeDefinition;

  public CreateNewRelation(DomainClassNode source, DomainClassNode target, Project project) {
    super(project, true);
    mySource = source;
    myProject = project;
    setModal(true);
    myTargetTypeDefinition = target.getTypeDefinition();
    newNodeShortTypeText = myTargetTypeDefinition.getName();
    mySourceTypeDefinition = (GrTypeDefinition) mySource.getTypeDefinition();
    assert newNodeShortTypeText != null;

    setUpLabel(myRelationFieldNameLable);
    setUpNameComboBox();
    setTitle(GrailsBundle.message("create.relation"));

    init();
    updateOkStatus();

    myContentPane.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myNameComboBox, true));
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


  }

  private void setUpLabel(JLabel relationFieldNameLabel) {
    relationFieldNameLabel.setText(GrailsBundle.message("add.field.to.class", mySource.getTypeDefinition().getName()));
  }

  @Override
  protected void doOKAction() {
    assert mySource != null;
    String enteredName = getEnteredName();
    if (enteredName == null) return;
    RelationsCreationsProvider creationsProvider = new RelationsCreationsProvider(myTargetTypeDefinition.getQualifiedName(), mySourceTypeDefinition, myProject,
                                                                                  enteredName);

    int exitCode = OK_EXIT_CODE;

    if (myBelongsToRadioButton.isSelected()) {
      if (!creationsProvider.canCreateBelongsToRelation()) {
        exitCode = Messages.showDialog(myProject, GrailsBundle.message("Relation.already.defined.Could.you.create.it.in.any.way"),
                                       GrailsBundle.message("such.relation.already.defined"),
                                       new String[]{
                                         CommonBundle.message("button.ok"),
                                         CommonBundle.message("button.cancel")
                                       },
                                       1,
                                       AllIcons.General.TodoQuestion);
      }

      if (exitCode == OK_EXIT_CODE) {
        creationsProvider.createBelongsToRelation();
      }
    } else if (myHasManyRadioButton.isSelected()) {
      if (!creationsProvider.canCreateHasManyRelation()) {
        exitCode = Messages.showDialog(getContentPane(), GrailsBundle.message("Relation.already.defined.Could.you.create.it.in.any.way"),
                                       GrailsBundle.message("such.relation.already.defined"),
                                       new String[]{
                                         CommonBundle.message("button.ok"),
                                         CommonBundle.message("button.cancel")
                                       },
                                       1,
                                       AllIcons.General.TodoQuestion);
      }

      if (exitCode == OK_EXIT_CODE) {
        creationsProvider.createHasManyRelation();
      }
    } else if (myStableRadioButton.isSelected()) {
      if (!creationsProvider.canCreateStrongRelation()) {
        exitCode = Messages.showDialog(getContentPane(), GrailsBundle.message("Relation.already.defined.Could.you.create.it.in.any.way"),
                                       GrailsBundle.message("such.relation.already.defined"),
                                       new String[]{
                                         CommonBundle.message("button.ok"),
                                         CommonBundle.message("button.cancel")
                                       },
                                       1,
                                       AllIcons.General.TodoQuestion);
      }

      if (exitCode == OK_EXIT_CODE) {
        creationsProvider.createStrongRelation();
      }
    }

    if (myBelongsToRadioButton.isSelected()) edgeRelationType = Relation.BELONGS_TO;
    else if (myHasManyRadioButton.isSelected()) edgeRelationType = Relation.HAS_MANY;
    else if (myStableRadioButton.isSelected()) edgeRelationType = Relation.STRONG;
    else edgeRelationType = Relation.UNKNOWN;


    super.doOKAction();
  }

  private void setUpNameComboBox() {
    final EditorComboBoxEditor comboEditor = new StringComboboxEditor(myProject, GroovyFileType.GROOVY_FILE_TYPE, myNameComboBox);

    myNameComboBox.setEditor(comboEditor);

    myNameComboBox.setRenderer(new EditorComboBoxRenderer(comboEditor));

    myNameComboBox.setEditable(true);
    myNameComboBox.setMaximumRowCount(8);

    myListenerList.add(DataChangedListener.class, new DataChangedListener());

    myNameComboBox.addItemListener(
        new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            fireNameDataChanged();
          }
        }
    );

    ((EditorTextField) myNameComboBox.getEditor().getEditorComponent()).addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        fireNameDataChanged();
      }
    });

    myNameComboBox.addItem(findSuitableName(mySourceTypeDefinition, firstLetterToLowerCase(newNodeShortTypeText), 1));

    myHasManyRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        myNameComboBox.removeAllItems();
        myNameComboBox.addItem(findSuitableName(mySourceTypeDefinition, StringUtil.pluralize(StringUtil.toLowerCase(newNodeShortTypeText)), 1));
      }
    });

    myBelongsToRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        myNameComboBox.removeAllItems();
        myNameComboBox.addItem(findSuitableName(mySourceTypeDefinition, StringUtil.decapitalize(newNodeShortTypeText), 1));
      }
    });

    myStableRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        myNameComboBox.removeAllItems();
        myNameComboBox.addItem(findSuitableName(mySourceTypeDefinition, StringUtil.decapitalize(newNodeShortTypeText), 1));
      }
    });

  }

  private static @NlsSafe String findSuitableName(GrTypeDefinition typeDefinition, String baseName, int counter) {
    if (baseName == null) return "";

    final GrField[] fields = typeDefinition.getFields();

    for (GrField field : fields) {
      if ((baseName + counter).equals(field.getName())) {
        return findSuitableName(typeDefinition, baseName, ++counter);
      }

      Map<DomainClassNode, List<DomainClassRelationsInfo>> sourcesToOutEdges = new HashMap<>();
      if (DomainClassUtils.isHasManyField(field)) {
        DomainClassUtils.buildHasManySourcesToOutEdgesMap(sourcesToOutEdges, field);

        final List<DomainClassRelationsInfo> thisOutEdges = sourcesToOutEdges.get(new DomainClassNode(typeDefinition));
        if (thisOutEdges == null) break;

        for (DomainClassRelationsInfo outEdge : thisOutEdges) {
          if ((baseName + counter).equals(outEdge.getVarName())) {
            return findSuitableName(typeDefinition, baseName, ++counter);
          }
        }
      }

      if (DomainClassUtils.isBelongsToField(field)) {
        sourcesToOutEdges.clear();
        DomainClassUtils.buildBelongsToSourcesToOutEdges(sourcesToOutEdges, field);

        final List<DomainClassRelationsInfo> thisOutEdges = sourcesToOutEdges.get(new DomainClassNode(typeDefinition));
        if (thisOutEdges == null) break;

        for (DomainClassRelationsInfo outEdge : thisOutEdges) {
          if ((baseName + counter).equals(outEdge.getVarName())) {
            return findSuitableName(typeDefinition, baseName, ++counter);
          }
        }
      }

    }

    return baseName + counter;
  }

  private static @Nullable String firstLetterToLowerCase(String newNodeTypeText) {
    if (newNodeTypeText == null) return null;

    return StringUtil.toLowerCase(String.valueOf(newNodeTypeText.charAt(0))) + newNodeTypeText.substring(1);
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameComboBox;
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return myContentPane;
  }

  class DataChangedListener implements EventListener {
    void dataChanged() {
      updateOkStatus();
    }
  }

  private void updateOkStatus() {
    String text = getEnteredName();
    setOKActionEnabled(GroovyNamesUtil.isIdentifier(text) && !(newNodeShortTypeText.equals(text)));
  }

  public @Nullable String getEnteredName() {
    if (myNameComboBox.getEditor().getItem() instanceof String &&
        !((String)myNameComboBox.getEditor().getItem()).isEmpty()) {
      return (String) myNameComboBox.getEditor().getItem();
    } else {
      return null;
    }
  }

  private void fireNameDataChanged() {
    Object[] list = myListenerList.getListenerList();
    for (Object aList : list) {
      if (aList instanceof DataChangedListener) {
        ((DataChangedListener) aList).dataChanged();
      }
    }
  }

  public Relation getEdgeRelationType() {
    return edgeRelationType;
  }
}
