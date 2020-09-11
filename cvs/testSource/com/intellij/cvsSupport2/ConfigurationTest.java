package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.config.CvsApplicationLevelConfiguration;
import com.intellij.cvsSupport2.config.CvsRootConfiguration;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.Assert.assertNotEquals;

public class ConfigurationTest extends HeavyPlatformTestCase {
  private CvsRootConfiguration myConfiguration;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myConfiguration = CvsApplicationLevelConfiguration.createNewConfiguration(CvsApplicationLevelConfiguration.getInstance());
    myConfiguration.CVS_ROOT = ":method:user@host:port:repository";
  }

  @Override
  protected void tearDown() throws Exception {
    myConfiguration = null;
    super.tearDown();
  }

  public void testCvsRoot() {
    assertEquals(":method:user@host:port:repository", myConfiguration.getCvsRootAsString());
  }

  public void testEquals() {
    CvsRootConfiguration another = myConfiguration.clone();
    assertEquals(myConfiguration, another);
    assertEquals(myConfiguration.hashCode(), another.hashCode());
  }

  public void testNotEquals() throws Exception {
    Field[] fields = CvsRootConfiguration.class.getDeclaredFields();
    for (Field field : fields) {
      if (shouldBeExcludedFromComparison(field)) continue;
      if (isPublicAndNotStatic(field)) {
        checkNotEqualsWith(createConfigurationCopyWithDifferedField(field), field.getName());
      }
    }

  }

  private static boolean shouldBeExcludedFromComparison(Field field) {
    String[] excludedFields = {
      "PATH_TO_WORKING_FOLDER",
      "ALTERNATIVE_CHECKOUT_PATH",
      "USE_ALTERNATIVE_CHECKOUT_PATH",
      "CHECKOUT_DATE_OR_REVISION_SETTINGS",
      "UPDATE_DATE_OR_REVISION_SETTINGS",
      "SHOW_CHANGES_REVISION_SETTINGS",
      "DATE_OR_REVISION_SETTINGS",
      "PROXY_SETTINGS",
      "SSH_CONFIGURATION",
      "SSH_FOR_EXT_CONFIGURATION",
      "LOCAL_CONFIGURATION",
      "EXT_CONFIGURATION"
    };

    return Arrays.asList(excludedFields).contains(field.getName());
  }


  private CvsRootConfiguration createConfigurationCopyWithDifferedField(Field field) throws Exception {
    CvsRootConfiguration another = myConfiguration.clone();
    Class type = field.getType();
    if (type.equals(String.class))
      field.set(another, "another");
    else if (type.equals(int.class))
      field.setInt(another, field.getInt(another) + 1);
    else if (type.equals(boolean.class))
      field.setBoolean(another, !field.getBoolean(another));
    else
      field.set(another, type.newInstance());
    return another;
  }

  private static boolean isPublicAndNotStatic(Field field) {
    int modifiers = field.getModifiers();
    return (modifiers & Modifier.PUBLIC) != 0 && (modifiers & Modifier.STATIC) == 0;
  }

  private void checkNotEqualsWith(CvsRootConfiguration another, String fieldName) {
    assertNotEquals(fieldName + " is different", myConfiguration, another);
  }
}
