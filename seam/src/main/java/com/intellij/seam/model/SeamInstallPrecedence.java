package com.intellij.seam.model;

public interface SeamInstallPrecedence {
  //constants were copied from org.jboss.seam.annotations.Install

  /**
   * Precedence of all built-in Seam components
   */
  int BUILT_IN = 0;
  /**
   * Precedence to use for components of frameworks
   * which extend Seam
   */
  int FRAMEWORK = 10;
  /**
   * Predence of application components (the
   * default precedence)
   */
  int APPLICATION = 20;
  /**
   * Precedence to use for components which override
   * application components in a particular deployment
   */
  int DEPLOYMENT = 30;

  /**
   * Precedence to use for mock objects in tests
   */
  int MOCK = 40;

  int DEFAULT = APPLICATION;
}
