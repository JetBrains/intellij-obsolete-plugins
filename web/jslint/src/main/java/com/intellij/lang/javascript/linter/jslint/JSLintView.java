package com.intellij.lang.javascript.linter.jslint;

import com.intellij.lang.javascript.linter.JSLinterBaseView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Sergey Simonchik
 */
public class JSLintView extends JSLinterBaseView<JSLintState> {

  private final JSLintOptionsView myOptionsView;

  public JSLintView() {
    myOptionsView = new JSLintOptionsView();
  }

  @Nullable
  @Override
  protected Component createTopRightComponent() {
    return new JLabel("Version 2016-07-13");
  }

  @NotNull
  @Override
  protected Component createCenterComponent() {
    return myOptionsView.getComponent();
  }

  @Override
  protected void handleEnableStatusChanged(boolean enabled) {
    myOptionsView.handleEnableStatusChanged(enabled);
  }

  @NotNull
  @Override
  protected JSLintState getState() {
    JSLintState.Builder builder = myOptionsView.getStateBuilder();
    return builder.build();
  }

  @Override
  protected void setState(@NotNull JSLintState state) {
    myOptionsView.setState(state);
  }
}
