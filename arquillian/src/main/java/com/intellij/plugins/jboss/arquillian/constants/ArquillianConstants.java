package com.intellij.plugins.jboss.arquillian.constants;

import org.jetbrains.annotations.NonNls;

public interface ArquillianConstants {
  @NonNls String DEPLOYMENT_CLASS = "org.jboss.arquillian.container.test.api.Deployment";
  @NonNls String JAVA_ARCHIVE_CLASS = "org.jboss.shrinkwrap.api.Archive";
  @NonNls String TESTNG_TEST_CLASS = "org.testng.annotations.Test";
  @NonNls String TESTNG_ARQUILLIAN_CLASS = "org.jboss.arquillian.testng.Arquillian";
  @NonNls String JUNIT_TEST_CLASS = "org.junit.Test";
  @NonNls String JUNIT_RUN_WITH_CLASS = "org.junit.runner.RunWith";
  @NonNls String JUNIT_ARQUILLIAN_CLASS = "org.jboss.arquillian.junit.Arquillian";
  @NonNls String SHRINK_WRAP_CLASS = "org.jboss.shrinkwrap.api.ShrinkWrap";
  @NonNls String EMPTY_ASSERT_CLASS = "org.jboss.shrinkwrap.api.asset.EmptyAsset";

  String ARQUILLIAN_CORE_MAVEN = "org.jboss.arquillian.core:arquillian-core-api";
}
