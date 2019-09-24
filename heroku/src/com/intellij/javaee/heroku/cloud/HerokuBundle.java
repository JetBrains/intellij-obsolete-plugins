package com.intellij.javaee.heroku.cloud;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

/**
 * @author michael.golubev
 */
public class HerokuBundle extends ListResourceBundle {

  private static Reference<ResourceBundle> ourBundle;

  @NonNls private static final String BUNDLE = "resources.heroku";

  @Override
  protected Object[][] getContents() {
    return new Object[0][];
  }

  public static String getText(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
