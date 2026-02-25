// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.projectView;

public final class NodeWeights {

  private NodeWeights() {}

  public static final int DOMAIN_CLASSES_FOLDER = 20;
  public static final int CONTROLLERS_FOLDER = 30;
  public static final int VIEWS_FOLDER = 40;
  public static final int SERVICES_FOLDER = 50;
  public static final int CONFIG_FOLDER = 60;
  public static final int OTHER_GRAILS_APP_FOLDER = 64;
  public static final int WEB_APP_FOLDER = 65;
  public static final int SRC_FOLDERS = 70;
  public static final int TESTS_FOLDER = 80;
  public static final int TAGLIB_FOLDER = 90;
  public static final int FOLDER = 100;
}
