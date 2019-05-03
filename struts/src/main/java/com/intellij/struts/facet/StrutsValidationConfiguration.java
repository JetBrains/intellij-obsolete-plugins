/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet;

/**
 */
public class StrutsValidationConfiguration {

  public StrutsValidationConfiguration() {
  }

  public boolean myStrutsValidationEnabled = true;
  public boolean myTilesValidationEnabled = true;
  public boolean myValidatorValidationEnabled = true;

  /**
   * Report errors with warning level so compilation does not fail.
   */
  public boolean myReportErrorsAsWarnings = true;

  public boolean mySuppressPropertiesValidation = false;
}
