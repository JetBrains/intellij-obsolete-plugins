/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.run.ui;

import com.intellij.concurrency.JobScheduler;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.MobileSdk;
import com.intellij.j2meplugin.emulator.midp.uei.UnifiedEmulatorType;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class OTASettingsConfigurable extends SettingsEditor<J2MERunConfiguration> {
  private JTextField myInstallUrl;
  private JPanel myWholePanel;
  private JRadioButton myInstall;
  private JTextField myTransientUrl;
  private JRadioButton myRun;
  private JComboBox myExistAppToRun;
  private final DefaultComboBoxModel myExistSuitsToRun = new DefaultComboBoxModel();
  private JRadioButton myRunRemove;
  private JComboBox myExistAppToRemove;
  private final DefaultComboBoxModel myExistSuitsToRunAndRemove = new DefaultComboBoxModel();
  private JRadioButton myTransient;
  private String[] myCommands;
  private final Sdk myProjectJdk;
  private final Module myModule;

  private static final Logger LOG = Logger.getInstance("#com.intellij.j2meplugin");
  private JCheckBox myForce;
  private JButton myUpdate;

  private HandlingProcessHandler myRetrievingProcess;
  private String [] myInstalledSuits;
  public static final int _INSTALL = 1;
  public static final int _REMOVE = 2;
  public static final int _RUN = 3;
  public static final int _TRANSIENT = 4;

  public OTASettingsConfigurable(final Sdk projectJdk, final Module module) {
    myProjectJdk = projectJdk;
    myModule = module;

    final ButtonGroup myStart = new ButtonGroup();
    myStart.add(myInstall);
    myStart.add(myRun);
    myStart.add(myRunRemove);
    myStart.add(myTransient);

    myInstall.setSelected(true);

    myExistAppToRemove.setModel(myExistSuitsToRunAndRemove);
    myExistAppToRun.setModel(myExistSuitsToRun);
    myUpdate.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        refreshInstalledSuits();
      }
    });

    myInstall.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (myInstall.isSelected() && canForce()) {
          myForce.setEnabled(true);
        }
        else {
          myForce.setEnabled(false);
        }
      }
    });
  }

  public void setCommands(String[] commands) {
    myCommands = commands;
    myInstall.setEnabled(false);
    myInstallUrl.setEnabled(false);
    myExistAppToRun.setEnabled(false);
    myRun.setEnabled(false);
    myExistAppToRemove.setEnabled(false);
    myRunRemove.setEnabled(false);
    myTransient.setEnabled(false);
    myTransientUrl.setEnabled(false);
    myUpdate.setEnabled(false);

    for (int i = 0; commands != null && i < commands.length; i++) {
      if (commands[i].indexOf(UnifiedEmulatorType.INSTALL) != -1) {
        myInstall.setEnabled(true);
        myInstallUrl.setEnabled(true);
      }
      if (commands[i].indexOf(UnifiedEmulatorType.RUN) != -1) {
        myExistAppToRun.setEnabled(true);
        myRun.setEnabled(true);
      }
      if (commands[i].indexOf(UnifiedEmulatorType.REMOVE) != -1) {
        myExistAppToRemove.setEnabled(true);
        myRunRemove.setEnabled(true);
      }
      if (commands[i].indexOf(UnifiedEmulatorType.TRANSIENT) != -1) {
        myTransient.setEnabled(true);
        myTransientUrl.setEnabled(true);
      }

    }

    if (myExistAppToRun.isEnabled() || myExistAppToRemove.isEnabled()) {
      myUpdate.setEnabled(true);
    }


  }

  private boolean canForce() {
    for (int i = 0; myCommands != null && i < myCommands.length; i++) {
      if (myCommands[i].indexOf(UnifiedEmulatorType.FORCE) != -1) {
        return true;
      }
    }
    return false;
  }

  public void refreshInstalledSuits() {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (progressIndicator != null) {
          progressIndicator.setText(J2MEBundle.message("run.configuration.ota.retrieving.progress.title"));
          progressIndicator.setIndeterminate(true);
        }
        final Future<?> getterFuture = ApplicationManager.getApplication().executeOnPooledThread(
          (Runnable)() -> myInstalledSuits = getInstalledSuits(myCommands));

        JobScheduler.getScheduler().schedule(new Runnable() {
          @Override
          public void run() {
            if (progressIndicator == null || progressIndicator.isCanceled()) {
              cancelRetrieval();
            }
            else {
              progressIndicator.setText2("");
              JobScheduler.getScheduler().schedule(this, 50, TimeUnit.MILLISECONDS);
            }
          }
        }, 10, TimeUnit.MILLISECONDS);

        try {
          getterFuture.get();
        }
        catch (Exception e) {
          LOG.error(e);
          cancelRetrieval();
        }
        SwingUtilities.invokeLater(() -> {
          myExistSuitsToRun.removeAllElements();
          myExistSuitsToRunAndRemove.removeAllElements();
          if (myInstalledSuits.length > 0 && myInstalledSuits[0].compareToIgnoreCase(J2MEBundle.message("run.configuration.ota.no.suites.installed")) == 0){
            myExistSuitsToRun.addElement(myInstalledSuits[0]);
            myExistSuitsToRunAndRemove.addElement(myInstalledSuits[0]);
          } else {
            for (int i = 1; myInstalledSuits != null && i < myInstalledSuits.length; i++) {
              if (myInstalledSuits[i].indexOf(J2MEBundle.message("run.configuration.ota.exclusion.suits.filter")) > -1) continue;    //todo
              myExistSuitsToRun.addElement(myInstalledSuits[i]);
              myExistSuitsToRunAndRemove.addElement(myInstalledSuits[i]);
            }
          }
        });
        if (myInstalledSuits == null || myInstalledSuits.length == 0){
          Messages.showInfoMessage(J2MEBundle.message("run.configuration.ota.no.suites.installed"), J2MEBundle.message("run.configuration.ota.no.suites.installed"));
        }

      }
    }, J2MEBundle.message("run.configuration.ota.retrieving.progress.title"), true, myModule.getProject());
    cancelRetrieval();
  }

  private void cancelRetrieval() {
    if (myRetrievingProcess != null) {
      myRetrievingProcess.terminate();
    }
  }


  @Override
  @NotNull
  public JComponent createEditor() {
    return myWholePanel;
  }


  @Override
  public void resetEditorFrom(@NotNull final J2MERunConfiguration jdomExternalizable) {
    final J2MERunConfiguration specificConfiguration = jdomExternalizable;
    myExistAppToRun.setSelectedItem(specificConfiguration.RUN);
    myExistAppToRemove.setSelectedItem(specificConfiguration.REMOVE);
    myTransientUrl.setText(specificConfiguration.TRANSIENT);
    if (specificConfiguration.INSTALL != null) {
      @NonNls final String jam = " -Xjam:force";
      final int forceIndex = specificConfiguration.INSTALL.indexOf(jam);
      if (forceIndex > -1) {
        myForce.setSelected(true);
        myInstallUrl.setText(specificConfiguration.INSTALL.substring(0, forceIndex));
      }
      else {
        myInstallUrl.setText(specificConfiguration.INSTALL);
      }
    }
    if (specificConfiguration.SELECTION == _TRANSIENT) {
      myTransient.setSelected(true);
    }
    else {
      if (specificConfiguration.SELECTION == _RUN) {
        myRun.setSelected(true);
      }
      else {
        if (specificConfiguration.SELECTION == _REMOVE) {
          myRunRemove.setSelected(true);
        }
        else {
          myInstall.setSelected(true);
        }
      }
    }
  }

  @Override
  public void applyEditorTo(@NotNull final J2MERunConfiguration jdomExternalizable) throws ConfigurationException {
    @NonNls final String jam = " -Xjam:";
    jdomExternalizable.INSTALL = myInstallUrl.getText() + (myForce.isSelected() ? jam + UnifiedEmulatorType.FORCE : "");
    jdomExternalizable.RUN = (String)myExistAppToRun.getSelectedItem();
    jdomExternalizable.REMOVE = (String)myExistAppToRemove.getSelectedItem();
    jdomExternalizable.TRANSIENT = myTransientUrl.getText();
    if (myInstall.isSelected()) {
      jdomExternalizable.TO_START = jdomExternalizable.INSTALL;
      jdomExternalizable.SELECTION = _INSTALL;
    }
    else {
      if (myRun.isSelected()) {
        jdomExternalizable.TO_START = jdomExternalizable.RUN;
        jdomExternalizable.SELECTION = _RUN;
      }
      else {
        if (myRunRemove.isSelected()) {
          jdomExternalizable.TO_START = jdomExternalizable.REMOVE;
          jdomExternalizable.SELECTION = _REMOVE;
        }
        else {
          jdomExternalizable.TO_START = jdomExternalizable.TRANSIENT;
          jdomExternalizable.SELECTION = _TRANSIENT;
        }
      }
    }
  }

  private String[] getInstalledSuits(final String[] commands) {
    final HashSet<String> installed = new HashSet<>();
    if (myProjectJdk != null && MobileSdk.checkCorrectness(myProjectJdk, myModule)) {
      boolean canRunPreviouslyInstalled = false;
      for (int i = 0; commands != null && i < commands.length; i++) {
        if (commands[i].indexOf(UnifiedEmulatorType.STORAGE_NAMES) > -1) {
          canRunPreviouslyInstalled = true;
          break;
        }
      }
      if (!canRunPreviouslyInstalled) return null;
      final GeneralCommandLine generalCommandLine = new GeneralCommandLine();
      generalCommandLine.setExePath(myProjectJdk.getHomePath() + File.separatorChar + "bin" + File.separatorChar + "emulator");
      generalCommandLine.addParameter("-Xjam:" + UnifiedEmulatorType.STORAGE_NAMES);

      try {
        myRetrievingProcess = new HandlingProcessHandler(generalCommandLine);
      }
      catch (ExecutionException e) {
        LOG.error(e);
      }
      myRetrievingProcess.addProcessListener(new ProcessAdapter() {
        @Override
        public void onTextAvailable(@NotNull final ProcessEvent event, @NotNull final Key outputType) {
          String name = event.getText();
          name = StringUtil.convertLineSeparators(name);
          installed.add(name);
        }
      });
      myRetrievingProcess.startNotify();
      myRetrievingProcess.waitFor();

      return ArrayUtil.toStringArray(installed);
    }
    return null;
  }

  private static class HandlingProcessHandler extends OSProcessHandler{

    private HandlingProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
      super(commandLine);
    }

    public void terminate(){
      notifyProcessDetached();
      if (detachIsDefault()){
        detachProcess();
      } else {
        destroyProcess();
      }
    }
  }
}
