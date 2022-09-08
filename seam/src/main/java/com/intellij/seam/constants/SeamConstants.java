package com.intellij.seam.constants;

import org.jetbrains.annotations.NonNls;

public interface SeamConstants {
  @NonNls String SEAM_DETECTION_CLASS = "org.jboss.seam.annotations.Name";
  @NonNls String SEAM_CONFIG_FILENAME = "components.xml";
  @NonNls String SEAM_PAGES_FILENAME = "pages.xml";
  @NonNls String SEAM_CONFIG_ROOT_TAG_NAME = "components";

  @NonNls String J2EE_INTERCEPTORS_ANNOTATION = "org.jboss.seam.annotations.intercept.Interceptors";

  @NonNls String FILE_TEMPLATE_NAME_SEAM_1_2 ="components.1_2.xml";
  @NonNls String FILE_TEMPLATE_NAME_SEAM_2_0 ="components.2_0.xml";
  @NonNls String FILE_TEMPLATE_NAME_PAGES_2_0 ="pages.2_0.xml";
  @NonNls String FILE_TEMPLATE_NAME_PAGEFLOW_2_0 ="pageflow.2_0.xml";
}
