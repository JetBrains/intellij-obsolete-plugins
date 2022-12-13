package com.intellij.dmserver.util;

import com.intellij.icons.AllIcons;

import javax.swing.*;

public final class IconUtils {

  private IconUtils() {
  }

  public static void setupWarningLabel(JLabel label) {
    label.setIcon(AllIcons.General.BalloonError);
  }
}
