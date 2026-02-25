// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

public final class PrintGrailsSettingsConstants {

  // See
  public static final String COMPILE = "Compile";
  public static final String RUNTIME = "Runtime";
  public static final String TESTS = "Test";
  public static final String PROVIDED = "Provided";
  public static final String BUILD = "Build";

  public static final String CUSTOM_PLUGIN_PREFIX = "grails.plugin.location.";

  public static final String SETTINGS_START_MARKER = "---=== IDEA Grails build settings ===---";
  public static final String SETTINGS_END_MARKER = "---=== End IDEA Grails build settings ===---";

  public static final String DEBUG_RUN_FORK = "grails.project.fork.run";
  public static final String DEBUG_TEST_FORK = "grails.project.fork.test";

  // --- === Copied from BuildConfig.groovy === ---
  public static final String WORK_DIR = "grails.work.dir";
  public static final String PROJECT_WORK_DIR = "grails.project.work.dir";
  public static final String PLUGINS_DIR = "grails.project.plugins.dir";
  public static final String GLOBAL_PLUGINS_DIR = "grails.global.plugins.dir";

  //public static final String PROJECT_RESOURCES_DIR = "grails.project.resource.dir";
  //public static final String PROJECT_CLASSES_DIR = "grails.project.class.dir";
  //public static final String PROJECT_TEST_CLASSES_DIR = "grails.project.test.class.dir";
  //public static final String PROJECT_WAR_FILE = "grails.project.war.file";
  //public static final String PROJECT_WAR_EXPLODED_DIR = "grails.project.war.exploded.dir";
  //public static final String PROJECT_SOURCE_DIR = "grails.project.source.dir";
  //public static final String PROJECT_WEB_XML_FILE = "grails.project.web.xml";
  //public static final String PROJECT_TEST_REPORTS_DIR = "grails.project.test.reports.dir";
  //public static final String PROJECT_TEST_SOURCE_DIR = "grails.project.test.source.dir";
  //public static final String PROJECT_TARGET_DIR = "grails.project.target.dir";

  private PrintGrailsSettingsConstants() {
  }

}
