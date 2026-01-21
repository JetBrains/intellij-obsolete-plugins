// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.guice.GuiceBundle;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.JBInsets;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProviderDialog extends DialogWrapper implements DocumentListener{
    private JTextField providerNameField = null;
    private JTextField providedClassField = null;
    private FixedSizeButton classChooserButton = null;
    private final Project project;

    protected ProviderDialog(Project project){
        super(project, true);
        this.project = project;
        setModal(true);
        setTitle(GuiceBundle.message("new.guice.provider"));
        init();
        validateButtons();
    }

    @Override
    protected @NonNls String getDimensionServiceKey(){
        return "GuiceyIDEA.NewGuiceProvider";
    }

    @Override
    protected @Nullable JComponent createCenterPanel(){
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.fill = GridBagConstraints.HORIZONTAL;
        gbConstraints.weightx = 1.0;
        gbConstraints.weighty = 0.0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
      gbConstraints.insets = JBInsets.emptyInsets();
        final JPanel classNamePanel = new JPanel(new BorderLayout());

        final JLabel label1 = new JLabel(GuiceBundle.message("provider.class.name"));
        providerNameField = new JTextField();
        providerNameField.getDocument().addDocumentListener(this);
        label1.setDisplayedMnemonic('P');
        label1.setLabelFor(providerNameField);
        classNamePanel.add(label1, BorderLayout.WEST);
        classNamePanel.add(providerNameField, BorderLayout.CENTER);
        panel.add(classNamePanel, gbConstraints);

        providedClassField = new JTextField();
        providedClassField.getDocument().addDocumentListener(this);
        classChooserButton = new FixedSizeButton(providedClassField);
        classChooserButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
                final TreeJavaClassChooserDialog chooser = new TreeJavaClassChooserDialog(
                        GuiceBundle.message("select.provided.class"), project, scope, null, null);
                final String classText = classChooserButton.getText();
                final PsiClass currentClass =
                        JavaPsiFacade.getInstance(project).findClass(classText, GlobalSearchScope.allScope(project));
                if(currentClass != null){
                    chooser.select(currentClass);
                }
                chooser.show();
                final PsiClass selectedClass = chooser.getSelected();
                if(selectedClass != null){
                    final String className = selectedClass.getQualifiedName();
                    providedClassField.setText(className);
                    validateButtons();
                }
            }
        });
        panel.add(classChooserButton, gbConstraints);
        final JPanel existingClassPanel = new JPanel(new BorderLayout());
        final JLabel label2 = new JLabel(GuiceBundle.message("class.provided"));
        label2.setLabelFor(providedClassField);
        label2.setDisplayedMnemonic('C');
        existingClassPanel.add(label2, BorderLayout.WEST);
        existingClassPanel.add(providedClassField, BorderLayout.CENTER);
        existingClassPanel.add(classChooserButton, BorderLayout.EAST);
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 1;
        gbConstraints.gridwidth = 3;
        panel.add(existingClassPanel, gbConstraints);
        return panel;
    }

    public String getProviderName(){
        return providerNameField.getText();
    }

    private void validateButtons(){
      final PsiNameHelper nameHelper = PsiNameHelper.getInstance(project);

        final String providerName = getProviderName();
        final String providedClass = getProvidedClass();
        final boolean valid = nameHelper.isIdentifier(providerName) && nameHelper.isQualifiedName(providedClass);
        getOKAction().setEnabled(valid);
    }

    public String getProvidedClass(){
        return providedClassField.getText();
    }

    @Override
    public void insertUpdate(DocumentEvent event){
        validateButtons();
    }

    @Override
    public void removeUpdate(DocumentEvent event){
        validateButtons();
    }

    @Override
    public void changedUpdate(DocumentEvent event){
        validateButtons();
    }
}
