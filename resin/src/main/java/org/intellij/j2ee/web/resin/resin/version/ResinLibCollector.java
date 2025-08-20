/*
 * Copyright (c) 2006 Openwave Systems Inc. All rights reserved.
 *
 * The copyright to the computer software herein is the property of Openwave Systems Inc. The software may be used
 * and/or copied only with the written permission of Openwave Systems Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the software has been supplied.
 */

package org.intellij.j2ee.web.resin.resin.version;

import com.intellij.openapi.util.io.FileFilters;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Sergio Cuellar (sergio.cuellar@openwave.com)
 */
public final class ResinLibCollector {
  private static final FileFilter JAR_FILTER = FileFilters.filesWithExtension("jar");

  private ResinLibCollector() {
  }

  public static File[] getLibFiles(File libDir, boolean all) {
    if (all) {
      return libDir.listFiles(JAR_FILTER);
    }
    else {
      //Resin 3.2.0 -> javaee-XX.jar
      //Other versions -> jsdk-XX.jar
      return libDir.listFiles(
        (dir, name) -> JAR_FILTER.accept(new File(dir, name)) && (name.contains("jsdk") || name.contains("javaee-"))
      );
    }
  }
}
