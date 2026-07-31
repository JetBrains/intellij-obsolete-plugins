package com.intellij.gwt.run;

import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.gwt.GwtBundle;

public class GwtCommonParametersPanel extends CommonJavaParametersPanel {

  @Override
  protected void initComponents() {
    super.initComponents();

    getVMParametersComponent().getComponent().setDialogCaption(GwtBundle.message("dialog.caption.vm.parameters"));
    getVMParametersComponent().getLabel().setText(GwtBundle.message("label.text.vm.parameters"));

    getProgramParametersComponent().getComponent().setDialogCaption(GwtBundle.message("dialog.caption.gwt.dev.mode.parameters"));
    getProgramParametersComponent().getLabel().setText(GwtBundle.message("label.text.gwt.dev.mode.parameters"));
  }
}
