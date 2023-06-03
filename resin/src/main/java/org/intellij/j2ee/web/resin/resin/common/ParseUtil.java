package org.intellij.j2ee.web.resin.resin.common;

import com.intellij.openapi.options.ConfigurationException;

import javax.swing.*;

public abstract class ParseUtil {

  public int parseInt(JTextField text)
    throws ConfigurationException {
    String toParse = text.getText();
    if (toParse == null) {
      toParse = "";
    }
    toParse = toParse.trim();
    try {
      return Integer.parseInt(toParse);
    }
    catch (NumberFormatException e) {
      throw new ConfigurationException(getErrorMessage(toParse));
    }
  }

  protected abstract String getErrorMessage(String unparsableValue);
}
